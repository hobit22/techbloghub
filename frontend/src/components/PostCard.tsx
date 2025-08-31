'use client';

import { Post } from '@/types';
import { Calendar, ExternalLink, User, Building2 } from 'lucide-react';
import { format } from 'date-fns';
import { ko } from 'date-fns/locale';

interface PostCardProps {
  post: Post;
}

export default function PostCard({ post }: PostCardProps) {
  const handleClick = () => {
    window.open(post.originalUrl, '_blank');
  };

  return (
    <div 
      onClick={handleClick}
      className="bg-white rounded-lg shadow-sm border border-gray-200 p-4 hover:shadow-md transition-shadow cursor-pointer group"
    >
      <div className="flex items-center justify-between">
        <div className="flex-1 min-w-0">
          <div className="flex items-center space-x-4 mb-2">
            <h3 className="text-lg font-semibold text-gray-900 group-hover:text-blue-600 line-clamp-1 flex-1">
              {post.title}
            </h3>
            <ExternalLink className="h-4 w-4 text-gray-400 group-hover:text-blue-600 flex-shrink-0" />
          </div>

          {post.content && (
            <p className="text-gray-600 text-sm mb-3 line-clamp-2">
              {post.content}
            </p>
          )}

          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-4 text-sm text-gray-500">
              <div className="flex items-center">
                <Building2 className="h-4 w-4 mr-1" />
                <span>{post.blog.company}</span>
              </div>
              {post.author && (
                <div className="flex items-center">
                  <User className="h-4 w-4 mr-1" />
                  <span>{post.author}</span>
                </div>
              )}
              <div className="flex items-center">
                <Calendar className="h-4 w-4 mr-1" />
                <span>
                  {format(new Date(post.publishedAt), 'yyyy년 MM월 dd일', { locale: ko })}
                </span>
              </div>
            </div>

            <div className="flex items-center space-x-2">
              <span className="text-xs px-2 py-1 bg-blue-100 text-blue-800 rounded-full">
                {post.blog.name}
              </span>
            </div>
          </div>

          <div className="flex flex-wrap gap-1 mt-2">
            {post.categories && post.categories.length > 0 && post.categories.slice(0, 2).map((category) => (
              <span
                key={category}
                className="text-xs px-2 py-1 bg-green-100 text-green-800 rounded-full"
              >
                {category}
              </span>
            ))}
            {post.tags && post.tags.length > 0 && post.tags.slice(0, 3).map((tag) => (
              <span
                key={tag}
                className="text-xs px-2 py-1 bg-gray-100 text-gray-600 rounded-full"
              >
                #{tag}
              </span>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}