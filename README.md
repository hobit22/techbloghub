# TechBlogHub

국내 IT 기업 기술블로그를 한 곳에서 모아보는 플랫폼입니다. RSS 피드를 기반으로 자동으로 포스트를 수집하고, AI를 활용한 콘텐츠 요약 생성으로 원하는 주제의 글을 쉽게 찾을 수 있습니다.

## 프로젝트 소개

TechBlogHub는 여러 IT 기업의 기술블로그를 일일이 방문하지 않고도 최신 기술 트렌드와 개발 노하우를 한 곳에서 확인할 수 있는 서비스입니다.

### 주요 특징

- **자동 수집**: RSS 피드를 통해 새로운 포스트를 자동으로 수집
- **AI 기반 요약**: OpenAI GPT-4o-mini를 활용한 포스트 요약 생성
- **하이브리드 콘텐츠 추출**: Playwright와 Trafilatura를 활용한 안정적인 웹 스크래핑
- **검색 & 필터링**: 블로그별 필터링 및 검색 기능
- **반응형 디자인**: 모바일/데스크톱 모두 최적화된 UI

## 시스템 구성

### 기술 스택

#### Frontend

- **Next.js 15** - App Router 기반 React 프레임워크
- **TypeScript** - 타입 안정성
- **Tailwind CSS 3.4** - 유틸리티 기반 스타일링
- **React Query** - 서버 상태 관리
- **Lucide React** - 아이콘 라이브러리

#### Backend

- **FastAPI 0.121.3** - Python 3.11 기반 고성능 API 프레임워크
- **SQLAlchemy 2.0** - 비동기 ORM
- **AsyncPG** - PostgreSQL 비동기 드라이버
- **Playwright 1.56** - 웹 스크래핑
- **Trafilatura 2.0** - 콘텐츠 추출
- **OpenAI GPT-4o-mini** - AI 요약 생성
- **APScheduler 3.10** - 스케줄러
- **FeedParser** - RSS 피드 파싱
- **PostgreSQL** - 메인 데이터베이스

#### Infrastructure

- **Docker** - 컨테이너화
- **Cloudflare Workers** - RSS 프록시

## 주요 기능

### 1. RSS 기반 자동 크롤링

- 주기적으로 등록된 블로그의 RSS 피드를 확인
- 새로운 포스트 자동 수집 및 저장
- 중복 포스트 자동 필터링

### 2. AI 기반 콘텐츠 요약

- **하이브리드 웹 스크래핑**
  - Trafilatura: 일반 웹사이트 고속 처리
  - Playwright: SPA 및 JavaScript 렌더링 사이트
  - 자동 Fallback: 실패 시 자동 재시도
- **구조화된 AI 요약**
  - OpenAI GPT-4o-mini를 활용한 요약 생성
  - 핵심 내용 추출

### 3. 사용자 기능

- 최신 포스트 목록 조회
- 블로그별 필터링
- 포스트 검색
- AI 생성 요약 조회
- 원본 블로그 바로가기

## 프로젝트 구조

```
techbloghub/
├── frontend/                    # Next.js 프론트엔드
│   ├── src/
│   │   ├── app/                # App Router 페이지
│   │   ├── components/         # React 컴포넌트
│   │   ├── hooks/              # 커스텀 훅
│   │   ├── lib/                # 유틸리티 함수
│   │   └── types/              # TypeScript 타입
│   └── README.md
│
├── fastapi/                     # FastAPI 백엔드
│   ├── main.py                 # 애플리케이션 진입점
│   ├── app/
│   │   ├── core/               # 핵심 설정 (config, database)
│   │   ├── models/             # SQLAlchemy 모델
│   │   ├── schemas/            # Pydantic 스키마
│   │   ├── api/                # API 라우터
│   │   └── services/           # 비즈니스 로직
│   ├── requirements.txt
│   ├── Dockerfile
│   └── README.md
│
└── README.md                    # 프로젝트 소개 (이 문서)
```

## 데이터 파이프라인

### 포스트 수집 및 처리 흐름

```
1. RSS Feed Collection (APScheduler - 매일 01:00)
   └─> posts 테이블에 기본 정보 저장
       (title, url, published_date, blog_id)

2. Content Extraction (APScheduler - 매일 02:00)
   └─> 하이브리드 웹 스크래핑
       Trafilatura → (실패 시) Playwright
       └─> total_content 저장

3. Summary Generation (OpenAI GPT-4o-mini)
   └─> AI 기반 요약 생성
       └─> summary_content 저장

4. User API Response (FastAPI)
   └─> 포스트 정보 제공
       (기본정보 + 요약)
```

## 시작하기

### 빠른 시작

```bash
# 1. 환경 변수 설정
cp .env.example .env
# .env 파일 편집하여 OPENAI_API_KEY 등 설정

# 2. 데이터베이스 시작 (PostgreSQL)
docker run -d \
  --name postgres \
  -e POSTGRES_USER=admin \
  -e POSTGRES_PASSWORD=password \
  -e POSTGRES_DB=techbloghub_fastapi \
  -p 5432:5432 \
  postgres:15

# 3. 백엔드 실행
cd fastapi
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
playwright install chromium
uvicorn main:app --reload

# 4. 프론트엔드 실행 (별도 터미널)
cd frontend
npm install
npm run dev
```

### 상세 가이드

- **백엔드**: [fastapi/README.md](./fastapi/README.md)
- **프론트엔드**: [frontend/README.md](./frontend/README.md)

## 기여하기

### 블로그 등록 요청

새로운 기술블로그를 등록하고 싶다면 Issue를 생성하여 다음 정보를 제공해주세요:

- 블로그 이름
- RSS 피드 URL
- 기업/조직명
- 블로그 설명
