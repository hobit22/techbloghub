'use client';

import { Post } from '@/types';
import { ExternalLink, Building2, Clock, ArrowUpRight } from 'lucide-react';
import { format } from 'date-fns';
import { ko } from 'date-fns/locale';
import { useUrlState } from '@/hooks/useUrlState';
import { useState } from 'react';
import Image from 'next/image';
import { useRouter } from 'next/navigation';
import Link from 'next/link';

interface PostCardProps {
  post: Post;
}

export default function PostCard({ post }: PostCardProps) {
  const { setBlogIds } = useUrlState();
  const [logoError, setLogoError] = useState(false);
  const router = useRouter();

  const summary = post.content?.replace(/\s+/g, ' ').trim() || '';

  const handleClick = () => {
    router.push(`/posts/${post.id}`);
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault();
      handleClick();
    }
  };

  const handleCompanyClick = (e: React.MouseEvent) => {
    e.stopPropagation();
    setBlogIds([post.blog?.id ?? 0]);
  };

  const handleCompanyKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault();
      e.stopPropagation();
      setBlogIds([post.blog?.id ?? 0]);
    }
  };

  const publishedDate = new Date(post.published_at);
  const isRecent = Date.now() - publishedDate.getTime() < 24 * 60 * 60 * 1000; // 24 hours;

  return (
    <article
      role="button"
      tabIndex={0}
      onClick={handleClick}
      onKeyDown={handleKeyDown}
      aria-label={`${post.blog?.company}: ${post.title}`}
      className="group border-b border-slate-200 py-4 first:pt-0 last:border-b-0 hover:bg-slate-50/70
                 transition-colors duration-150 cursor-pointer focus:outline-none focus:ring-2
                 focus:ring-blue-500 focus:ring-offset-2 rounded-md"
    >
      <div className="flex items-start gap-3">
        <div className="mt-1 flex-shrink-0 flex flex-col items-center">
          <div
            role="button"
            tabIndex={0}
            onClick={handleCompanyClick}
            onKeyDown={handleCompanyKeyDown}
            aria-label={`${post.blog?.company} 블로그 필터`}
            className="h-6 w-6 flex items-center justify-center cursor-pointer overflow-hidden
                      focus:outline-none focus:ring-2 focus:ring-blue-500 rounded"
          >
            {post.blog?.logo_url && !logoError ? (
              <Image
                src={post.blog?.logo_url}
                alt={`${post.blog?.company} logo`}
                width={24}
                height={24}
                loading="lazy"
                className="w-full h-full object-contain"
                onError={() => setLogoError(true)}
              />
            ) : (
              <div className="h-6 w-6 rounded-md bg-slate-900 flex items-center justify-center overflow-hidden">
                <Building2 className="h-3.5 w-3.5 text-white" />
              </div>
            )}
          </div>
        </div>

        <div className="flex-1 min-w-0">
          <div className="flex items-start justify-between gap-4">
            <h3 className="flex-1 text-[17px] font-semibold leading-6 text-slate-950
                           group-hover:text-blue-700 transition-colors duration-150">
              {post.title}
            </h3>
            <div className="flex items-center gap-2 flex-shrink-0">
              {isRecent && (
                <div className="rounded-full bg-emerald-50 px-2 py-0.5 text-[11px] font-semibold text-emerald-700 ring-1 ring-emerald-200">
                  NEW
                </div>
              )}
              <div className="opacity-0 group-hover:opacity-100 transition-opacity duration-150">
                <ArrowUpRight className="h-4 w-4 text-slate-400 group-hover:text-blue-600" />
              </div>
            </div>
          </div>

          <div className="mt-2 flex flex-wrap items-center gap-x-3 gap-y-1 text-sm text-slate-500">
            <div
              role="button"
              tabIndex={0}
              onClick={handleCompanyClick}
              onKeyDown={handleCompanyKeyDown}
              aria-label={`${post.blog?.company} 블로그 필터`}
              className="font-medium text-slate-700 hover:text-blue-700 focus:outline-none focus:text-blue-700"
            >
              {post.blog?.company}
            </div>
            <div className="flex items-center gap-1">
              <Clock className="h-3.5 w-3.5" />
              <span>{format(publishedDate, 'yyyy년 MM월 dd일', { locale: ko })}</span>
            </div>
            {post.author && <span className="truncate">{post.author}</span>}
          </div>

          {summary && (
            <p className="mt-2 line-clamp-1 text-sm leading-6 text-slate-600">
              {summary}
            </p>
          )}

          <div className="mt-3 flex flex-wrap items-center gap-3 text-sm font-medium text-slate-600">
            <Link
              href={`/posts/${post.id}`}
              onClick={(event) => event.stopPropagation()}
              className="inline-flex items-center gap-1 hover:text-blue-700"
            >
              상세 보기
              <ArrowUpRight className="h-3.5 w-3.5" />
            </Link>
            <Link
              href={post.original_url}
              target="_blank"
              rel="noopener noreferrer"
              onClick={(event) => event.stopPropagation()}
              className="inline-flex items-center gap-1 hover:text-blue-700"
            >
              원문 보기
              <ExternalLink className="h-3.5 w-3.5" />
            </Link>
          </div>
        </div>
      </div>
    </article>
  );
}
