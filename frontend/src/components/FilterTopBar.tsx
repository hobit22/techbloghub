'use client';

import { Blog } from '@/types';
import { ChevronDown, Filter, Building2, Tag, Bookmark, Check, Search } from 'lucide-react';
import { useState, useCallback, useRef, useEffect } from 'react';

interface FilterTopBarProps {
  blogs: Blog[];
  tags: string[];
  categories: string[];
  selectedBlogs: number[];
  selectedTags: string[];
  selectedCategories: string[];
  onBlogChange: (blogIds: number[]) => void;
  onTagChange: (tags: string[]) => void;
  onCategoryChange: (categories: string[]) => void;
}

export default function FilterTopBar({
  blogs,
  tags,
  categories,
  selectedBlogs,
  selectedTags,
  selectedCategories,
  onBlogChange,
  onTagChange,
  onCategoryChange,
}: FilterTopBarProps) {
  const [openDropdown, setOpenDropdown] = useState<string | null>(null);
  const [searchTerms, setSearchTerms] = useState<Record<string, string>>({});
  const dropdownRefs = useRef<Record<string, HTMLDivElement | null>>({});

  // 이벤트 핸들러 최적화
  const handleBlogToggle = useCallback((blogId: number, event?: React.MouseEvent) => {
    event?.stopPropagation();
    if (selectedBlogs.includes(blogId)) {
      onBlogChange(selectedBlogs.filter(id => id !== blogId));
    } else {
      onBlogChange([...selectedBlogs, blogId]);
    }
  }, [selectedBlogs, onBlogChange]);

  const handleTagToggle = useCallback((tag: string, event?: React.MouseEvent) => {
    event?.stopPropagation();
    if (selectedTags.includes(tag)) {
      onTagChange(selectedTags.filter(t => t !== tag));
    } else {
      onTagChange([...selectedTags, tag]);
    }
  }, [selectedTags, onTagChange]);

  const handleCategoryToggle = useCallback((category: string, event?: React.MouseEvent) => {
    event?.stopPropagation();
    if (selectedCategories.includes(category)) {
      onCategoryChange(selectedCategories.filter(c => c !== category));
    } else {
      onCategoryChange([...selectedCategories, category]);
    }
  }, [selectedCategories, onCategoryChange]);

  const toggleDropdown = useCallback((dropdown: string) => {
    setOpenDropdown(openDropdown === dropdown ? null : dropdown);
  }, [openDropdown]);

  // 스크롤 이벤트 방지
  const handleScrollEvent = useCallback((e: React.WheelEvent) => {
    e.stopPropagation();
  }, []);

  // 키보드 접근성 및 외부 클릭 감지
  useEffect(() => {
    const handleEscKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        setOpenDropdown(null);
      }
    };

    const handleClickOutside = (e: MouseEvent) => {
      const target = e.target as Element;
      
      // 드롭다운 내부 클릭인지 확인
      const isDropdownClick = Object.values(dropdownRefs.current).some(ref => 
        ref?.contains(target)
      );
      
      // 드롭다운 버튼 클릭인지 확인
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

  const topTags = tags?.slice(0, 30) || [];
  const activeFiltersCount = selectedBlogs.length + selectedTags.length + selectedCategories.length;

  // 검색 필터링
  const getFilteredBlogs = (blogs: Blog[]) => {
    const searchTerm = searchTerms['blogs']?.toLowerCase() || '';
    if (!searchTerm) return blogs;
    
    return blogs.filter(blog => 
      blog.name.toLowerCase().includes(searchTerm) || 
      blog.company.toLowerCase().includes(searchTerm)
    );
  };

  const getFilteredCategories = (categories: string[]) => {
    const searchTerm = searchTerms['categories']?.toLowerCase() || '';
    if (!searchTerm) return categories;
    
    return categories.filter(category => 
      category.toLowerCase().includes(searchTerm)
    );
  };

  const getFilteredTags = (tags: string[]) => {
    const searchTerm = searchTerms['tags']?.toLowerCase() || '';
    if (!searchTerm) return tags;
    
    return tags.filter(tag => 
      tag.toLowerCase().includes(searchTerm)
    );
  };

  const FilterDropdown = ({ 
    title, 
    icon: Icon, 
    count, 
    dropdownKey,
    children,
    hasSearch = false
  }: { 
    title: string; 
    icon: React.ComponentType<{ className?: string }>; 
    count: number;
    dropdownKey: string;
    children: React.ReactNode;
    hasSearch?: boolean;
  }) => (
    <div className="relative">
      <button
        data-dropdown={dropdownKey}
        onClick={() => toggleDropdown(dropdownKey)}
        className={`flex items-center space-x-2 px-4 py-2.5 border rounded-xl
                   text-sm font-medium transition-all duration-200 ${
          count > 0 
            ? 'bg-blue-50 border-blue-200 text-blue-700 hover:bg-blue-100' 
            : 'bg-white border-slate-200 text-slate-700 hover:bg-slate-50 hover:border-slate-300'
        }`}
      >
        <Icon className={`h-4 w-4 ${count > 0 ? 'text-blue-600' : 'text-slate-500'}`} />
        <span>{title}</span>
        {count > 0 && (
          <span className="bg-blue-600 text-white text-xs px-2 py-1 rounded-full font-medium min-w-[1.5rem] text-center">
            {count}
          </span>
        )}
        <ChevronDown className={`h-4 w-4 text-slate-400 transition-transform duration-200 ${
          openDropdown === dropdownKey ? 'rotate-180' : ''
        }`} />
      </button>
      
      {openDropdown === dropdownKey && (
        <div 
          ref={(el) => { dropdownRefs.current[dropdownKey] = el; }}
          onWheel={handleScrollEvent}
          className="absolute top-full left-0 mt-2 w-96 bg-white border border-slate-200 
                   rounded-xl shadow-xl z-50 animate-in fade-in slide-in-from-top-2 duration-200"
        >
          {hasSearch && (
            <div className="p-3 border-b border-slate-100">
              <div className="relative">
                <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-slate-400" />
                <input
                  type="text"
                  placeholder={`${title} 검색...`}
                  value={searchTerms[dropdownKey] || ''}
                  onChange={(e) => setSearchTerms(prev => ({ ...prev, [dropdownKey]: e.target.value }))}
                  className="w-full pl-10 pr-4 py-2 text-sm border border-slate-200 rounded-lg
                           focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-400"
                  onClick={(e) => e.stopPropagation()}
                />
              </div>
            </div>
          )}
          <div className="p-2 max-h-72 overflow-y-auto">
            {children}
          </div>
        </div>
      )}
    </div>
  );

  const CustomCheckbox = ({ 
    checked, 
    onChange,
    disabled = false
  }: { 
    checked: boolean; 
    onChange: (e: React.MouseEvent) => void;
    disabled?: boolean;
  }) => (
    <div 
      onClick={onChange}
      className={`w-4 h-4 rounded border-2 flex items-center justify-center transition-all duration-150 cursor-pointer ${
        checked 
          ? 'bg-blue-600 border-blue-600' 
          : 'border-slate-300 hover:border-blue-400 bg-white'
      } ${disabled ? 'opacity-50 cursor-not-allowed' : ''}`}
    >
      {checked && (
        <Check className="h-2.5 w-2.5 text-white stroke-[3]" />
      )}
    </div>
  );

  const CheckboxItem = ({ 
    checked, 
    onChange, 
    children, 
    isCompany = false,
    disabled = false
  }: { 
    checked: boolean; 
    onChange: (e: React.MouseEvent) => void; 
    children: React.ReactNode;
    isCompany?: boolean;
    disabled?: boolean;
  }) => (
    <div 
      onClick={onChange}
      className={`flex items-center p-3 rounded-lg cursor-pointer group transition-all duration-150 ${
        checked 
          ? 'bg-blue-50 hover:bg-blue-100' 
          : 'hover:bg-slate-50'
      } ${disabled ? 'opacity-50 cursor-not-allowed' : ''}`}
    >
      <CustomCheckbox checked={checked} onChange={onChange} disabled={disabled} />
      <div className="ml-3 flex items-center space-x-2 flex-1 min-w-0">
        {isCompany && (
          <div className={`w-2 h-2 rounded-full flex-shrink-0 ${
            checked ? 'bg-blue-500' : 'bg-slate-400'
          }`}></div>
        )}
        <span className={`text-sm truncate transition-colors ${
          checked 
            ? 'text-blue-900 font-medium' 
            : 'text-slate-700 group-hover:text-slate-900'
        }`}>
          {children}
        </span>
      </div>
    </div>
  );

  const filteredBlogs = getFilteredBlogs(blogs);
  const filteredCategories = getFilteredCategories(categories);
  const filteredTags = getFilteredTags(topTags);

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
            {activeFiltersCount > 0 && (
              <div className="flex items-center space-x-2">
                <span className="bg-blue-600 text-white text-xs px-2.5 py-1 rounded-full font-medium">
                  {activeFiltersCount}개 선택
                </span>
                <button
                  onClick={() => {
                    onBlogChange([]);
                    onTagChange([]);
                    onCategoryChange([]);
                  }}
                  className="text-slate-500 hover:text-slate-700 text-xs font-medium hover:underline transition-colors"
                >
                  전체 해제
                </button>
              </div>
            )}
          </div>

          <div className="flex flex-wrap items-center gap-2">
            <FilterDropdown
              title="블로그"
              icon={Building2}
              count={selectedBlogs.length}
              dropdownKey="blogs"
              hasSearch={blogs.length > 10
              }
            >
              <div className="space-y-1">
                {filteredBlogs.length === 0 ? (
                  <div className="text-center py-8 text-slate-500 text-sm">
                    <Search className="h-8 w-8 mx-auto mb-2 opacity-50" />
                    검색 결과가 없습니다
                  </div>
                ) : (
                  filteredBlogs.map((blog: Blog) => (
                    <CheckboxItem
                      key={blog.id}
                      checked={selectedBlogs.includes(blog.id)}
                      onChange={(e) => handleBlogToggle(blog.id, e)}
                      isCompany
                    >
                      <div className="flex flex-col min-w-0">
                        <span className="font-medium truncate">{blog.name}</span>
                        <span className="text-xs text-slate-500 truncate">{blog.company}</span>
                      </div>
                    </CheckboxItem>
                  ))
                )}
              </div>
            </FilterDropdown>

            <FilterDropdown
              title="카테고리"
              icon={Bookmark}
              count={selectedCategories.length}
              dropdownKey="categories"
              hasSearch={categories.length > 10}
            >
              <div className="space-y-1">
                {filteredCategories.length === 0 ? (
                  <div className="text-center py-8 text-slate-500 text-sm">
                    <Search className="h-8 w-8 mx-auto mb-2 opacity-50" />
                    검색 결과가 없습니다
                  </div>
                ) : (
                  filteredCategories.map((category: string) => (
                    <CheckboxItem
                      key={category}
                      checked={selectedCategories.includes(category)}
                      onChange={(e) => handleCategoryToggle(category, e)}
                    >
                      {category}
                    </CheckboxItem>
                  ))
                )}
              </div>
            </FilterDropdown>

            <FilterDropdown
              title="태그"
              icon={Tag}
              count={selectedTags.length}
              dropdownKey="tags"
              hasSearch={topTags.length > 10}
            >
              <div className="space-y-1">
                {filteredTags.length === 0 ? (
                  <div className="text-center py-8 text-slate-500 text-sm">
                    <Search className="h-8 w-8 mx-auto mb-2 opacity-50" />
                    검색 결과가 없습니다
                  </div>
                ) : (
                  filteredTags.map((tag: string) => (
                    <CheckboxItem
                      key={tag}
                      checked={selectedTags.includes(tag)}
                      onChange={(e) => handleTagToggle(tag, e)}
                    >
                      <span className="font-mono text-sm">#{tag}</span>
                    </CheckboxItem>
                  ))
                )}
              </div>
            </FilterDropdown>
          </div>
        </div>
      </div>
    </div>
  );
}