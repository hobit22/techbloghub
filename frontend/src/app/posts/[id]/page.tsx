'use client';

import { useState } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { usePost } from '@/hooks/usePost';
import { format } from 'date-fns';
import { ko } from 'date-fns/locale';
import { ArrowLeft, ExternalLink, User, Clock, Building2 } from 'lucide-react';
import Image from 'next/image';
import Link from 'next/link';
import { PostSummary } from '@/components/PostSummary';

export default function PostDetailPage() {
  const params = useParams();
  const router = useRouter();
  const postId = parseInt(params.id as string);
  const { data: post, isLoading, error } = usePost(postId);
  const [logoError, setLogoError] = useState(false);

  if (isLoading) {
    return (
      <div className="min-h-screen bg-slate-50">
        <div className="max-w-4xl mx-auto p-4 lg:p-6">
          <div className="animate-pulse">
            {/* Header skeleton */}
            <div className="mb-6">
              <div className="h-6 bg-slate-200 rounded w-24 mb-4"></div>
              <div className="h-8 bg-slate-200 rounded w-3/4 mb-2"></div>
              <div className="h-4 bg-slate-200 rounded w-1/2"></div>
            </div>

            {/* Content skeleton */}
            <div className="bg-white rounded-lg border border-slate-200 p-6 space-y-4">
              <div className="h-4 bg-slate-200 rounded w-full"></div>
              <div className="h-4 bg-slate-200 rounded w-5/6"></div>
              <div className="h-4 bg-slate-200 rounded w-4/6"></div>
            </div>
          </div>
        </div>
      </div>
    );
  }

  if (error || !post) {
    return (
      <div className="min-h-screen bg-slate-50">
        <div className="max-w-4xl mx-auto p-4 lg:p-6">
          <div className="text-center py-16">
            <h1 className="text-2xl font-bold text-slate-900 mb-4">포스트를 찾을 수 없습니다</h1>
            <p className="text-slate-600 mb-6">요청하신 포스트가 존재하지 않거나 삭제되었습니다.</p>
            <button
              onClick={() => router.back()}
              className="inline-flex items-center space-x-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
            >
              <ArrowLeft className="h-4 w-4" />
              <span>돌아가기</span>
            </button>
          </div>
        </div>
      </div>
    );
  }

  const publishedDate = new Date(post.published_at);

  return (
    <div className="min-h-screen bg-slate-50">
      <div className="max-w-4xl mx-auto p-4 lg:p-6">
        {/* Back button */}
        <div className="mb-6">
          <button
            onClick={() => router.back()}
            className="inline-flex items-center space-x-2 text-slate-600 hover:text-slate-900 transition-colors"
          >
            <ArrowLeft className="h-4 w-4" />
            <span>뒤로가기</span>
          </button>
        </div>

        {/* Article */}
        <article className="bg-white rounded-lg border border-slate-200 shadow-sm">
          {/* Header */}
          <div className="p-6 border-b border-slate-200">
            <div className="flex items-start space-x-4 mb-6">
              {/* Blog info */}
              <div className="flex-shrink-0 flex items-center space-x-3">
                <div className="w-10 h-10 flex items-center justify-center overflow-hidden">
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
                    <div className="w-10 h-10 bg-gradient-to-br from-blue-500 to-blue-600 rounded-lg
                            flex items-center justify-center">
                      <Building2 className="h-6 w-6 text-white" />
                    </div>
                  )}
                </div>
                <div>
                  <div className="font-medium text-slate-900">{post.blog.company}</div>
                  <div className="text-sm text-slate-600">{post.blog.name}</div>
                </div>
              </div>
            </div>

            <h1 className="text-2xl lg:text-3xl font-bold text-slate-900 leading-tight mb-4">
              {post.title}
            </h1>

            {/* Meta info */}
            <div className="flex flex-wrap items-center gap-4 text-sm text-slate-600 mb-4">
              {post.author && (
                <div className="flex items-center space-x-1">
                  <User className="h-4 w-4" />
                  <span>{post.author}</span>
                </div>
              )}
              <div className="flex items-center space-x-1">
                <Clock className="h-4 w-4" />
                <span>{format(publishedDate, 'yyyy년 MM월 dd일', { locale: ko })}</span>
              </div>
            </div>

          </div>

          {/* Content */}
          <div className="p-6">
            {/* AI Streaming Summary */}
            <PostSummary postId={postId} />
          </div>

          {/* Footer */}
          <div className="p-6 border-t border-slate-200 bg-slate-50">
            <div className="flex flex-col sm:flex-row gap-3">
              <Link
                href={post.original_url}
                target="_blank"
                rel="noopener noreferrer"
                className="inline-flex items-center justify-center space-x-2 px-4 py-2
                         bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors flex-1"
              >
                <span>원본 글 보기</span>
                <ExternalLink className="h-4 w-4" />
              </Link>

              <Link
                href={post.blog.site_url}
                target="_blank"
                rel="noopener noreferrer"
                className="inline-flex items-center justify-center space-x-2 px-4 py-2
                         bg-slate-600 text-white rounded-lg hover:bg-slate-700 transition-colors flex-1"
              >
                <span>{post.blog.company} 블로그</span>
                <ExternalLink className="h-4 w-4" />
              </Link>
            </div>
          </div>
        </article>
      </div>
    </div>
  );
}