'use client';

import { useQuery } from '@tanstack/react-query';
import { blogApi } from '@/lib/api';
import { useSearchPosts } from './usePosts';
import { useMemo } from 'react';
import { Blog } from '@/types';

export function useBlogs(initialData?: Blog[]) {
  return useQuery({
    queryKey: ['blogs'],
    queryFn: blogApi.getActive,
    // 서버에서 받은 초기 데이터가 있으면 사용
    ...(initialData && {
      initialData,
      staleTime: 5 * 60 * 1000, // 5분
    }),
  });
}

export function useAvailableFilters(initialTags?: string[], initialCategories?: string[]) {
  const { data: postsData } = useSearchPosts({}, { size: 1000 });
  
  const filters = useMemo(() => {
    // 초기 데이터가 있으면 우선 사용
    if (initialTags && initialCategories) {
      return { 
        tags: initialTags, 
        categories: initialCategories 
      };
    }
    
    if (!postsData?.content) {
      return { tags: initialTags || [], categories: initialCategories || [] };
    }
    
    const tagsSet = new Set<string>(initialTags || []);
    const categoriesSet = new Set<string>(initialCategories || []);
    
    postsData.content.forEach(post => {
      post.tags?.forEach(tag => tagsSet.add(tag));
      post.categories?.forEach(category => categoriesSet.add(category));
    });
    
    return {
      tags: Array.from(tagsSet).sort(),
      categories: Array.from(categoriesSet).sort(),
    };
  }, [postsData, initialTags, initialCategories]);
  
  return filters;
}