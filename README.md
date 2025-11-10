# TechBlogHub

국내 IT 기업 기술블로그를 한 곳에서 모아보는 플랫폼입니다. RSS 피드를 기반으로 자동으로 포스트를 수집하고, LLM을 활용한 자동 태그 분류 및 요약 생성 시스템으로 원하는 주제의 글을 쉽게 찾을 수 있습니다.

## 프로젝트 소개

TechBlogHub는 여러 IT 기업의 기술블로그를 일일이 방문하지 않고도 최신 기술 트렌드와 개발 노하우를 한 곳에서 확인할 수 있는 서비스입니다.

### 주요 특징

- **자동 수집**: RSS 피드를 통해 새로운 포스트를 자동으로 수집
- **AI 기반 태그**: OpenAI GPT를 활용한 자동 태그 분류 시스템
- **AI 기반 요약**: LangChain과 GPT-4o-mini를 활용한 포스트 요약 생성
- **하이브리드 콘텐츠 추출**: WebBaseLoader와 Playwright를 활용한 안정적인 웹 스크래핑
- **실시간 알림**: Discord 웹훅을 통한 새 포스트 알림 (선택적)
- **검색 & 필터링**: 블로그별, 카테고리별, 태그별 필터링 기능
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

- **Spring Boot 3.2** - Java 21 기반
- **Spring Data JPA** - 데이터 접근 계층
- **QueryDSL** - 타입 안전한 쿼리 작성
- **Spring Security** - 관리자 인증
- **Spring AI** - OpenAI 통합 (태그 생성)
- **Rome Tools** - RSS 피드 파싱
- **PostgreSQL** - 메인 데이터베이스

#### Content Processor (Python)

- **LangChain** - LLM 체인 및 문서 처리
- **OpenAI GPT-4o-mini** - AI 요약 생성
- **Playwright** - JavaScript 렌더링 웹 스크래핑
- **BeautifulSoup4** - HTML 파싱
- **SQLAlchemy** - 데이터베이스 ORM

#### Infrastructure

- **Docker & Docker Compose** - 컨테이너 오케스트레이션
- **Cloudflare Workers** - RSS 프록시
- **Discord Webhook** - 알림 시스템

## 주요 기능

### 1. RSS 기반 자동 크롤링

- 주기적으로 등록된 블로그의 RSS 피드를 확인
- 새로운 포스트 자동 수집 및 저장
- 중복 포스트 자동 필터링 (originalUrl 기반)

### 2. AI 기반 자동 태그 생성 (Spring AI + OpenAI)

- OpenAI GPT를 활용한 포스트 내용 분석
- 기술 스택, 주제별 자동 태그 생성
- 카테고리 자동 분류

### 3. AI 기반 콘텐츠 요약 (LangChain + GPT-4o-mini)

- **하이브리드 웹 스크래핑**
  - WebBaseLoader: 일반 웹사이트 처리 (빠름)
  - PlaywrightURLLoader: SPA 및 JavaScript 필요 사이트 (안정적)
  - 자동 Fallback: 실패 시 자동 재시도
- **구조화된 AI 요약**
  - 제목, 주요내용, 작성자, 핵심 내용 추출
  - LangChain 사용
- **안정성**
  - 개별 URL별 에러 격리
  - 상세한 로깅 및 모니터링
  - 자동 재시도 로직

### 4. 관리자 기능

- 블로그 등록/수정/삭제
- 포스트 수동 관리
- 배치 작업 트리거
- Basic Auth 인증

### 5. 사용자 기능

- 최신 포스트 목록 조회
- 블로그별 필터링
- 카테고리별 필터링
- 태그별 검색
- AI 생성 요약 조회
- 원본 블로그 바로가기

### 6. Discord 알림

- 새 포스트 발행 시 Discord 채널로 알림
- 환경변수로 활성화/비활성화 제어

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
│   └── README.md               # 프론트엔드 시작 가이드
│
├── backend/                     # Spring Boot 멀티모듈 백엔드
│   ├── domain/                 # 도메인 로직
│   ├── api-web/                # 공개 API
│   ├── admin-web/              # 관리자 API
│   ├── output-web/             # 외부 연동 (RSS, GPT, Discord)
│   ├── output-persistence/     # 데이터 영속성
│   ├── batch/                  # 배치 작업
│   ├── bootstrap/              # 애플리케이션 진입점
│   └── README.md               # 백엔드 시작 가이드
│
├── content_processor/           # Python 콘텐츠 처리기
│   ├── main.py                 # 메인 실행 스크립트
│   ├── processors/             # 처리 모듈
│   │   ├── database_manager.py
│   │   ├── content_processor.py
│   │   └── summary_generator.py
│   ├── config/                 # 설정
│   ├── logs/                   # 로그 파일
│   └── README.md               # Content Processor 가이드
│
├── docker/                      # Docker 설정
├── aws/                         # AWS Terraform 설정
├── Makefile                     # 개발 편의 명령어
└── README.md                    # 프로젝트 소개 (이 문서)
```

## 데이터 파이프라인

### 포스트 수집 및 처리 흐름

```
1. RSS Feed Crawling (Spring Boot Batch)
   └─> posts 테이블에 기본 정보 저장
       (title, url, published_date)

2. Tag Generation (Spring AI + OpenAI)
   └─> GPT가 포스트 내용 분석하여 태그 생성
       (tags, categories)

3. Content Extraction (Python Content Processor)
   └─> 하이브리드 웹 스크래핑
       WebBaseLoader → (total length 1000 이하일 경우) PlaywrightURLLoader
       └─> total_content 저장

4. Summary Generation (LangChain + GPT-4o-mini)
   └─> AI 기반 구조화된 요약 생성
       └─> summary_content 저장

5. User API Response (Spring Boot API)
   └─> 완전한 포스트 정보 제공
       (기본정보 + 태그 + 요약)
```

## 시작하기

각 하위 디렉토리의 README를 참고하세요:

- **백엔드 개발**: [backend/README.md](./backend/README.md)
- **프론트엔드 개발**: [frontend/README.md](./frontend/README.md)
- **콘텐츠 처리기**: [content_processor/README.md](./content_processor/README.md)

## 기여하기

### 블로그 등록 요청

새로운 기술블로그를 등록하고 싶다면 Issue를 생성하여 다음 정보를 제공해주세요:

- 블로그 이름
- RSS 피드 URL
- 기업/조직명
- 블로그 설명
