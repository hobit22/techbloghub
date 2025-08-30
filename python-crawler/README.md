# TechBlogHub Python Crawler Service

FastAPI 기반의 RSS 크롤링 및 NLP 태깅 서비스

## 기능

- RSS 피드 크롤링
- 비동기 HTTP 요청 처리
- 데이터베이스 저장 (PostgreSQL)
- RESTful API 제공
- 중복 포스트 자동 필터링

## 설치 및 실행

### 로컬 개발 환경

1. 가상환경 생성 및 활성화
```bash
python -m venv venv
source venv/bin/activate  # Linux/Mac
# venv\Scripts\activate  # Windows
```

2. 의존성 설치
```bash
pip install -r requirements.txt
```

3. 환경변수 설정
```bash
cp .env.example .env
# .env 파일을 수정하여 데이터베이스 설정
```

4. 애플리케이션 실행
```bash
uvicorn app.main:app --host 0.0.0.0 --port 8001 --reload
```

### Docker 실행

```bash
docker build -t techbloghub-crawler .
docker run -p 8001:8001 --env-file .env techbloghub-crawler
```

## API 엔드포인트

- `GET /health` - 헬스체크
- `GET /api/crawler/status` - 크롤링 상태 조회
- `POST /api/crawler/blogs/{blog_id}` - 특정 블로그 크롤링
- `POST /api/crawler/blogs/all` - 모든 활성 블로그 크롤링 (백그라운드)
- `POST /api/crawler/blogs/all/sync` - 모든 활성 블로그 크롤링 (동기)

## API 문서

- Swagger UI: http://localhost:8001/api/docs
- ReDoc: http://localhost:8001/api/redoc