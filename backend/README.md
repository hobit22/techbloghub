# TechBlogHub Backend

Spring Boot 기반의 멀티모듈 백엔드 애플리케이션입니다. 헥사고날 아키텍처를 적용하여 비즈니스 로직을 프레임워크와 독립적으로 관리합니다.

## 기술 스택

- **Java 21**
- **Spring Boot 3.2**
- **Spring Data JPA** - 데이터 접근
- **QueryDSL** - 타입 안전한 쿼리
- **Spring Security** - 관리자 인증
- **Spring AI** - OpenAI 통합 (자동 태그 생성)
- **Rome Tools** - RSS 피드 파싱
- **PostgreSQL** - 메인 데이터베이스

## 아키텍처

### 멀티 모듈 구조 (Hexagonal Architecture)

```
backend/
├── domain/                 # 핵심 비즈니스 로직
│   ├── model/             # 도메인 엔티티 (Post, Blog, Category, Tag)
│   ├── port/in/           # 입력 포트 (Use Case 인터페이스)
│   ├── port/out/          # 출력 포트 (Repository 인터페이스)
│   └── service/           # 비즈니스 로직 구현
│
├── api-web/                # 공개 REST API
│   ├── controller/        # API 컨트롤러
│   └── dto/               # 응답 DTO
│
├── admin-web/              # 관리자 API
│   ├── controller/        # 관리자 컨트롤러
│   ├── dto/               # 요청/응답 DTO
│   └── config/            # Spring Security 설정
│
├── output-web/             # 외부 연동 모듈
│   ├── rss/               # RSS 피드 크롤링
│   ├── gpt/               # OpenAI GPT 태그 생성
│   └── discord/           # Discord 웹훅 알림
│
├── output-persistence/     # 데이터 영속성
│   ├── entity/            # JPA 엔티티
│   ├── repository/        # JPA 레포지토리
│   └── adapter/           # 레포지토리 구현체
│
├── batch/                  # 배치 작업
│   └── scheduler/         # 크롤링 스케줄러
│
└── bootstrap/              # 애플리케이션 진입점
    └── resources/         # 설정 파일
```

## 시작하기

### 필수 요구사항

- Java 21 이상
- Docker & Docker Compose (PostgreSQL 사용 시)
- OpenAI API Key (자동 태그 기능 사용 시)

### 환경 변수 설정

`.env` 파일 생성:

```env
# OpenAI API Key (필수)
OPENAI_API_KEY=your_openai_api_key

# 데이터베이스 (선택 - 로컬 개발 시 H2 사용 가능)
DATABASE_URL=jdbc:postgresql://localhost:5432/techbloghub
DATABASE_USERNAME=admin
DATABASE_PASSWORD=password

# RSS 프록시 (선택적)
RSS_PROXY_URL=https://rss-proxy.example.workers.dev/?url=

# Discord 웹훅 (선택적)
DISCORD_WEBHOOK_URL=
DISCORD_WEBHOOK_ENABLED=false
```

### 실행 방법

#### 1. PostgreSQL 시작 (Docker)

```bash
# 프로젝트 루트에서
cd ..
make dev
```

#### 2. 애플리케이션 실행

```bash
# 방법 1: Gradle 직접 실행
./gradlew bootRun

# 방법 2: Makefile 사용 (루트 디렉토리)
cd ..
make backend-run
```

#### 3. 접속 확인

- **API**: http://localhost:8080
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **H2 Console** (local 프로파일): http://localhost:8080/h2-console

## 개발 가이드

### 빌드 및 테스트

```bash
# 전체 빌드
./gradlew build

# 테스트 제외 빌드
./gradlew build -x test

# 테스트 실행
./gradlew test

# 특정 모듈만 테스트
./gradlew :domain:test
./gradlew :api-web:test

# 클린 빌드
./gradlew clean build
```

### 프로파일 설정

애플리케이션은 다음 프로파일을 지원합니다:

#### local (기본값)

```bash
./gradlew bootRun
# 또는
./gradlew bootRun --args='--spring.profiles.active=local'
```

- H2 인메모리 데이터베이스
- H2 콘솔 활성화
- DEBUG 로그 레벨
- 스케줄러 활성화

#### docker

```bash
./gradlew bootRun --args='--spring.profiles.active=docker'
```

- PostgreSQL 데이터베이스
- 환경변수로 DB 설정
- INFO 로그 레벨

#### prod

```bash
./gradlew bootRun --args='--spring.profiles.active=prod'
```

- PostgreSQL 데이터베이스
- 프로덕션 최적화 설정
- INFO 로그 레벨
- DDL validate 모드

### API 문서

#### Swagger UI

애플리케이션 실행 후 http://localhost:8080/swagger-ui.html 접속

#### 주요 엔드포인트

**공개 API (api-web)**

- `GET /api/posts` - 포스트 목록 (페이징, 필터링)
- `GET /api/posts/{id}` - 포스트 상세
- `GET /api/blogs` - 블로그 목록
- `GET /api/categories` - 카테고리 목록
- `GET /api/tags` - 태그 목록

**관리자 API (admin-web)** - Basic Auth 필요

- `POST /admin/blogs` - 블로그 등록
- `PUT /admin/blogs/{id}` - 블로그 수정
- `DELETE /admin/blogs/{id}` - 블로그 삭제
- `PUT /admin/posts/{id}` - 포스트 수정
- `DELETE /admin/posts/{id}` - 포스트 삭제

### 관리자 인증

Basic Authentication:

- **Username**: `admin`
- **Password**: `admin123`

## 주요 기능

### 1. RSS 피드 크롤링

`output-web/rss` 모듈에서 구현:

- 등록된 블로그의 RSS 피드를 주기적으로 확인
- 새로운 포스트 자동 수집
- `originalUrl` 기반 중복 방지

스케줄 설정 (`batch/application.yml`):

```yaml
crawler:
  schedule:
    hourly: "0 0 * * * *" # 매시간
    daily: "0 0 0 * * *" # 매일 자정
```

### 2. AI 기반 자동 태그 생성

`output-web/gpt` 모듈에서 구현:

- Spring AI + OpenAI GPT 사용
- 포스트 제목과 설명을 분석하여 자동 태그 생성
- 카테고리 자동 분류

### 3. Discord 알림

`output-web/discord` 모듈에서 구현:

- 새 포스트 크롤링 시 Discord 웹훅으로 알림
- 환경변수로 활성화/비활성화 제어

## 데이터베이스

### 로컬 개발 (H2)

```yaml
# 기본 설정 (local 프로파일)
spring:
  datasource:
    url: jdbc:h2:mem:techbloghub
    username: sa
    password: password
```

H2 Console: http://localhost:8080/h2-console

### Docker/Production (PostgreSQL)

```yaml
# docker/prod 프로파일
spring:
  datasource:
    url: ${DATABASE_URL}
    username: ${DATABASE_USERNAME}
    password: ${DATABASE_PASSWORD}
```

### 스키마 관리

- **local**: `ddl-auto: create-drop` (매번 재생성)
- **docker**: `ddl-auto: update` (자동 업데이트)
- **prod**: `ddl-auto: validate` (검증만)

## 아키텍처 패턴

### 헥사고날 아키텍처 원칙

1. **도메인 독립성**: `domain` 모듈은 프레임워크에 의존하지 않음
2. **포트와 어댑터**: 명확한 인터페이스로 계층 분리
3. **의존성 역전**: 외부 계층이 내부(도메인)에 의존

### 주요 패턴

**빌더 패턴**

```java
Post post = Post.builder()
    .title("제목")
    .originalUrl("https://...")
    .publishedDate(LocalDateTime.now())
    .build();
```

**DTO 변환**

```java
// Controller에서
PostResponse response = PostResponse.from(post);
```

**Repository 포트**

```java
// domain/port/out/
public interface PostRepositoryPort {
    Post save(Post post);
    Optional<Post> findById(Long id);
}

// output-persistence/adapter/
public class PostRepositoryAdapter implements PostRepositoryPort {
    // JPA 구현
}
```

## 트러블슈팅

### 일반적인 문제

**1. OpenAI API 오류**

```
Error: OpenAI API key not found
```

→ `.env` 파일에 `OPENAI_API_KEY` 확인

**2. PostgreSQL 연결 실패**

```
Error: Connection refused
```

→ `make dev`로 PostgreSQL 시작 확인

**3. 포트 충돌**

```
Error: Port 8080 already in use
```

→ 실행 중인 서비스 종료 또는 포트 변경

**4. Gradle 빌드 실패**

```
./gradlew clean build --refresh-dependencies
```

## 배포

### Docker 이미지 빌드

```bash
# 루트 디렉토리에서
docker-compose build backend
```

### 환경별 실행

```bash
# 개발 환경
docker-compose -f docker/docker-compose.dev.yml up

# 프로덕션
docker-compose up -d
```
