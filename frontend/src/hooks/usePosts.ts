'use client';

import { useQuery } from '@tanstack/react-query';
import { postApi } from '@/lib/api';
import { SearchRequest } from '@/types';

export function usePosts(params: {
  page?: number;
  size?: number;
  sortBy?: string;
  sortDirection?: string;
} = {}) {
  return useQuery({
    queryKey: ['posts', params],
    queryFn: () => postApi.getAll(params),
  });
}

export function usePost(id: number) {
  return useQuery({
    queryKey: ['posts', id],
    queryFn: () => postApi.getById(id),
    enabled: !!id,
  });
}

export function usePostsByBlog(blogId: number, params: {
  page?: number;
  size?: number;
} = {}) {
  return useQuery({
    queryKey: ['posts', 'blog', blogId, params],
    queryFn: () => postApi.getByBlog(blogId, params),
    enabled: !!blogId,
  });
}

export function useSearchPosts(searchRequest: SearchRequest, enabled = true) {
  return useQuery({
    queryKey: ['posts', 'search', searchRequest],
    queryFn: () => postApi.search(searchRequest),
    enabled,
  });
}

export function useRecentPosts(params: {
  page?: number;
  size?: number;
} = {}) {
  return useQuery({
    queryKey: ['posts', 'recent', params],
    queryFn: () => postApi.getRecent(params),
  });
}