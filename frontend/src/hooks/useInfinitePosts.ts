"use client";

import { useInfiniteQuery } from "@tanstack/react-query";
import { useMemo } from "react";
import { postsApi } from "@/lib/api/endpoints/posts";
import { SearchRequest } from "@/types";

export function useInfinitePosts(
  searchRequest: SearchRequest,
  pageSize = 20
) {
  // 필터가 있는지 확인
  const hasFilters = Boolean(
    searchRequest.keyword ||
      searchRequest.blogIds?.length
  );

  // 키워드 검색인지 일반 목록인지 구분
  const isKeywordSearch = Boolean(searchRequest.keyword);

  // Query key를 더 명확하게 직렬화 (undefined 값 제거)
  const queryKey = useMemo(
    () => [
      "posts",
      "infinite",
      {
        keyword: searchRequest.keyword || null,
        blogIds: searchRequest.blogIds || null,
      },
    ],
    [searchRequest]
  );

  return useInfiniteQuery({
    queryKey,
    queryFn: async ({ pageParam = 0 }) => {
      if (isKeywordSearch) {
        // 키워드 검색 API 사용
        const result = await postsApi.search(searchRequest.keyword!, {
          limit: pageSize,
          offset: pageParam * pageSize,
        });

        // FastAPI 응답을 Spring Boot 형식으로 변환
        return {
          content: result.results,
          totalElements: result.total,
          totalPages: Math.ceil(result.total / pageSize),
          size: pageSize,
          number: pageParam,
          first: pageParam === 0,
          last: (pageParam + 1) * pageSize >= result.total,
        };
      } else {
        // 일반 포스트 목록 API 사용
        const result = await postsApi.getAll({
          skip: pageParam * pageSize,
          limit: pageSize,
          blog_id: searchRequest.blogIds?.[0], // 첫 번째 블로그 ID만 사용
        });

        // FastAPI 응답을 Spring Boot 형식으로 변환
        return {
          content: result.posts,
          totalElements: result.total,
          totalPages: Math.ceil(result.total / pageSize),
          size: pageSize,
          number: pageParam,
          first: pageParam === 0,
          last: (pageParam + 1) * pageSize >= result.total,
        };
      }
    },
    getNextPageParam: (lastPage) => {
      if (lastPage.last) return undefined;
      return lastPage.number + 1;
    },
    initialPageParam: 0,
    // 필터가 있으면 항상 fresh 데이터를 요청, 없으면 30분 캐시
    staleTime: hasFilters ? 0 : 30 * 60 * 1000,
  });
}
