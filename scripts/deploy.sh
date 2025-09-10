#!/bin/bash

# AWS ECS 배포 스크립트

set -e

# 색상 설정
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 로깅 함수
log() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 필수 도구 확인
check_dependencies() {
    log "필수 도구 확인 중..."
    
    # AWS CLI 확인
    if ! command -v aws &> /dev/null; then
        error "AWS CLI가 설치되지 않았습니다."
        exit 1
    fi
    
    # Docker 확인
    if ! command -v docker &> /dev/null; then
        error "Docker가 설치되지 않았습니다."
        exit 1
    fi
    
    # jq 확인
    if ! command -v jq &> /dev/null; then
        error "jq가 설치되지 않았습니다. 설치: brew install jq (macOS) 또는 apt-get install jq (Ubuntu)"
        exit 1
    fi
    
    log "필수 도구 확인 완료"
}

# 환경 변수 확인
check_aws_credentials() {
    log "AWS 자격 증명 확인 중..."
    if ! aws sts get-caller-identity > /dev/null 2>&1; then
        error "AWS CLI가 구성되지 않았습니다. 'aws configure'를 실행하세요."
        exit 1
    fi
    log "AWS 자격 증명 확인 완료"
}

# ECR 로그인
ecr_login() {
    log "ECR 로그인 중..."
    AWS_REGION=${AWS_REGION:-ap-northeast-2}
    AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
    
    aws ecr get-login-password --region $AWS_REGION | docker login --username AWS --password-stdin $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com
    log "ECR 로그인 완료"
}

# ALB DNS 주소 조회
get_alb_dns() {
    log "ALB DNS 주소 조회 중..."
    ALB_DNS=$(aws elbv2 describe-load-balancers --names techbloghub-alb --region $AWS_REGION --query 'LoadBalancers[0].DNSName' --output text 2>/dev/null)
    
    if [ "$ALB_DNS" = "None" ] || [ -z "$ALB_DNS" ]; then
        warn "ALB를 찾을 수 없습니다. Terraform이 완료되었는지 확인하세요."
        ALB_DNS="localhost"
    else
        log "ALB DNS 주소: $ALB_DNS"
    fi
}

# 백엔드 빌드 및 푸시
build_and_push_backend() {
    log "백엔드 빌드 시작..."
    
    cd backend
    ./gradlew build -x test
    cd ..
    
    log "백엔드 Docker 이미지 빌드 중..."
    docker build --platform linux/amd64 -t techbloghub-backend ./backend
    
    # ECR에 태그 및 푸시
    BACKEND_ECR_URI="$AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/techbloghub/backend"
    TIMESTAMP=$(date +%Y%m%d-%H%M%S)
    docker tag techbloghub-backend:latest $BACKEND_ECR_URI:latest
    docker tag techbloghub-backend:latest $BACKEND_ECR_URI:$TIMESTAMP
    
    log "백엔드 이미지 ECR에 푸시 중..."
    docker push $BACKEND_ECR_URI:latest
    docker push $BACKEND_ECR_URI:$TIMESTAMP
    
    log "백엔드 빌드 및 푸시 완료"
}

# 프론트엔드 빌드 및 푸시
build_and_push_frontend() {
    log "프론트엔드 빌드 시작..."
    
    # 환경 설정 (기본값: production)
    FRONTEND_ENV=${FRONTEND_ENV:-"production"}
    log "프론트엔드 환경: $FRONTEND_ENV"
    
    log "프론트엔드 Docker 이미지 빌드 중..."
    docker build --platform linux/amd64 --build-arg ENV="$FRONTEND_ENV" -t techbloghub-frontend ./frontend
    
    # ECR에 태그 및 푸시
    FRONTEND_ECR_URI="$AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/techbloghub/frontend"
    TIMESTAMP=$(date +%Y%m%d-%H%M%S)
    docker tag techbloghub-frontend:latest $FRONTEND_ECR_URI:latest
    docker tag techbloghub-frontend:latest $FRONTEND_ECR_URI:$TIMESTAMP
    
    log "프론트엔드 이미지 ECR에 푸시 중..."
    docker push $FRONTEND_ECR_URI:latest
    docker push $FRONTEND_ECR_URI:$TIMESTAMP
    
    log "프론트엔드 빌드 및 푸시 완료"
}

# ECS 서비스 업데이트
update_ecs_service() {
    local service_name=$1
    local task_family=$2
    
    log "ECS 서비스 업데이트: $service_name"
    
    # 현재 Task Definition 조회
    CURRENT_TASK_DEF=$(aws ecs describe-task-definition --task-definition $task_family --region $AWS_REGION --query 'taskDefinition.taskDefinitionArn' --output text)
    
    if [ -z "$CURRENT_TASK_DEF" ]; then
        error "Task Definition을 찾을 수 없습니다: $task_family"
        return 1
    fi
    
    # ECS 서비스 업데이트 (새 이미지로 강제 재배포)
    log "ECS 서비스 $service_name 업데이트 중..."
    aws ecs update-service \
        --cluster techbloghub-cluster \
        --service $service_name \
        --force-new-deployment \
        --region $AWS_REGION > /dev/null
    
    log "ECS 서비스 $service_name 업데이트 시작됨"
}

# ECS 배포 대기
wait_for_deployment() {
    local service_name=$1
    local timeout=${2:-600}  # 10분 기본값
    
    log "ECS 서비스 $service_name 배포 완료 대기 중... (최대 ${timeout}초)"
    
    local start_time=$(date +%s)
    
    while true; do
        # 현재 시간 확인
        local current_time=$(date +%s)
        local elapsed=$((current_time - start_time))
        
        if [ $elapsed -gt $timeout ]; then
            error "배포 대기 시간 초과 (${timeout}초)"
            return 1
        fi
        
        # 서비스 상태 확인
        local deployments=$(aws ecs describe-services \
            --cluster techbloghub-cluster \
            --services $service_name \
            --region $AWS_REGION \
            --query 'services[0].deployments[?status==`RUNNING`]' \
            --output json)
        
        local running_count=$(echo "$deployments" | jq '. | length')
        
        if [ "$running_count" = "1" ]; then
            log "ECS 서비스 $service_name 배포 완료!"
            break
        fi
        
        log "배포 진행 중... (경과 시간: ${elapsed}초)"
        sleep 10
    done
}

# 메인 실행
main() {
    log "AWS ECS 배포 시작"
    local start_time=$(date +%s)
    
    check_dependencies
    check_aws_credentials
    ecr_login
    get_alb_dns
    
    case "${1:-all}" in
        backend)
            build_and_push_backend
            log "백엔드 ECS 서비스 업데이트 중..."
            update_ecs_service "techbloghub-backend-service" "techbloghub-backend"
            ;;
        frontend)
            build_and_push_frontend
            log "프론트엔드 ECS 서비스 업데이트 중..."
            update_ecs_service "techbloghub-frontend-service" "techbloghub-frontend"
            ;;
        all|*)
            # 백엔드 먼저 배포
            build_and_push_backend
            log "백엔드 ECS 서비스 업데이트 중..."
            update_ecs_service "techbloghub-backend-service" "techbloghub-backend"
            
            # 프론트엔드 배포
            build_and_push_frontend
            log "프론트엔드 ECS 서비스 업데이트 중..."
            update_ecs_service "techbloghub-frontend-service" "techbloghub-frontend"
            ;;
    esac
    
    local end_time=$(date +%s)
    local total_time=$((end_time - start_time))
    
    log "🎉 배포 완료! (총 소요시간: ${total_time}초)"
    log "🌐 서비스 URL: https://teckbloghub.kr"
    log "📊 백엔드 API: https://api.teckbloghub.kr"
    log "📝 Swagger UI: https://api.teckbloghub.kr/swagger-ui.html"
}

# 스크립트 실행
main "$@"