'use client';

import { useState, useEffect } from 'react';
import Header from '@/components/Header';
import PostCard from '@/components/PostCard';
import FilterSidebar from '@/components/FilterSidebar';
import LoadingSpinner from '@/components/LoadingSpinner';
import InfiniteScroll from '@/components/InfiniteScroll';
import FilterResetButton from '@/components/FilterResetButton';
import { useInfinitePosts } from '@/hooks/useInfinitePosts';
import { useBlogs, useAvailableFilters } from '@/hooks/useFilters';
import { useUrlState } from '@/hooks/useUrlState';
import { SearchRequest } from '@/types';

export default function Home() {
  const {
    state: urlState,
    setKeyword,
    setBlogIds,
    setTags,
    setCategories,
    reset,
  } = useUrlState();

  const [searchQuery, setSearchQuery] = useState(urlState.keyword || '');
  const [selectedBlogs, setSelectedBlogs] = useState<number[]>(urlState.blogIds || []);
  const [selectedTags, setSelectedTags] = useState<string[]>(urlState.tags || []);
  const [selectedCategories, setSelectedCategories] = useState<string[]>(urlState.categories || []);

  const { data: blogs = [], isLoading: blogsLoading } = useBlogs();
  const { tags, categories } = useAvailableFilters();

  const searchRequest: SearchRequest = {
    keyword: searchQuery || undefined,
    blogIds: selectedBlogs.length > 0 ? selectedBlogs : undefined,
    tags: selectedTags.length > 0 ? selectedTags : undefined,
    categories: selectedCategories.length > 0 ? selectedCategories : undefined,
    sortBy: 'publishedAt',
    sortDirection: 'desc',
  };

  const hasFilters = searchQuery || selectedBlogs.length > 0 || selectedTags.length > 0 || selectedCategories.length > 0;

  const {
    data: infiniteData,
    isLoading: postsLoading,
    isFetchingNextPage,
    hasNextPage,
    fetchNextPage,
  } = useInfinitePosts(searchRequest, 20);

  const allPosts = infiniteData?.pages.flatMap(page => page.content) || [];
  const totalElements = infiniteData?.pages[0]?.totalElements || 0;

  const handleSearch = (query: string) => {
    setSearchQuery(query);
    setKeyword(query);
  };

  const handleReset = () => {
    setSearchQuery('');
    setSelectedBlogs([]);
    setSelectedTags([]);
    setSelectedCategories([]);
    reset();
  };

  // URL 상태와 로컬 상태 동기화
  useEffect(() => {
    setSearchQuery(urlState.keyword || '');
    setSelectedBlogs(urlState.blogIds || []);
    setSelectedTags(urlState.tags || []);
    setSelectedCategories(urlState.categories || []);
  }, [urlState]);

  // 필터 변경 시 URL 업데이트
  const handleBlogChange = (blogIds: number[]) => {
    setSelectedBlogs(blogIds);
    setBlogIds(blogIds);
  };

  const handleTagChange = (tags: string[]) => {
    setSelectedTags(tags);
    setTags(tags);
  };

  const handleCategoryChange = (categories: string[]) => {
    setSelectedCategories(categories);
    setCategories(categories);
  };

  if (blogsLoading) {
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
          selectedBlogs={selectedBlogs}
          selectedTags={selectedTags}
          selectedCategories={selectedCategories}
          onBlogChange={handleBlogChange}
          onTagChange={handleTagChange}
          onCategoryChange={handleCategoryChange}
        />

        <main className="flex-1 p-6">
          <div className="max-w-6xl mx-auto">
            <div className="mb-6 flex items-center justify-between">
              <div>
                <h2 className="text-2xl font-bold text-gray-900">
                  {hasFilters ? '검색 결과' : '최신 기술 블로그 포스트'}
                </h2>
                <p className="text-gray-600 mt-1">
                  {totalElements > 0 ? `총 ${totalElements}개의 포스트` : '포스트를 불러오는 중...'}
                </p>
              </div>
              <FilterResetButton onReset={handleReset} hasFilters={hasFilters} />
            </div>

            {postsLoading && allPosts.length === 0 ? (
              <LoadingSpinner />
            ) : allPosts.length > 0 ? (
              <InfiniteScroll
                onLoadMore={fetchNextPage}
                hasNextPage={hasNextPage as boolean}
                isFetchingNextPage={isFetchingNextPage as boolean}
              >
                <div className="space-y-4">
                  {allPosts.map((post) => (
                    <PostCard key={post.id} post={post} />
                  ))}
                </div>
              </InfiniteScroll>
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
