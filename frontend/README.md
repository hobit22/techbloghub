# TechBlogHub Frontend

Next.js 15 기반의 현대적인 프론트엔드 애플리케이션입니다. App Router와 TypeScript를 사용하여 구축되었습니다.

## 기술 스택

- **Next.js 15** - App Router, React Server Components
- **React 19** - 최신 React
- **TypeScript** - 타입 안정성
- **Tailwind CSS 3.4** - 유틸리티 우선 스타일링
- **React Query (@tanstack/react-query)** - 서버 상태 관리
- **Axios** - HTTP 클라이언트
- **Lucide React** - 아이콘 라이브러리
- **date-fns** - 날짜 포맷팅

## 프로젝트 구조

```
frontend/
├── src/
│   ├── app/                    # App Router 페이지
│   │   ├── page.tsx           # 홈 페이지 (포스트 목록)
│   │   ├── posts/             # 포스트 상세 페이지
│   │   ├── admin/             # 관리자 페이지
│   │   ├── api/               # API Routes
│   │   └── layout.tsx         # 루트 레이아웃
│   │
│   ├── components/             # React 컴포넌트
│   │   ├── PostCard.tsx       # 포스트 카드
│   │   ├── BlogFilter.tsx     # 블로그 필터
│   │   ├── CategoryFilter.tsx # 카테고리 필터
│   │   ├── TagFilter.tsx      # 태그 필터
│   │   └── ...
│   │
│   ├── hooks/                  # 커스텀 훅
│   │   ├── usePosts.ts        # 포스트 목록 조회
│   │   ├── usePost.ts         # 포스트 상세 조회
│   │   ├── useBlogs.ts        # 블로그 목록 조회
│   │   └── ...
│   │
│   ├── lib/                    # 유틸리티 함수
│   │   ├── api.ts             # API 클라이언트 설정
│   │   ├── utils.ts           # 공통 유틸리티
│   │   └── ...
│   │
│   └── types/                  # TypeScript 타입 정의
│       ├── post.ts
│       ├── blog.ts
│       └── ...
│
├── public/                     # 정적 파일
├── tailwind.config.ts          # Tailwind 설정
├── tsconfig.json               # TypeScript 설정
└── package.json
```

## 시작하기

### 필수 요구사항

- **Node.js 20** 이상
- **npm** 또는 **yarn**

### 설치

```bash
npm install
# 또는
yarn install
```

### 환경 변수 설정

`.env.local` 파일 생성:

```env
# Backend API URL
NEXT_PUBLIC_API_URL=http://localhost:8080

# Google Analytics (선택적)
NEXT_PUBLIC_GA_ID=G-XXXXXXXXXX
```

### 개발 서버 실행

```bash
npm run dev
# 또는
yarn dev
```

애플리케이션이 http://localhost:3000 에서 실행됩니다.

### 빌드

```bash
# 프로덕션 빌드
npm run build

# 프로덕션 서버 실행
npm run start

# ESLint 실행
npm run lint
```

## 주요 기능

### 1. 포스트 목록 조회

- 최신 포스트를 카드 형식으로 표시
- 무한 스크롤 또는 페이지네이션
- 반응형 그리드 레이아웃

**페이지**: `src/app/page.tsx`

### 2. 필터링 및 검색

- **블로그별 필터**: 특정 블로그의 포스트만 보기
- **카테고리별 필터**: 카테고리로 그룹화
- **태그 검색**: AI가 생성한 태그로 검색

**컴포넌트**:

- `src/components/BlogFilter.tsx`
- `src/components/CategoryFilter.tsx`
- `src/components/TagFilter.tsx`

### 3. 포스트 상세 페이지

- 포스트 제목, 발행일, 블로그 정보
- AI 생성 요약 (summary_content)
- 원본 블로그로 이동 링크
- 관련 태그 표시

**페이지**: `src/app/posts/[id]/page.tsx`

### 4. 관리자 페이지

- 블로그 등록/수정/삭제
- 포스트 관리
- 배치 작업 트리거

**페이지**: `src/app/admin/`

## API 통합

### API 클라이언트 설정

`src/lib/api.ts`:

```typescript
import axios from "axios";

export const api = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080",
  headers: {
    "Content-Type": "application/json",
  },
});
```

### React Query 사용

`src/hooks/usePosts.ts`:

```typescript
import { useQuery } from "@tanstack/react-query";
import { api } from "@/lib/api";

export function usePosts(filters?: PostFilters) {
  return useQuery({
    queryKey: ["posts", filters],
    queryFn: async () => {
      const { data } = await api.get("/api/posts", { params: filters });
      return data;
    },
  });
}
```

**사용 예시**:

```typescript
function PostList() {
  const { data: posts, isLoading } = usePosts({ blogId: 1 });

  if (isLoading) return <div>Loading...</div>;

  return (
    <div>
      {posts.map((post) => (
        <PostCard key={post.id} post={post} />
      ))}
    </div>
  );
}
```

## 스타일링

### Tailwind CSS

프로젝트는 Tailwind CSS를 사용합니다.

**설정 파일**: `tailwind.config.ts`

**예시**:

```tsx
<div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
  <div className="bg-white rounded-lg shadow-md p-6 hover:shadow-lg transition">
    <h2 className="text-xl font-bold mb-2">제목</h2>
    <p className="text-gray-600">설명</p>
  </div>
</div>
```

### 글로벌 스타일

`src/app/globals.css`:

- Tailwind 기본 설정
- 커스텀 CSS 변수
- 다크 모드 지원 (예정)

## 라우팅

### App Router

Next.js 15 App Router 사용:

| 경로          | 파일                          | 설명             |
| ------------- | ----------------------------- | ---------------- |
| `/`           | `src/app/page.tsx`            | 홈 (포스트 목록) |
| `/posts/[id]` | `src/app/posts/[id]/page.tsx` | 포스트 상세      |
| `/admin`      | `src/app/admin/page.tsx`      | 관리자 대시보드  |
| `/api/*`      | `src/app/api/*/route.ts`      | API Routes       |

## 배포

### Vercel 배포 (권장)

```bash
# Vercel CLI 설치
npm i -g vercel

# 배포
vercel
```

환경 변수 설정:

- `NEXT_PUBLIC_API_URL`: 프로덕션 API URL

### Docker 배포

```bash
# 이미지 빌드
docker build -t techbloghub-frontend .

# 컨테이너 실행
docker run -p 3000:3000 techbloghub-frontend
```

### Static Export (옵션)

```bash
# next.config.js에 output: 'export' 추가 후
npm run build

# out/ 디렉토리를 정적 호스팅 서비스에 배포
```
