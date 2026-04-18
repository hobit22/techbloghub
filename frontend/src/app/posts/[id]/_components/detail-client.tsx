'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { format } from 'date-fns';
import { ko } from 'date-fns/locale';
import { ArrowLeft, ExternalLink, User, Clock, Building2 } from 'lucide-react';
import Image from 'next/image';
import Link from 'next/link';
import { PostSummary } from '@/components/PostSummary';
import { Post } from '@/types';

interface PostDetailClientProps {
  post: Post;
}

export function PostDetailClient({ post }: PostDetailClientProps) {
  const router = useRouter();
  const [logoError, setLogoError] = useState(false);
  const publishedDate = new Date(post.published_at);
  const excerpt = post.content?.replace(/\s+/g, ' ').trim() || '';

  return (
    <div className="min-h-screen bg-slate-50">
      <div className="max-w-5xl mx-auto p-4 lg:p-6">
        <div className="mb-6">
          <button
            onClick={() => router.back()}
            className="inline-flex items-center space-x-2 text-slate-600 hover:text-slate-900 transition-colors"
          >
            <ArrowLeft className="h-4 w-4" />
            <span>뒤로가기</span>
          </button>
        </div>

        <article className="rounded-2xl border border-slate-200 bg-white shadow-sm shadow-slate-200/50">
          <div className="border-b border-slate-200 p-6 lg:p-8">
            <div className="mb-6 flex items-start gap-4">
              <div className="flex-shrink-0 flex items-center gap-3">
                <div className="flex h-10 w-10 items-center justify-center overflow-hidden rounded-md bg-slate-100">
                  {post.blog.logo_url && !logoError ? (
                    <Image
                      src={post.blog.logo_url}
                      alt={`${post.blog.company} logo`}
                      width={40}
                      height={40}
                      className="w-full h-full object-contain"
                      onError={() => setLogoError(true)}
                    />
                  ) : (
                    <div className="flex h-10 w-10 items-center justify-center rounded-md bg-slate-900">
                      <Building2 className="h-6 w-6 text-white" />
                    </div>
                  )}
                </div>
                <div>
                  <div className="text-sm font-semibold uppercase tracking-[0.18em] text-slate-500">
                    {post.blog.company}
                  </div>
                  <div className="text-sm text-slate-600">{post.blog.name}</div>
                </div>
              </div>
            </div>

            <h1 className="mb-4 text-3xl font-bold leading-tight text-slate-950 lg:text-4xl">
              {post.title}
            </h1>

            <div className="mb-5 flex flex-wrap items-center gap-4 text-sm text-slate-600">
              {post.author && (
                <div className="flex items-center gap-1">
                  <User className="h-4 w-4" />
                  <span>{post.author}</span>
                </div>
              )}
              <div className="flex items-center gap-1">
                <Clock className="h-4 w-4" />
                <span>{format(publishedDate, 'yyyy년 MM월 dd일', { locale: ko })}</span>
              </div>
            </div>

            <div className="flex flex-col gap-3 sm:flex-row">
              <Link
                href={post.original_url}
                target="_blank"
                rel="noopener noreferrer"
                className="inline-flex items-center justify-center gap-2 rounded-lg bg-slate-950 px-4 py-2.5 text-sm font-semibold text-white hover:bg-slate-800 transition-colors"
              >
                원문 보기
                <ExternalLink className="h-4 w-4" />
              </Link>

              <Link
                href={post.blog.site_url}
                target="_blank"
                rel="noopener noreferrer"
                className="inline-flex items-center justify-center gap-2 rounded-lg border border-slate-300 px-4 py-2.5 text-sm font-semibold text-slate-700 hover:bg-slate-50 transition-colors"
              >
                {post.blog.company} 블로그
                <ExternalLink className="h-4 w-4" />
              </Link>
            </div>
          </div>

          <div className="grid gap-6 p-6 lg:grid-cols-[minmax(0,1fr)_280px] lg:p-8">
            <div className="space-y-6">
              <section className="rounded-xl border border-slate-200 bg-slate-50 p-5">
                <h2 className="mb-3 text-sm font-semibold uppercase tracking-[0.18em] text-slate-500">
                  요약 중심 읽기
                </h2>
                <p className="text-sm leading-6 text-slate-600">
                  TechBlogHub는 원문을 다시 쓰기보다 핵심 내용을 빠르게 파악한 뒤 원문으로 이동하는 경험을 지향합니다.
                </p>
              </section>

              <PostSummary postId={post.id} />
            </div>

            <aside className="space-y-4">
              <section className="rounded-xl border border-slate-200 p-5">
                <h2 className="mb-3 text-sm font-semibold uppercase tracking-[0.18em] text-slate-500">
                  원문 메모
                </h2>
                <p className="text-sm leading-6 text-slate-600">
                  {excerpt || '원문 본문은 아직 수집되지 않았습니다. 원문 링크를 통해 전체 글을 확인하세요.'}
                </p>
              </section>

              {!!post.keywords?.length && (
                <section className="rounded-xl border border-slate-200 p-5">
                  <h2 className="mb-3 text-sm font-semibold uppercase tracking-[0.18em] text-slate-500">
                    키워드
                  </h2>
                  <div className="flex flex-wrap gap-2">
                    {post.keywords.map((keyword) => (
                      <span
                        key={keyword}
                        className="rounded-full bg-slate-100 px-3 py-1 text-xs font-medium text-slate-700"
                      >
                        {keyword}
                      </span>
                    ))}
                  </div>
                </section>
              )}
            </aside>
          </div>
        </article>
      </div>
    </div>
  );
}
