"use client";

import { useInfiniteQuery } from "@tanstack/react-query";
import { searchApi } from "@/lib/api";
import { SearchRequest, PageResponse, Post } from "@/types";

export function useInfinitePosts(
  searchRequest: SearchRequest, 
  pageSize = 20,
  initialData?: PageResponse<Post>,
  useInitialData = false
) {
  // 필터가 있는지 확인
  const hasFilters = Boolean(
    searchRequest.keyword || 
    searchRequest.blogIds?.length || 
    searchRequest.tags?.length || 
    searchRequest.categories?.length
  );

  // Query key를 더 명확하게 직렬화 (undefined 값 제거)
  const queryKey = [
    "posts", 
    "infinite",
    {
      keyword: searchRequest.keyword || null,
      blogIds: searchRequest.blogIds || null,
      tags: searchRequest.tags || null,
      categories: searchRequest.categories || null,
      sortBy: searchRequest.sortBy,
      sortDirection: searchRequest.sortDirection,
    }
  ];

  return useInfiniteQuery({
    queryKey,
    queryFn: ({ pageParam = 0 }) =>
      searchApi.searchPosts(searchRequest, { page: pageParam, size: pageSize }),
    getNextPageParam: (lastPage) => {
      if (lastPage.last) return undefined;
      return lastPage.number + 1;
    },
    initialPageParam: 0,
    // 초기 데이터는 필터가 없고 useInitialData가 true일 때만 사용
    ...(initialData && useInitialData && !hasFilters && {
      initialData: {
        pages: [initialData],
        pageParams: [0],
      },
    }),
    // 필터가 있으면 항상 fresh 데이터를 요청, 없으면 5분 캐시
    staleTime: hasFilters ? 0 : 5 * 60 * 1000,
  });
}
