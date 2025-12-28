# TechBlog Hub - FastAPI Service

FastAPI 기반의 RSS 수집 및 콘텐츠 처리 서비스입니다.

## 주요 기능

- RSS 피드 자동 수집
- 웹 콘텐츠 추출 (Playwright, Trafilatura)
- OpenAI를 활용한 콘텐츠 요약
- 스케줄러 기반 자동화 (매일 1시, 2시, 3시 실행)

## 기술 스택

- **Python**: 3.11
- **Framework**: FastAPI 0.121.3
- **Database**: PostgreSQL (asyncpg)
- **Web Scraping**: Playwright 1.56.0, Trafilatura 2.0.0
- **AI**: OpenAI API (gpt-4o-mini)
- **Scheduler**: APScheduler 3.10.4

## 로컬 개발 환경 설정

### 1. 의존성 설치

```bash
# 가상환경 생성
python -m venv .venv

# 가상환경 활성화
source .venv/bin/activate  # Mac/Linux
# .venv\Scripts\activate   # Windows

# 의존성 설치
pip install -r requirements.txt

# Playwright 브라우저 설치
playwright install chromium
```

### 2. 환경변수 설정

`.env` 파일을 생성하고 다음 내용을 설정합니다:

```env
APP_NAME="TechBlog Hub"
APP_VERSION="1.0.0"
DEBUG=True

# 데이터베이스 (로컬 PostgreSQL)
DATABASE_URL=postgresql+asyncpg://admin:password@localhost:5432/techbloghub_fastapi

# CORS 설정
ALLOWED_ORIGINS=["http://localhost:3000", "http://localhost:8000"]

# OpenAI API
OPENAI_API_KEY=your-openai-api-key-here
OPENAI_MODEL=gpt-4o-mini
OPENAI_MAX_TOKENS=10000

# RSS 프록시
RSS_PROXY_URL=https://rss-proxy.hoqein22.workers.dev/?url=

# 콘텐츠 추출 설정
MIN_CONTENT_LENGTH=500
MIN_TEXT_RATIO=0.01
PLAYWRIGHT_TIMEOUT=30000
```

### 3. 데이터베이스 초기화

```bash
# 로컬 PostgreSQL이 실행 중인지 확인
docker ps | grep postgres

# 스키마 및 테이블 생성 (SQLAlchemy create_all 사용)
python create_schema.py
```

### 4. 로컬 실행

```bash
# 개발 서버 실행
uvicorn main:app --reload --host 0.0.0.0 --port 8000

# 또는
python -m uvicorn main:app --reload
```

서버가 실행되면:

- API 문서: http://localhost:8000/docs
- Health Check: http://localhost:8000/health

## Docker 실행

### 1. Docker 이미지 빌드

```bash
# 이미지 빌드
docker build -t techbloghub-fastapi:latest .

# 특정 플랫폼용 빌드 (ECS용)
docker build --platform linux/amd64 -t techbloghub/fastapi:latest .
```

### 2. Docker 컨테이너 실행

#### 로컬 PostgreSQL 연결 (가장 일반적)

```bash
docker run -d \
  --name techbloghub-fastapi \
  -p 8000:8000 \
  -e DATABASE_URL="postgresql+asyncpg://admin:password@host.docker.internal:5432/techbloghub_fastapi" \
  -e OPENAI_API_KEY="your-openai-api-key" \
  -e DEBUG="True" \
  -e ALLOWED_ORIGINS='["http://localhost:3000", "http://localhost:8000"]' \
  -e RSS_PROXY_URL="https://rss-proxy.hoqein22.workers.dev/?url=" \
  techbloghub-fastapi:latest
```

**중요**:

- `host.docker.internal`은 Docker 컨테이너에서 호스트 머신의 localhost에 접근하는 방법입니다.
- Mac/Windows에서 작동하며, Linux에서는 `--add-host=host.docker.internal:host-gateway` 옵션 추가가 필요합니다.

#### Supabase PostgreSQL 연결

```bash
docker run -d \
  --name techbloghub-fastapi \
  -p 8000:8000 \
  -e DATABASE_URL="postgresql+asyncpg://postgres.xxxx:password@aws-0-ap-northeast-2.pooler.supabase.com:5432/postgres" \
  -e OPENAI_API_KEY="your-openai-api-key" \
  -e DEBUG="False" \
  -e ALLOWED_ORIGINS='["https://yourdomain.com"]' \
  -e RSS_PROXY_URL="https://rss-proxy.hoqein22.workers.dev/?url=" \
  techbloghub-fastapi:latest
```

### 3. 로그 확인

```bash
# 실시간 로그
docker logs -f techbloghub-fastapi

# 최근 로그 50줄
docker logs --tail 50 techbloghub-fastapi
```

### 4. 컨테이너 관리

```bash
# 컨테이너 상태 확인
docker ps | grep techbloghub-fastapi

# 컨테이너 중지
docker stop techbloghub-fastapi

# 컨테이너 삭제
docker rm techbloghub-fastapi

# 컨테이너 재시작
docker restart techbloghub-fastapi
```

## API 엔드포인트

API는 **Public API**와 **Admin API**로 분리되어 있습니다.

### Public API (인증 불필요)

#### Health Check
```bash
GET /health
```

#### Blogs
```bash
GET /api/v1/blogs           # 블로그 목록 조회
GET /api/v1/blogs/active    # 활성 블로그 목록
GET /api/v1/blogs/{id}      # 블로그 상세 조회
```

#### Posts
```bash
GET /api/v1/posts           # 포스트 목록 조회
GET /api/v1/posts/{id}      # 포스트 상세 조회
GET /api/v1/posts/search    # 포스트 전문 검색
```

#### Summaries
```bash
GET /api/v1/summaries/stream/{post_id}  # AI 요약 스트리밍 (SSE)
```

### Admin API (인증 필요)

**모든 Admin API는 HTTP Basic Authentication이 필요합니다.**

#### Blogs (Admin)
```bash
GET    /api/v1/admin/blogs        # 블로그 목록 조회 (모든 상태, 포스트 통계 포함)
GET    /api/v1/admin/blogs/{id}   # 블로그 상세 조회
POST   /api/v1/admin/blogs        # 블로그 생성
PATCH  /api/v1/admin/blogs/{id}   # 블로그 수정
DELETE /api/v1/admin/blogs/{id}   # 블로그 삭제

# 예시
curl -X POST http://localhost:8000/api/v1/admin/blogs \
  -u admin:admin123 \
  -H "Content-Type: application/json" \
  -d '{"name": "Example Blog", "company": "Example Inc", "rss_url": "https://example.com/rss", "site_url": "https://example.com"}'
```

#### Posts (Admin)
```bash
POST   /api/v1/admin/posts        # 포스트 생성
PATCH  /api/v1/admin/posts/{id}   # 포스트 수정
DELETE /api/v1/admin/posts/{id}   # 포스트 삭제
```

#### Scheduler (Admin)
```bash
GET  /api/v1/admin/scheduler/stats                        # 스케줄러 통계 조회
POST /api/v1/admin/scheduler/rss-collect                  # 모든 RSS 수집
POST /api/v1/admin/scheduler/rss-collect/{blog_id}        # 특정 블로그 RSS 수집
POST /api/v1/admin/scheduler/content-process              # 콘텐츠 배치 처리
POST /api/v1/admin/scheduler/content-process/{post_id}    # 단일 포스트 처리
POST /api/v1/admin/scheduler/content-process/blog/{blog_id} # 특정 블로그 포스트 처리
POST /api/v1/admin/scheduler/retry-failed                 # 실패 포스트 재시도

# 예시
curl -X POST http://localhost:8000/api/v1/admin/scheduler/rss-collect \
  -u admin:admin123
```

## 스케줄러

### 자동 실행 작업 (Asia/Seoul 시간대)

1. **RSS 수집** - 매일 01:00 AM KST

   - 활성화된 블로그의 RSS 피드 수집
   - 완료 시 Discord 알림 전송

2. **콘텐츠 처리** - 매일 02:00 AM KST

   - 수집된 포스트의 전체 콘텐츠 추출
   - 완료 시 Discord 알림 전송

3. **실패 포스트 재시도** - 매일 03:00 AM KST
   - 처리 실패한 포스트 재시도
   - 완료 시 Discord 알림 전송

### 수동 실행 (HTTP Basic Auth 필요)

```bash
# RSS 수집 수동 실행
curl -X POST http://localhost:8000/api/v1/admin/scheduler/rss-collect \
  -u admin:admin123

# 콘텐츠 처리 수동 실행
curl -X POST http://localhost:8000/api/v1/admin/scheduler/content-process \
  -u admin:admin123

# 재시도 수동 실행
curl -X POST http://localhost:8000/api/v1/admin/scheduler/retry-failed \
  -u admin:admin123

# 스케줄러 통계 조회
curl -X GET http://localhost:8000/api/v1/admin/scheduler/stats \
  -u admin:admin123
```

### Discord 알림

스케줄러 작업 완료 시 Discord 웹훅으로 결과를 전송합니다:

- ✅ 성공한 작업: 녹색 알림
- ⚠️ 일부 에러: 주황색 알림
- ❌ 실패한 작업: 빨간색 알림

`.env` 파일에 Discord 웹훅 설정이 필요합니다:
```env
DISCORD_WEBHOOK_URL=https://discord.com/api/webhooks/YOUR_WEBHOOK_ID/YOUR_WEBHOOK_TOKEN
DISCORD_WEBHOOK_ENABLED=true
```

## 테스트

```bash
# 단위 테스트 실행
pytest

# 커버리지 포함
pytest --cov=app tests/
```

## 프로젝트 구조

```
fastapi/
├── main.py                 # FastAPI 애플리케이션 진입점
├── requirements.txt        # Python 의존성
├── Dockerfile             # Docker 이미지 정의
├── .dockerignore          # Docker 빌드 제외 파일
├── alembic.ini            # Alembic 설정
├── alembic/               # 데이터베이스 마이그레이션
│   └── versions/          # 마이그레이션 파일들
└── app/
    ├── core/              # 핵심 설정
    │   ├── config.py      # Pydantic Settings
    │   └── database.py    # SQLAlchemy 설정
    ├── models/            # SQLAlchemy 모델
    │   ├── blog.py
    │   └── post.py
    ├── schemas/           # Pydantic 스키마
    │   ├── blog.py
    │   └── post.py
    ├── api/               # API 라우터
    │   └── v1/
    │       ├── blogs.py
    │       ├── posts.py
    │       ├── scheduler.py
    │       └── summaries.py
    └── services/          # 비즈니스 로직
        ├── rss_collector.py
        ├── content_extractor.py
        ├── content_processor.py
        ├── summary_generator.py
        └── scheduler.py
```

## 문제 해결

### 1. Playwright 브라우저 설치 오류

```bash
# Playwright 재설치
playwright install --with-deps chromium
```

### 2. 데이터베이스 연결 오류

- PostgreSQL이 실행 중인지 확인
- `.env` 파일의 `DATABASE_URL` 형식 확인 (`postgresql+asyncpg://` 사용)
- Docker에서 실행 시 `host.docker.internal` 사용

### 3. 포트 충돌

```bash
# 8000 포트 사용 중인 프로세스 확인
lsof -i :8000

# 다른 포트로 실행
uvicorn main:app --port 8001
```

## 환경 변수 전체 목록

| 변수명                    | 기본값                                      | 설명                          |
| ------------------------- | ------------------------------------------- | ----------------------------- |
| `APP_NAME`                | "TechBlog Hub"                              | 애플리케이션 이름             |
| `APP_VERSION`             | "1.0.0"                                     | 버전                          |
| `DEBUG`                   | True                                        | 디버그 모드                   |
| `DATABASE_URL`            | -                                           | PostgreSQL 연결 URL (필수)    |
| `ALLOWED_ORIGINS`         | []                                          | CORS 허용 도메인              |
| `ADMIN_USERNAME`          | "admin"                                     | Admin API 사용자명 (HTTP Basic Auth) |
| `ADMIN_PASSWORD`          | "admin123"                                  | Admin API 비밀번호 (HTTP Basic Auth) |
| `OPENAI_API_KEY`          | -                                           | OpenAI API 키 (필수)          |
| `OPENAI_MODEL`            | "gpt-4o-mini"                               | 사용할 OpenAI 모델            |
| `OPENAI_MAX_TOKENS`       | 10000                                       | 최대 토큰 수                  |
| `RSS_PROXY_URL`           | -                                           | RSS 프록시 URL                |
| `MIN_CONTENT_LENGTH`      | 500                                         | 최소 콘텐츠 길이              |
| `MIN_TEXT_RATIO`          | 0.01                                        | 최소 텍스트 비율              |
| `PLAYWRIGHT_TIMEOUT`      | 30000                                       | Playwright 타임아웃 (ms)      |
| `DISCORD_WEBHOOK_URL`     | ""                                          | Discord 웹훅 URL (선택)       |
| `DISCORD_WEBHOOK_ENABLED` | False                                       | Discord 알림 활성화 (선택)    |

## 라이선스

MIT

## 기여

Pull Request는 언제나 환영합니다!
