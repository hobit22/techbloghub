'use client';

import { useMemo } from 'react';
import { Building2, Check, Search, FileText, Calendar } from 'lucide-react';
import { formatDistanceToNow } from 'date-fns';
import { ko } from 'date-fns/locale';
import Image from 'next/image';
import type { Blog } from '@/types';

interface BlogFilterProps {
  blogs: Blog[];
  selectedBlogs: number[];
  searchTerm: string;
  onBlogToggle: (blogId: number) => void;
}

interface CheckboxProps {
  checked: boolean;
  onChange: (e: React.MouseEvent) => void;
  disabled?: boolean;
}

const CustomCheckbox = ({ checked, onChange, disabled = false }: CheckboxProps) => (
  <div
    onClick={onChange}
    className={`w-4 h-4 rounded border-2 flex items-center justify-center transition-all duration-150 cursor-pointer ${
      checked
        ? 'bg-blue-600 border-blue-600'
        : 'border-slate-300 hover:border-blue-400 bg-white'
    } ${disabled ? 'opacity-50 cursor-not-allowed' : ''}`}
  >
    {checked && <Check className="h-2.5 w-2.5 text-white stroke-[3]" />}
  </div>
);

interface CheckboxItemProps {
  checked: boolean;
  onChange: (e: React.MouseEvent) => void;
  children: React.ReactNode;
  disabled?: boolean;
}

const CheckboxItem = ({ checked, onChange, children, disabled = false }: CheckboxItemProps) => (
  <div
    onClick={onChange}
    className={`flex items-center p-3 rounded-lg cursor-pointer group transition-all duration-150 ${
      checked ? 'bg-blue-50 hover:bg-blue-100' : 'hover:bg-slate-50'
    } ${disabled ? 'opacity-50 cursor-not-allowed' : ''}`}
  >
    <CustomCheckbox checked={checked} onChange={onChange} disabled={disabled} />
    <div className="ml-3 flex items-center space-x-2 flex-1 min-w-0">{children}</div>
  </div>
);

export function BlogFilter({ blogs, selectedBlogs, searchTerm, onBlogToggle }: BlogFilterProps) {
  const filteredBlogs = useMemo(() => {
    const searchLower = searchTerm.toLowerCase();
    let filtered = blogs;

    if (searchLower) {
      filtered = blogs.filter(
        (blog) =>
          blog.name.toLowerCase().includes(searchLower) ||
          blog.company.toLowerCase().includes(searchLower)
      );
    }

    // Sort by latest post date
    return filtered.sort((a, b) => {
      const dateA = a.latest_post_published_at;
      const dateB = b.latest_post_published_at;

      if (!dateA && !dateB) return 0;
      if (!dateA) return 1;
      if (!dateB) return -1;

      return new Date(dateB).getTime() - new Date(dateA).getTime();
    });
  }, [blogs, searchTerm]);

  if (filteredBlogs.length === 0) {
    return (
      <div className="text-center py-8 text-slate-500 text-sm">
        <Search className="h-8 w-8 mx-auto mb-2 opacity-50" />
        검색 결과가 없습니다
      </div>
    );
  }

  return (
    <div className="space-y-1">
      {filteredBlogs.map((blog) => {
        const postCount = blog.post_count ?? blog.postCount ?? 0;
        const latestDate = blog.latest_post_published_at;

        return (
          <CheckboxItem
            key={blog.id}
            checked={selectedBlogs.includes(blog.id)}
            onChange={(e) => {
              e.stopPropagation();
              onBlogToggle(blog.id);
            }}
          >
            <div className="flex items-center gap-3 min-w-0 flex-1">
              {blog.logo_url ? (
                <Image
                  src={blog.logo_url}
                  alt={`${blog.name} logo`}
                  width={32}
                  height={32}
                  className="rounded-lg object-cover flex-shrink-0"
                />
              ) : (
                <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-blue-100 to-blue-200 flex items-center justify-center flex-shrink-0">
                  <Building2 className="h-4 w-4 text-blue-600" />
                </div>
              )}
              <div className="flex flex-col min-w-0 flex-1">
                <div className="flex items-center gap-2">
                  <span className="font-medium truncate">{blog.name}</span>
                </div>
                <div className="flex items-center gap-3 mt-1">
                  <div className="flex items-center gap-1 text-xs text-slate-600">
                    <FileText className="h-3 w-3" />
                    <span>{postCount.toLocaleString()}개</span>
                  </div>
                  {latestDate && (
                    <div className="flex items-center gap-1 text-xs text-slate-600">
                      <Calendar className="h-3 w-3" />
                      <span>
                        {formatDistanceToNow(new Date(latestDate), {
                          addSuffix: true,
                          locale: ko,
                        })}
                      </span>
                    </div>
                  )}
                </div>
              </div>
            </div>
          </CheckboxItem>
        );
      })}
    </div>
  );
}
