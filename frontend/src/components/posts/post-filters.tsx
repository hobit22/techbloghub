'use client';

import { useState, useCallback, useRef, useEffect } from 'react';
import { Filter, Building2 } from 'lucide-react';
import { FilterDropdown } from './filter-dropdown';
import { BlogFilter } from './blog-filter';
import type { Blog } from '@/types';

interface PostFiltersProps {
  blogs: Blog[];
  selectedBlogs: number[];
  onBlogChange: (blogIds: number[]) => void;
}

export function PostFilters({ blogs, selectedBlogs, onBlogChange }: PostFiltersProps) {
  const [openDropdown, setOpenDropdown] = useState<string | null>(null);
  const [searchTerms, setSearchTerms] = useState<Record<string, string>>({});
  const dropdownRefs = useRef<Record<string, HTMLDivElement | null>>({});

  const handleBlogToggle = useCallback(
    (blogId: number) => {
      if (selectedBlogs.includes(blogId)) {
        onBlogChange(selectedBlogs.filter((id) => id !== blogId));
      } else {
        onBlogChange([...selectedBlogs, blogId]);
      }
      // Clear search after selection
      setSearchTerms((prev) => ({ ...prev, blogs: '' }));
    },
    [selectedBlogs, onBlogChange]
  );

  const toggleDropdown = useCallback(
    (dropdown: string) => {
      setOpenDropdown(openDropdown === dropdown ? null : dropdown);
    },
    [openDropdown]
  );

  const handleScrollEvent = useCallback((e: React.WheelEvent) => {
    e.stopPropagation();
  }, []);

  // Keyboard and outside click handling
  useEffect(() => {
    const handleEscKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        setOpenDropdown(null);
      }
    };

    const handleClickOutside = (e: MouseEvent) => {
      const target = e.target as Element;

      // Check if click is inside dropdown
      const isDropdownClick = Object.values(dropdownRefs.current).some((ref) =>
        ref?.contains(target)
      );

      // Check if click is on dropdown button
      const isButtonClick = target.closest('button[data-dropdown]');

      if (!isDropdownClick && !isButtonClick && openDropdown) {
        setOpenDropdown(null);
      }
    };

    if (openDropdown) {
      document.addEventListener('keydown', handleEscKey);
      document.addEventListener('mousedown', handleClickOutside);

      return () => {
        document.removeEventListener('keydown', handleEscKey);
        document.removeEventListener('mousedown', handleClickOutside);
      };
    }
  }, [openDropdown]);

  return (
    <div className="bg-white/95 backdrop-blur-sm border-b border-slate-200 sticky top-16 lg:top-18 z-30">
      <div className="max-w-7xl mx-auto px-4 lg:px-6 py-4">
        <div className="flex flex-wrap items-center gap-3">
          <div className="flex items-center space-x-3 mr-2">
            <div className="flex items-center space-x-2">
              <div className="p-2 bg-gradient-to-br from-blue-100 to-blue-200 rounded-lg">
                <Filter className="h-4 w-4 text-blue-600" />
              </div>
              <span className="text-sm font-semibold text-slate-800">필터</span>
            </div>
          </div>

          <div className="flex flex-wrap items-center gap-2">
            <FilterDropdown
              title="블로그"
              icon={Building2}
              count={selectedBlogs.length}
              dropdownKey="blogs"
              hasSearch={true}
              isOpen={openDropdown === 'blogs'}
              searchValue={searchTerms['blogs'] || ''}
              onToggle={() => toggleDropdown('blogs')}
              onSearchChange={(value) =>
                setSearchTerms((prev) => ({ ...prev, blogs: value }))
              }
              onWheel={handleScrollEvent}
              dropdownRef={(el) => {
                dropdownRefs.current['blogs'] = el;
              }}
            >
              <BlogFilter
                blogs={blogs}
                selectedBlogs={selectedBlogs}
                searchTerm={searchTerms['blogs'] || ''}
                onBlogToggle={handleBlogToggle}
              />
            </FilterDropdown>
          </div>
        </div>
      </div>
    </div>
  );
}
