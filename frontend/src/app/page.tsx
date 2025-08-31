import { Metadata } from 'next';
import ClientHomePage from '@/components/ClientHomePage';

export const metadata: Metadata = {
  title: '기술 블로그 모음',
  description: '국내 주요 IT 기업의 최신 기술 블로그 포스트를 한 곳에서 확인하세요. 네이버, 카카오, 쿠팡, 당근마켓 등의 개발 인사이트와 기술 트렌드를 실시간으로 업데이트합니다.',
  keywords: ['기술블로그', '개발블로그', '개발자', '프로그래밍', '소프트웨어', '기술트렌드'],
  openGraph: {
    title: '기술 블로그 모음 - TechBlogHub',
    description: '국내 주요 IT 기업의 최신 기술 블로그 포스트를 한 곳에서',
    images: [
      {
        url: '/og-image.png',
        width: 1200,
        height: 630,
        alt: '기술 블로그 모음',
      },
    ],
  },
  twitter: {
    card: 'summary_large_image',
    title: '기술 블로그 모음 - TechBlogHub',
    description: '국내 주요 IT 기업의 최신 기술 블로그 포스트를 한 곳에서',
  },
};

// Server Component for SEO and initial render
export default async function HomePage() {
  // Here we could potentially do initial data fetching for SSR
  // For now, we'll use the client component for all functionality
  
  return <ClientHomePage />;
}
