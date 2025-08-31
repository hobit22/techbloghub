"use client";

import { useInfiniteQuery } from "@tanstack/react-query";
import { searchApi } from "@/lib/api";
import { SearchRequest } from "@/types";

export function useInfinitePosts(searchRequest: SearchRequest, pageSize = 20) {
  return useInfiniteQuery({
    queryKey: ["posts", "infinite", searchRequest],
    queryFn: ({ pageParam = 0 }) =>
      searchApi.searchPosts(searchRequest, { page: pageParam, size: pageSize }),
    getNextPageParam: (lastPage) => {
      if (lastPage.last) return undefined;
      return lastPage.number + 1;
    },
    initialPageParam: 0,
  });
}
