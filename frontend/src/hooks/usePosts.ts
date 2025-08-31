'use client';

import { useQuery } from '@tanstack/react-query';
import { searchApi } from '@/lib/api';
import { SearchRequest } from '@/types';

export function useSearchPosts(searchRequest: SearchRequest, params: {
  page?: number;
  size?: number;
} = {}, enabled = true) {
  return useQuery({
    queryKey: ['posts', 'search', searchRequest, params],
    queryFn: () => searchApi.searchPosts(searchRequest, params),
    enabled,
  });
}

export function usePosts(params: {
  page?: number;
  size?: number;
} = {}) {
  const searchRequest: SearchRequest = {
    sortBy: 'publishedAt',
    sortDirection: 'desc',
  };
  
  return useSearchPosts(searchRequest, params, true);
}

export function useRecentPosts(params: {
  page?: number;
  size?: number;
} = {}) {
  const searchRequest: SearchRequest = {
    sortBy: 'publishedAt',
    sortDirection: 'desc',
  };
  
  return useSearchPosts(searchRequest, params, true);
}