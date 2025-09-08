import { Metadata } from 'next';
import { Suspense } from 'react';
import ClientHomePage from '@/components/ClientHomePage';
import { fetchServerSideData } from '@/lib/server-api';
import { SkeletonGrid } from '@/components/SkeletonCard';

// 동적 메타데이터 생성
export async function generateMetadata({
  searchParams,
}: {
  searchParams: Promise<{ [key: string]: string | string[] | undefined }>;
}): Promise<Metadata> {
  const params = await searchParams;
  const keyword = params.keyword as string;
  const blogIds = params.blogIds as string;
  const tags = params.tags as string;
  const categories = params.categories as string;

  // 필터 조합에 따른 타이틀과 설명 생성
  let title = '기술 블로그 모음';
  let description = '국내 주요 IT 기업의 최신 기술 블로그 포스트를 한 곳에서 확인하세요.';

  if (keyword) {
    title = `"${keyword}" 검색 결과 - TechBlogHub`;
    description = `"${keyword}" 관련 기술 블로그 포스트를 모아보세요. 국내 IT 대기업의 개발 인사이트와 기술 트렌드를 확인하세요.`;
  } else if (tags) {
    const tagList = tags.split(',').slice(0, 3).join(', ');
    title = `${tagList} 태그 포스트 - TechBlogHub`;
    description = `${tagList} 태그가 포함된 기술 블로그 포스트들을 모아보세요.`;
  } else if (categories) {
    const categoryList = categories.split(',').slice(0, 2).join(', ');
    title = `${categoryList} 카테고리 포스트 - TechBlogHub`;
    description = `${categoryList} 분야의 기술 블로그 포스트들을 모아보세요.`;
  }

  return {
    title: {
      default: title,
      template: '%s | TechBlogHub',
    },
    description,
    keywords: [
      '기술블로그',
      '개발블로그',
      '개발자',
      keyword,
      ...(tags ? tags.split(',') : []),
      ...(categories ? categories.split(',') : []),
    ].filter(Boolean),
    openGraph: {
      title,
      description,
      type: 'website',
      images: [
        {
          url: '/og-image.png',
          width: 1200,
          height: 630,
          alt: title,
        },
      ],
    },
    twitter: {
      card: 'summary_large_image',
      title,
      description,
      images: ['/og-image.png'],
    },
  };
}

interface HomePageProps {
  searchParams: Promise<{ [key: string]: string | string[] | undefined }>;
}

// 메인 Server Component
export default async function HomePage({ searchParams }: HomePageProps) {
  const params = await searchParams;
  // URL 파라미터를 string으로 변환
  const normalizedSearchParams = {
    keyword: params.keyword as string,
    blogIds: params.blogIds as string,
    tags: params.tags as string,
    categories: params.categories as string,
    page: params.page as string,
  };

  // 서버에서 초기 데이터 페칭
  const serverData = await fetchServerSideData(normalizedSearchParams);

  return (
    <Suspense 
      fallback={
        <div className="min-h-screen bg-slate-50">
          <div className="max-w-7xl mx-auto p-4 lg:p-6">
            <SkeletonGrid count={12} />
          </div>
        </div>
      }
    >
      <ClientHomePage 
        initialData={serverData.initialPosts}
        initialBlogs={serverData.blogs}
        initialCategories={serverData.categories}
        initialTags={serverData.tags}
        initialHasFilters={serverData.hasFilters}
        searchSummary={serverData.searchSummary}
      />
    </Suspense>
  );
}
