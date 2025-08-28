# TechBlogHub 설정 가이드

## 요구사항

- Java 21+
- Node.js 18+
- Docker & Docker Compose
- Git

## 개발 환경 설정

### 1. 프로젝트 클론
```bash
git clone <repository-url>
cd techbloghub
```

### 2. 개발 환경 시작
```bash
# Makefile을 사용한 간편 설정
make setup

# 또는 수동 설정
make dev
```

이 명령어는 다음 서비스들을 시작합니다:
- PostgreSQL (포트 5432)
- ElasticSearch (포트 9200)
- Kibana (포트 5601)

### 3. 백엔드 실행
```bash
# 터미널 1
make backend-run

# 또는
cd backend
./gradlew bootRun
```

### 4. 프론트엔드 실행
```bash
# 터미널 2
make frontend-run

# 또는
cd frontend
npm install
npm run dev
```

### 5. 애플리케이션 접속
- 프론트엔드: http://localhost:3000
- 백엔드 API: http://localhost:8080
- H2 콘솔: http://localhost:8080/h2-console
- Kibana: http://localhost:5601

## 초기 데이터

초기 설정 시 다음 데이터가 자동으로 생성됩니다:

### 기술블로그
- 네이버 D2
- 카카오 Tech
- 우아한기술블로그
- LINE Engineering
- NHN Cloud Meetup
- 토스 Tech
- 당근 Tech Blog
- 컬리 기술 블로그

### 카테고리
- Frontend, Backend, DevOps, Data, Mobile, Security, Performance, Architecture

### 태그
- Java, Spring, JavaScript, React, Python, Docker, Kubernetes, AWS 등

## 데이터베이스 설정

### 개발 환경
- H2 인메모리 데이터베이스 사용
- 애플리케이션 시작 시 자동으로 테이블 생성

### 프로덕션 환경
- PostgreSQL 사용
- 환경 변수로 데이터베이스 설정

## 환경 변수

### 백엔드
```bash
# 데이터베이스
DATABASE_URL=jdbc:postgresql://localhost:5432/techbloghub
DATABASE_USERNAME=admin
DATABASE_PASSWORD=password

# ElasticSearch
ELASTICSEARCH_HOST=http://localhost:9200

# 프로필
SPRING_PROFILES_ACTIVE=local|docker|prod
```

### 프론트엔드
```bash
NEXT_PUBLIC_API_URL=http://localhost:8080
```

## 크롤링 시작

애플리케이션 시작 후 수동으로 크롤링을 시작할 수 있습니다:

```bash
# 모든 블로그 크롤링
curl -X POST http://localhost:8080/api/blogs/crawl-all

# 특정 블로그 크롤링
curl -X POST http://localhost:8080/api/blogs/{blogId}/crawl
```

## 문제 해결

### 포트 충돌
기본 포트가 사용 중인 경우 docker-compose.yml에서 포트를 변경하세요.

### ElasticSearch 메모리 부족
Docker Desktop의 메모리를 4GB 이상으로 설정하세요.

### 권한 문제
```bash
chmod +x backend/gradlew
```

## 추가 명령어

```bash
# 로그 확인
make logs

# 테스트 실행
make test

# 서비스 중지
make dev-down

# 리소스 정리
make clean
```