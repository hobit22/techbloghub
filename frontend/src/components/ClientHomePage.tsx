'use client';

import { useState, useEffect } from 'react';
import Header from '@/components/Header';
import PostCard from '@/components/PostCard';
import FilterTopBar from '@/components/FilterTopBar';
import InfiniteScroll from '@/components/InfiniteScroll';
import FilterResetButton from '@/components/FilterResetButton';
import { useInfinitePosts } from '@/hooks/useInfinitePosts';
import { useBlogs, useAvailableFilters } from '@/hooks/useFilters';
import { useUrlState } from '@/hooks/useUrlState';
import { SearchRequest, Post, Blog, PageResponse } from '@/types';
import { X } from 'lucide-react';

interface ClientHomePageProps {
  // 서버에서 받은 초기 데이터
  initialBlogs: Blog[];
}

export default function ClientHomePage({
  initialBlogs,
}: ClientHomePageProps) {
  const {
    state: urlState,
    setBlogIds,
    updateMultiple,
    reset,
  } = useUrlState();

  // 모든 필터 상태를 URL 상태와 동기화
  const [searchQuery, setSearchQuery] = useState(urlState.keyword || '');
  const [selectedBlogs, setSelectedBlogs] = useState<number[]>(urlState.blogIds || []);

  // 서버에서 받은 초기 데이터를 우선 사용하고, 필요시 클라이언트에서 추가 fetch
  const { data: blogs = initialBlogs } = useBlogs(initialBlogs);

  const searchRequest: SearchRequest = {
    keyword: urlState.keyword || undefined,
    blogIds: urlState.blogIds || undefined,
    sortBy: 'publishedAt',
    sortDirection: 'desc',
  };

  const hasFilters = Boolean(urlState.keyword || urlState.blogIds?.length);

  // SSR에서는 정적 데이터만 로드했으므로 항상 클라이언트에서 포스트 로드
  const {
    data: infiniteData,
    isLoading: postsLoading,
    isFetchingNextPage,
    hasNextPage,
    fetchNextPage,
  } = useInfinitePosts(searchRequest, 20);

  const allPosts = infiniteData?.pages.flatMap(page => page.content) || [];

  const handleSearch = (query: string) => {
    setSearchQuery(query);
    // 키워드 입력 시에는 URL 업데이트하지 않음 (엔터/검색 버튼에서만)
  };

  const handleSearchSubmit = (query: string) => {
    // 검색 버튼 클릭/엔터 시에만 URL 업데이트
    updateMultiple({
      keyword: query,
      page: 0
    });
  };

  const handleReset = () => {
    setSearchQuery('');
    setSelectedBlogs([]);

    reset();
  };

  // URL 상태와 로컬 상태 동기화
  useEffect(() => {
    setSearchQuery(urlState.keyword || '');
    setSelectedBlogs(urlState.blogIds || []);
  }, [urlState]);

  // 필터 변경 시 URL 업데이트
  const handleBlogChange = (blogIds: number[]) => {
    setSelectedBlogs(blogIds);
    setBlogIds(blogIds);
  };

  // 서버 데이터가 있으므로 초기 로딩 상태 제거
  // blogsLoading은 클라이언트 사이드 추가 데이터 로딩에만 사용

  return (
    <div className="min-h-screen bg-slate-50">
      <Header
        onSearch={handleSearch}
        onSearchSubmit={handleSearchSubmit}
        onReset={handleReset}
        searchValue={searchQuery}
      />

      <FilterTopBar
        blogs={blogs}
        selectedBlogs={selectedBlogs}
        onBlogChange={handleBlogChange}
      />

      <main className="max-w-7xl mx-auto p-4 lg:p-6">
        {/* Header Section */}
        <div className="mb-6 lg:mb-8">
          <div className="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-4">
            <div className="space-y-1">
              <h1 className="text-2xl lg:text-3xl font-bold text-slate-900 tracking-tight">
                {urlState.keyword ? (
                  <>&quot;{urlState.keyword}&quot; 검색 결과</>
                ) : hasFilters ? (
                  '필터링된 포스트'
                ) : (
                  '최신 기술 블로그 포스트'
                )}
              </h1>
              <p className="text-slate-600">
                {!postsLoading && infiniteData && infiniteData.pages[0] ? (
                  <>
                    총 <span className="font-semibold text-slate-700">{infiniteData.pages[0].totalElements.toLocaleString()}</span>개의 포스트
                    {(urlState.blogIds?.length || urlState.keyword) && (
                      <span className="ml-2 text-sm bg-blue-50 text-blue-700 px-2 py-1 rounded-full">
                        필터 적용
                      </span>
                    )}
                  </>
                ) : postsLoading ? (
                  '포스트를 불러오는 중...'
                ) : urlState.keyword ? (
                  `&quot;${urlState.keyword}&quot;에 대한 검색 결과가 없습니다`
                ) : (
                  '포스트를 불러오는 중...'
                )}
              </p>
            </div>
            <div className="flex items-center space-x-3">
              <FilterResetButton onReset={handleReset} hasFilters={hasFilters} />
            </div>
          </div>
        </div>

        {/* Content */}
        {postsLoading && allPosts.length === 0 ? (
          <div className="space-y-4">
            {Array.from({ length: 12 }).map((_, index) => (
              <div key={index} className="bg-white rounded-lg border border-slate-200 p-4 animate-pulse">
                <div className="flex items-start space-x-4">
                  <div className="w-12 h-12 bg-slate-200 rounded-lg"></div>
                  <div className="flex-1 space-y-2">
                    <div className="h-5 bg-slate-200 rounded-md w-3/4"></div>
                    <div className="h-4 bg-slate-200 rounded-md w-1/2"></div>
                    <div className="flex gap-2">
                      <div className="h-6 bg-slate-200 rounded-md w-16"></div>
                      <div className="h-6 bg-slate-200 rounded-md w-20"></div>
                    </div>
                  </div>
                </div>
              </div>
            ))}
          </div>
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
          <div className="text-center py-16">
            <div className="max-w-md mx-auto space-y-4">
              <div className="w-16 h-16 mx-auto bg-slate-100 rounded-full flex items-center justify-center">
                <X className="h-8 w-8 text-slate-400" />
              </div>
              <div className="space-y-2">
                <h3 className="text-lg font-semibold text-slate-900">검색 결과가 없습니다</h3>
                <p className="text-slate-600 text-sm">
                  다른 키워드나 필터로 다시 검색해보세요.
                </p>
              </div>
              {hasFilters && (
                <button
                  onClick={handleReset}
                  className="inline-flex items-center space-x-2 px-4 py-2 bg-blue-600 text-white 
                           rounded-lg hover:bg-blue-700 transition-colors text-sm font-medium"
                >
                  <span>필터 초기화</span>
                </button>
              )}
            </div>
          </div>
        )}
      </main>
    </div>
  );
}