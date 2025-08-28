# TechBlogHub

기술블로그 모음 사이트 - RSS 기반으로 국내 IT 대기업 기술블로그를 수집하고 검색할 수 있는 플랫폼

## 아키텍처

- **Frontend**: Next.js (TypeScript)
- **Backend**: Spring Boot
- **Database**: RDB + ElasticSearch
- **크롤링**: RSS 피드 기반
- **검색**: ElasticSearch 유사도 기반

## 주요 기능

- RSS 피드 기반 자동 블로그 포스트 수집
- ElasticSearch 기반 검색, 분류, 태그 기능
- Pull Request 기반 블로그 등록 워크플로우
- 외부 링크 이동 (원본 기술블로그로 직접 이동)

## 프로젝트 구조

```
techbloghub/
├── backend/          # Spring Boot 백엔드
├── frontend/         # Next.js 프론트엔드
├── docs/            # 문서
├── docker/          # Docker 설정
└── .github/         # GitHub Actions 워크플로우
```

## 시작하기

### 백엔드 실행
```bash
cd backend
./gradlew bootRun
```

### 프론트엔드 실행
```bash
cd frontend
npm run dev
```

## 기술 스택

### Backend
- Spring Boot 3.x
- Spring Data JPA
- Spring Data Elasticsearch
- Spring Security
- H2/PostgreSQL

### Frontend
- Next.js 14
- TypeScript
- Tailwind CSS
- React Query

### Infrastructure
- ElasticSearch
- Docker & Docker Compose
- GitHub Actions

## 블로그 등록

새로운 기술블로그를 등록하려면 Pull Request를 통해 등록 요청을 보내주세요.