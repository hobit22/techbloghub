'use client';

import { Post } from '@/types';
import { ExternalLink, User, Building2, Clock, ArrowUpRight } from 'lucide-react';
import { format } from 'date-fns';
import { ko } from 'date-fns/locale';
import { useUrlState } from '@/hooks/useUrlState';
import { useState } from 'react';
import Image from 'next/image';
import { useRouter } from 'next/navigation';

interface PostCardProps {
  post: Post;
}

export default function PostCard({ post }: PostCardProps) {
  const { setBlogIds } = useUrlState();
  const [logoError, setLogoError] = useState(false);
  const router = useRouter();

  const handleClick = () => {
    router.push(`/posts/${post.id}`);
  };

  const handleCompanyClick = (e: React.MouseEvent) => {
    e.stopPropagation(); // 이벤트 전파 방지
    setBlogIds([post.blog?.id ?? 0]); // 해당 회사의 블로그로 필터링
  };

  const publishedDate = new Date(post.published_at);
  const isRecent = Date.now() - publishedDate.getTime() < 24 * 60 * 60 * 1000; // 24 hours;

  return (
    <article 
      onClick={handleClick}
      className="group relative bg-white rounded-lg border border-slate-200 p-4 
                 hover:shadow-md hover:shadow-slate-200/50 hover:border-slate-300 
                 transition-all duration-200 cursor-pointer"
    >
      <div className="flex items-start space-x-4">
        {/* Left side - Company Logo and Info */}
        <div className="flex-shrink-0 flex flex-col items-center">
          <div 
            onClick={handleCompanyClick}
            className="w-6 h-6 
                      flex items-center justify-center mb-2 cursor-pointer 
                      overflow-hidden"
          >
            {post.blog?.logo_url && !logoError ? (
              <Image 
                src={post.blog?.logo_url}
                alt={`${post.blog?.company} logo`}
                width={24}
                height={24}
                className="w-full h-full object-contain"
                onError={() => setLogoError(true)}
              />
            ) : (
              <div className="w-6 h-6 bg-gradient-to-br from-blue-500 to-blue-600 rounded-lg 
                      flex items-center justify-center cursor-pointer hover:from-blue-600 hover:to-blue-700 
                      transition-all duration-200 overflow-hidden">
              <Building2 className="h-4 w-4 text-white" />
              </div>
            )}
          </div>
          <div className="text-center">
            <div 
              onClick={handleCompanyClick}
              className="text-xs font-medium text-slate-700 truncate w-16 cursor-pointer hover:text-blue-600 
                        transition-colors duration-200"
            >
              {post.blog?.company}
            </div>
          </div>
        </div>

        {/* Main Content */}
        <div className="flex-1 min-w-0">
          <div className="flex items-start justify-between mb-2">
            <h3 className="text-lg font-semibold text-slate-900 leading-tight line-clamp-1 flex-1
                         group-hover:text-blue-600 transition-colors duration-200 pr-4">
              {post.title}
            </h3>
            
            {/* Status and Arrow */}
            <div className="flex items-center space-x-2 flex-shrink-0">
              {isRecent && (
                <div className="flex items-center space-x-1 px-2 py-1 bg-green-50 border border-green-200 rounded-full">
                  <div className="w-1.5 h-1.5 bg-green-400 rounded-full animate-pulse"></div>
                  <span className="text-xs font-medium text-green-700">NEW</span>
                </div>
              )}
              <div className="opacity-0 group-hover:opacity-100 transition-opacity duration-200">
                <ArrowUpRight className="h-5 w-5 text-slate-400 group-hover:text-blue-600" />
              </div>
            </div>
          </div>

          {/* Content Preview */}
          {post.content && (
            <p className="text-slate-600 text-sm leading-relaxed mb-3 line-clamp-2">
              {post.content}
            </p>
          )}

          {/* Footer */}
          <div className="flex items-center justify-between text-sm">
            <div className="flex items-center space-x-4 text-slate-500">
              {post.author && (
                <div className="flex items-center space-x-1">
                  <User className="h-3.5 w-3.5" />
                  <span>{post.author}</span>
                </div>
              )}
              <div className="flex items-center space-x-1">
                <Clock className="h-3.5 w-3.5" />
                <span>
                  {format(publishedDate, 'yyyy년 MM월 dd일', { locale: ko })}
                </span>
              </div>
            </div>
            
            <div className="flex items-center space-x-2">
              <ExternalLink className="h-3.5 w-3.5 text-slate-400 group-hover:text-blue-500 transition-colors" />
            </div>
          </div>
        </div>
      </div>
    </article>
  );
}