# FastAPI 서비스 변경 사항

## 2025-12-05 - 주요 업데이트

### 1. API 구조 변경: Public API와 Admin API 분리

**변경 전:**
- 모든 API가 `/api/v1` 아래에 인증 없이 접근 가능

**변경 후:**
- **Public API** (`/api/v1/*`): 조회 작업 (GET) - 인증 불필요
  - `GET /api/v1/blogs` - 블로그 목록
  - `GET /api/v1/posts` - 포스트 목록
  - `GET /api/v1/posts/search` - 포스트 검색
  - `GET /api/v1/summaries/stream/{post_id}` - AI 요약

- **Admin API** (`/api/v1/admin/*`): 관리 작업 - HTTP Basic Auth 필요
  - `POST/PATCH/DELETE /api/v1/admin/blogs` - 블로그 관리
  - `POST/PATCH/DELETE /api/v1/admin/posts` - 포스트 관리
  - `POST /api/v1/admin/scheduler/*` - 스케줄러 수동 트리거

**마이그레이션 가이드:**
```bash
# 기존 (인증 없음)
curl -X POST http://localhost:8000/api/v1/blogs -d '{...}'

# 새로운 방식 (HTTP Basic Auth 필요)
curl -X POST http://localhost:8000/api/v1/admin/blogs \
  -u admin:admin123 \
  -d '{...}'
```

### 2. Discord 웹훅 알림 기능 추가

**새로운 기능:**
- 스케줄러 작업 완료 시 Discord로 결과 알림 전송
- RSS 수집, 콘텐츠 처리, 재시도 작업 각각에 대한 상세 리포트
- 에러 발생 시 즉시 알림

**설정 방법:**
```env
DISCORD_WEBHOOK_URL=https://discord.com/api/webhooks/YOUR_WEBHOOK_ID/YOUR_WEBHOOK_TOKEN
DISCORD_WEBHOOK_ENABLED=true
```

**알림 종류:**
- 📡 RSS 수집 완료 (새 포스트 수, 에러 수 등)
- 🔄 콘텐츠 처리 완료 (성공/실패 통계)
- ♻️ 재시도 완료 (복구된 포스트 수)
- 🚨 에러 알림 (작업 실패 시)
- ⏰ 스케줄러 시작 알림

### 3. 스케줄러 Timezone 설정 수정

**변경 전:**
- Timezone 설정 없음 (UTC 기본값)
- 스케줄러가 의도한 시간에 실행되지 않을 수 있음

**변경 후:**
- `Asia/Seoul` (KST) timezone 명시적으로 설정
- 매일 오전 1시, 2시, 3시 (한국 시간) 정확히 실행

**코드 변경:**
```python
# scheduler.py
from pytz import timezone
KST = timezone('Asia/Seoul')
scheduler = AsyncIOScheduler(timezone=KST)

# CronTrigger에도 timezone 명시
scheduler.add_job(
    collect_rss_job,
    trigger=CronTrigger(hour=1, minute=0, timezone=KST),
    ...
)
```

### 4. 새로운 환경 변수

```env
# Admin API 인증
ADMIN_USERNAME=admin
ADMIN_PASSWORD=change-in-production

# Discord 알림 (선택사항)
DISCORD_WEBHOOK_URL=https://discord.com/api/webhooks/...
DISCORD_WEBHOOK_ENABLED=true
```

### 5. 파일 구조 변경

**추가된 파일:**
```
fastapi/
├── app/
│   ├── core/
│   │   └── auth.py                    # Admin API 인증
│   ├── services/
│   │   └── discord_notifier.py         # Discord 웹훅 서비스
│   └── api/v1/
│       ├── public/                     # Public API (인증 불필요)
│       │   ├── blogs.py
│       │   ├── posts.py
│       │   └── summaries.py
│       └── admin/                      # Admin API (인증 필요)
│           ├── blogs.py
│           ├── posts.py
│           └── scheduler.py
```

**제거된 파일:**
- `app/api/v1/blogs.py` → `app/api/v1/public/blogs.py` + `app/api/v1/admin/blogs.py`로 분리
- `app/api/v1/posts.py` → `app/api/v1/public/posts.py` + `app/api/v1/admin/posts.py`로 분리
- `app/api/v1/scheduler.py` → `app/api/v1/admin/scheduler.py`로 이동
- `app/api/v1/summaries.py` → `app/api/v1/public/summaries.py`로 이동

### 6. Breaking Changes

**⚠️ 주의: 하위 호환성 없음**

1. **Admin 작업 URL 변경:**
   - `POST /api/v1/blogs` → `POST /api/v1/admin/blogs`
   - `PATCH /api/v1/blogs/{id}` → `PATCH /api/v1/admin/blogs/{id}`
   - `DELETE /api/v1/blogs/{id}` → `DELETE /api/v1/admin/blogs/{id}`
   - 포스트, 스케줄러 API도 동일하게 변경

2. **인증 필수:**
   - 모든 Admin API는 HTTP Basic Auth 필요
   - 인증 없이 요청 시 `401 Unauthorized` 반환

3. **환경 변수 필수:**
   - `ADMIN_USERNAME`, `ADMIN_PASSWORD` 환경 변수 설정 권장

### 테스트 방법

```bash
# 1. 환경 변수 설정
cp .env.example .env
# .env 파일에서 ADMIN_USERNAME, ADMIN_PASSWORD, DISCORD_WEBHOOK_URL 설정

# 2. 서버 실행
uvicorn main:app --reload

# 3. Public API 테스트 (인증 불필요)
curl http://localhost:8000/api/v1/blogs

# 4. Admin API 테스트 (인증 필요)
curl http://localhost:8000/api/v1/admin/scheduler/stats \
  -u admin:admin123

# 5. Swagger UI 확인
open http://localhost:8000/docs
```

### 롤백 방법

이전 버전으로 롤백하려면:
```bash
git checkout <previous-commit-hash>
```

## 이전 버전과의 호환성

**Frontend/Backend 통합:**
- Frontend에서 관리자 API 호출 시 HTTP Basic Auth 사용 필요
- 조회 API는 변경 없음 (하위 호환)

**Spring Boot Backend:**
- FastAPI는 독립적으로 작동하므로 Spring Boot에 영향 없음
