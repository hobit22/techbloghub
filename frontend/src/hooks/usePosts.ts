"use client";

import { useQuery } from "@tanstack/react-query";
import { searchApi, postApi } from "@/lib/api";

export function useSearchPosts(
  keyword: string,
  params: {
    limit?: number;
    offset?: number;
  } = {},
  enabled = true
) {
  return useQuery({
    queryKey: ["posts", "search", keyword, params],
    queryFn: () => searchApi.searchPosts(keyword, params),
    enabled,
  });
}

export function usePosts(
  params: {
    skip?: number;
    limit?: number;
    blog_id?: number;
  } = {},
  enabled = true
) {
  return useQuery({
    queryKey: ["posts", "list", params],
    queryFn: () => postApi.getAll(params),
    enabled,
  });
}
