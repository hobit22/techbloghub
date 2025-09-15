# AWS ECS 배포 가이드

## 전체 배포 과정

### 사전 준비사항

1. **AWS CLI 설치 및 구성**

```bash
aws configure
# AWS Access Key ID, Secret Access Key, Region(ap-northeast-2) 설정
```

2. **Terraform 설치**

```bash
# macOS
brew install terraform

# 또는 직접 다운로드
# https://developer.hashicorp.com/terraform/downloads
```

3. **Docker 설치 및 실행** (로컬에서 이미지 빌드용)

## 배포 단계

### 1단계: 인프라 생성

```bash
cd aws

# 보안 변수 설정
cp terraform.tfvars.example terraform.tfvars
# terraform.tfvars 파일을 편집하여 필수 변수들 설정:
# - db_username: 데이터베이스 관리자 계정명
# - db_password: 데이터베이스 비밀번호  
# - openai_api_key: OpenAI API 키 (LLM 태깅 기능용)

terraform init
terraform plan
terraform apply
```

생성되는 리소스:

- RDS PostgreSQL 인스턴스
- ECS 클러스터
- Application Load Balancer
- ECR 리포지토리
- 보안 그룹 및 네트워킹

### 2단계: Docker 이미지 빌드 및 푸시

```bash
cd ..  # 프로젝트 루트로 이동
./scripts/deploy.sh
```

이 스크립트는 다음을 수행합니다:

- Backend Gradle 빌드
- Docker 이미지 빌드 (Backend, Frontend)
- ECR에 이미지 푸시

### 3단계: ECS 서비스 시작

Terraform apply가 완료되면 ECS 서비스가 자동으로 시작됩니다.

## 배포 결과 확인

### 접속 URL

```bash
terraform output application_url
terraform output backend_api_url
```

### 주요 엔드포인트

- **Frontend**: `http://<ALB-DNS>/`
- **Backend API**: `http://<ALB-DNS>/api/`
- **Health Check**: `http://<ALB-DNS>/actuator/health`
- **Swagger UI**: `http://<ALB-DNS>/swagger-ui.html`

### 서비스 상태 확인

```bash
# ECS 서비스 상태 확인
aws ecs describe-services --cluster techbloghub-cluster --services techbloghub-backend-service techbloghub-frontend-service

# 로그 확인
aws logs tail /ecs/techbloghub/backend --follow
aws logs tail /ecs/techbloghub/frontend --follow
```

## 업데이트 배포

### 코드 변경 후 재배포

```bash
# 이미지만 다시 빌드하고 푸시
./scripts/deploy.sh

# ECS 서비스 강제 업데이트 (새 이미지로 재배포)
aws ecs update-service --cluster techbloghub-cluster --service techbloghub-backend-service --force-new-deployment
aws ecs update-service --cluster techbloghub-cluster --service techbloghub-frontend-service --force-new-deployment
```

## 모니터링

### CloudWatch Logs

- Backend: `/ecs/techbloghub/backend`
- Frontend: `/ecs/techbloghub/frontend`

### Auto Scaling

- CPU 사용률 70% 이상 시 자동 스케일 아웃
- 최소 1개, 최대 4개 인스턴스

## 비용 최적화

### 리소스 정리

```bash
# 모든 리소스 삭제
terraform destroy
```

### 개발환경 일시정지

```bash
# ECS 서비스 스케일을 0으로 설정 (비용 절약)
aws ecs update-service --cluster techbloghub-cluster --service techbloghub-backend-service --desired-count 0
aws ecs update-service --cluster techbloghub-cluster --service techbloghub-frontend-service --desired-count 0
```

## 보안 설정

### ⚠️ 중요 보안 사항

- **terraform.tfvars**: 민감한 정보 저장 (git에서 자동 무시됨)
- **terraform.tfstate**: 절대 git에 커밋하지 마세요!
- **DB 비밀번호**: 강력한 패스워드 사용 필수

### 보안 구조

- RDS는 VPC 내부에서만 접근 가능
- ECS Tasks는 ALB를 통해서만 외부 접근 가능
- 비밀번호는 `terraform.tfvars`에서만 설정
- 운영환경에서는 AWS Secrets Manager 사용 권장

## 문제 해결

### 일반적인 문제

1. **Task가 시작되지 않는 경우**: CloudWatch 로그 확인
2. **Health Check 실패**: Backend API 응답 확인
3. **이미지 푸시 실패**: ECR 권한 및 AWS CLI 설정 확인

## AWS Deployment & Monitoring

### AWS CloudWatch Logs Commands

```bash
# 로그 그룹 목록 조회
aws logs describe-log-groups

# 로그 스트림 조회
aws logs describe-log-streams --log-group-name "/ecs/techbloghub/backend"
aws logs describe-log-streams --log-group-name "/ecs/techbloghub/frontend"

# 실시간 로그 tail (가장 유용)
aws logs tail "/ecs/techbloghub/backend" --follow
aws logs tail "/ecs/techbloghub/frontend" --follow

# 시간 범위 로그
aws logs tail "/ecs/techbloghub/backend" --since 1h
aws logs tail "/ecs/techbloghub/backend" --since "2024-01-01 10:00" --until "2024-01-01 11:00"

# 로그 필터링
aws logs tail "/ecs/techbloghub/backend" --filter-pattern "ERROR"
aws logs tail "/ecs/techbloghub/backend" --filter-pattern "database"

# 로그 파일로 저장
aws logs tail "/ecs/techbloghub/backend" --since 1h > backend-logs.txt
```

### ECS 상태 확인 Commands

```bash
# 클러스터 및 서비스 상태
aws ecs list-clusters
aws ecs describe-services --cluster techbloghub-cluster --services techbloghub-backend-service
aws ecs describe-services --cluster techbloghub-cluster --services techbloghub-frontend-service

# 태스크 목록 및 세부 정보
aws ecs list-tasks --cluster techbloghub-cluster --service-name techbloghub-backend-service
aws ecs describe-tasks --cluster techbloghub-cluster --tasks <TASK_ARN>

# 컨테이너 접속 (디버깅용)
aws ecs execute-command --cluster techbloghub-cluster --task <TASK_ARN> --container backend --interactive and "/bin/bash"
```

### RDS 데이터 복원

```bash
# PostgreSQL 백업 복원 (RDS publicly_accessible=true 필요)
pg_restore \
  --host=<RDS_ENDPOINT> \
  --port=5432 \
  --username=<DB_USERNAME> \
  --dbname=techbloghub \
  --verbose \
  --clean \
  --no-owner \
  --no-privileges \
  techbloghub_backup.dump
```
