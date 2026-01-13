import { Metadata } from 'next';
import { notFound } from 'next/navigation';
import { serverApi } from '@/lib/server-api';
import { getPostUrl } from '@/lib/config';
import { PostDetailClient } from './_components/detail-client';

interface PageProps {
  params: Promise<{
    id: string;
  }>;
}

// 동적 메타데이터 생성
export async function generateMetadata({ params }: PageProps): Promise<Metadata> {
  const { id } = await params;
  const postId = parseInt(id);
  const post = await serverApi.getPost(postId);

  if (!post) {
    return {
      title: '포스트를 찾을 수 없습니다',
    };
  }

  // 설명 생성 (content의 첫 200자 사용)
  const description = post.content
    ? post.content.substring(0, 200).replace(/\s+/g, ' ').trim() + '...'
    : `${post.blog.company} 기술 블로그의 ${post.title}`;

  const url = getPostUrl(post.id);

  return {
    title: `${post.title} - ${post.blog.company}`,
    description,
    keywords: post.keywords || [],
    authors: post.author ? [{ name: post.author }] : undefined,
    openGraph: {
      title: post.title,
      description,
      url,
      siteName: 'TeckBlogHub',
      type: 'article',
      locale: 'ko_KR',
      publishedTime: post.published_at,
      authors: post.author ? [post.author] : undefined,
      tags: post.keywords || [],
      images: [
        {
          url: post.blog.logo_url || '/og-image.svg',
          width: 1200,
          height: 630,
          alt: `${post.blog.company} - ${post.title}`,
        },
      ],
    },
    twitter: {
      card: 'summary_large_image',
      title: post.title,
      description,
      images: [post.blog.logo_url || '/og-image.svg'],
      creator: '@teckbloghub',
    },
    alternates: {
      canonical: url,
    },
  };
}

// 서버 컴포넌트
export default async function PostDetailPage({ params }: PageProps) {
  const { id } = await params;
  const postId = parseInt(id);
  const post = await serverApi.getPost(postId);

  if (!post) {
    notFound();
  }

  return <PostDetailClient post={post} />;
}
