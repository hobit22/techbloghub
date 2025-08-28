'use client';

import { useQuery } from '@tanstack/react-query';
import { blogApi, tagApi, categoryApi } from '@/lib/api';

export function useBlogs() {
  return useQuery({
    queryKey: ['blogs'],
    queryFn: blogApi.getAll,
  });
}

export function useTags() {
  return useQuery({
    queryKey: ['tags'],
    queryFn: tagApi.getAll,
  });
}

export function useCategories() {
  return useQuery({
    queryKey: ['categories'],
    queryFn: categoryApi.getAll,
  });
}