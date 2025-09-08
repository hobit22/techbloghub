'use client';

import { Menu, X, Zap } from 'lucide-react';
import { useState, useEffect } from 'react';
import UnifiedSearchInput from './UnifiedSearchInput';

interface HeaderProps {
  onSearch: (query: string) => void;
  onSearchSubmit: (query: string) => void;
  onTagsChange: (tags: string[]) => void;
  onReset: () => void;
  searchValue: string;
  selectedTags: string[];
}

export default function Header({ onSearch, onSearchSubmit, onTagsChange, onReset, searchValue, selectedTags }: HeaderProps) {
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);
  const [isScrolled, setIsScrolled] = useState(false);

  useEffect(() => {
    const handleScroll = () => {
      setIsScrolled(window.scrollY > 10);
    };
    window.addEventListener('scroll', handleScroll);
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);


  return (
    <header className={`sticky top-0 z-50 transition-all duration-300 ${
      isScrolled 
        ? 'bg-white/95 backdrop-blur-md shadow-lg border-b border-slate-200/50' 
        : 'bg-white shadow-sm border-b border-slate-200'
    }`}>
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between items-center h-16 lg:h-18">
          {/* Logo Section */}
          <div className="flex items-center space-x-3">
            <div className="flex items-center space-x-2">
              <div className="w-8 h-8 bg-gradient-to-br from-blue-500 to-blue-600 rounded-lg flex items-center justify-center">
                <Zap className="h-5 w-5 text-white" />
              </div>
              <div>
                <h1 className="text-xl font-bold text-slate-900 tracking-tight">
                  TechBlogHub
                </h1>
                <p className="hidden sm:block text-xs text-slate-500 -mt-1">
                  기술 블로그 모음
                </p>
              </div>
            </div>
          </div>

          {/* Desktop Search */}
          <div className="hidden md:flex flex-1 max-w-xl mx-6">
            <UnifiedSearchInput
              value={searchValue}
              onValueChange={onSearch}
              selectedTags={selectedTags}
              onTagsChange={onTagsChange}
              onSearch={onSearchSubmit}
              onClear={onReset}
              placeholder="기술 포스트, 회사, 키워드 검색 또는 #으로 태그 검색..."
            />
          </div>

          {/* Desktop Navigation */}
          <div className="hidden md:flex items-center space-x-6">
            <div className="text-sm text-slate-600 font-medium">
              국내 IT 대기업 기술 블로그
            </div>
            <div className="flex items-center space-x-2 text-xs text-slate-500">
              <div className="w-2 h-2 bg-green-400 rounded-full animate-pulse"></div>
              <span>실시간 업데이트</span>
            </div>
          </div>

          {/* Mobile Menu Button */}
          <button
            onClick={() => setIsMobileMenuOpen(!isMobileMenuOpen)}
            className="md:hidden p-2 rounded-lg hover:bg-slate-100 transition-colors"
          >
            {isMobileMenuOpen ? (
              <X className="h-6 w-6 text-slate-600" />
            ) : (
              <Menu className="h-6 w-6 text-slate-600" />
            )}
          </button>
        </div>

        {/* Mobile Menu */}
        {isMobileMenuOpen && (
          <div className="md:hidden py-4 border-t border-slate-200 bg-white">
            <div className="mb-4">
              <UnifiedSearchInput
                value={searchValue}
                onValueChange={onSearch}
                selectedTags={selectedTags}
                onTagsChange={onTagsChange}
                onSearch={onSearchSubmit}
                onClear={onReset}
                placeholder="기술 포스트, 회사, 키워드 검색 또는 #으로 태그 검색..."
              />
            </div>
            <div className="space-y-3">
              <div className="text-sm text-slate-600 font-medium px-2">
                국내 IT 대기업 기술 블로그
              </div>
              <div className="flex items-center space-x-2 text-xs text-slate-500 px-2">
                <div className="w-2 h-2 bg-green-400 rounded-full animate-pulse"></div>
                <span>실시간 업데이트</span>
              </div>
            </div>
          </div>
        )}
      </div>
    </header>
  );
}