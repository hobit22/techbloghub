'use client';

import { useState, useEffect } from 'react';
import Header from '@/components/Header';
import PostCard from '@/components/PostCard';
import FilterSidebar from '@/components/FilterSidebar';
import LoadingSpinner from '@/components/LoadingSpinner';
import { usePosts, useSearchPosts } from '@/hooks/usePosts';
import { useBlogs, useTags, useCategories } from '@/hooks/useFilters';
import { SearchRequest } from '@/types';

export default function Home() {
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedCompanies, setSelectedCompanies] = useState<string[]>([]);
  const [selectedTags, setSelectedTags] = useState<string[]>([]);
  const [selectedCategories, setSelectedCategories] = useState<string[]>([]);
  const [page, setPage] = useState(0);
  const [isSearching, setIsSearching] = useState(false);

  const { data: blogs = [], isLoading: blogsLoading } = useBlogs();
  const { data: tags = [], isLoading: tagsLoading } = useTags();
  const { data: categories = [], isLoading: categoriesLoading } = useCategories();

  const searchRequest: SearchRequest = {
    query: searchQuery || undefined,
    companies: selectedCompanies.length > 0 ? selectedCompanies : undefined,
    tags: selectedTags.length > 0 ? selectedTags : undefined,
    categories: selectedCategories.length > 0 ? selectedCategories : undefined,
    page,
    size: 20,
    sortBy: 'publishedAt',
    sortDirection: 'desc',
  };

  const hasFilters = searchQuery || selectedCompanies.length > 0 || selectedTags.length > 0 || selectedCategories.length > 0;

  const { data: postsData, isLoading: postsLoading } = usePosts({ page, size: 20 });
  const { data: searchData, isLoading: searchLoading } = useSearchPosts(searchRequest, hasFilters);

  const currentData = hasFilters ? searchData : postsData;
  const isLoading = hasFilters ? searchLoading : postsLoading;

  const handleSearch = (query: string) => {
    setSearchQuery(query);
    setPage(0);
  };

  const handleLoadMore = () => {
    if (currentData && !currentData.last) {
      setPage(prev => prev + 1);
    }
  };

  useEffect(() => {
    setPage(0);
  }, [selectedCompanies, selectedTags, selectedCategories]);

  if (blogsLoading || tagsLoading || categoriesLoading) {
    return (
      <div className="min-h-screen bg-gray-50">
        <Header onSearch={handleSearch} />
        <LoadingSpinner />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <Header onSearch={handleSearch} />
      
      <div className="flex">
        <FilterSidebar
          blogs={blogs}
          tags={tags}
          categories={categories}
          selectedCompanies={selectedCompanies}
          selectedTags={selectedTags}
          selectedCategories={selectedCategories}
          onCompanyChange={setSelectedCompanies}
          onTagChange={setSelectedTags}
          onCategoryChange={setSelectedCategories}
        />

        <main className="flex-1 p-6">
          <div className="max-w-6xl mx-auto">
            <div className="mb-6">
              <h2 className="text-2xl font-bold text-gray-900">
                {hasFilters ? '검색 결과' : '최신 기술 블로그 포스트'}
              </h2>
              <p className="text-gray-600 mt-1">
                {currentData ? `총 ${currentData.totalElements}개의 포스트` : '포스트를 불러오는 중...'}
              </p>
            </div>

            {isLoading ? (
              <LoadingSpinner />
            ) : currentData?.content && currentData.content.length > 0 ? (
              <>
                <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
                  {currentData.content.map((post) => (
                    <PostCard key={post.id} post={post} />
                  ))}
                </div>

                {!currentData.last && (
                  <div className="mt-8 text-center">
                    <button
                      onClick={handleLoadMore}
                      className="px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
                    >
                      더 보기
                    </button>
                  </div>
                )}
              </>
            ) : (
              <div className="text-center py-12">
                <p className="text-gray-500">검색 결과가 없습니다.</p>
              </div>
            )}
          </div>
        </main>
      </div>
    </div>
  );
}
