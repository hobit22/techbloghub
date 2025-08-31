'use client';

import { useQuery } from '@tanstack/react-query';
import { blogApi } from '@/lib/api';
import { useSearchPosts } from './usePosts';
import { useMemo } from 'react';

export function useBlogs() {
  return useQuery({
    queryKey: ['blogs'],
    queryFn: blogApi.getActive,
  });
}

export function useAvailableFilters() {
  const { data: postsData } = useSearchPosts({}, { size: 1000 });
  
  const filters = useMemo(() => {
    if (!postsData?.content) {
      return { tags: [], categories: [] };
    }
    
    const tagsSet = new Set<string>();
    const categoriesSet = new Set<string>();
    
    postsData.content.forEach(post => {
      post.tags?.forEach(tag => tagsSet.add(tag));
      post.categories?.forEach(category => categoriesSet.add(category));
    });
    
    return {
      tags: Array.from(tagsSet).sort(),
      categories: Array.from(categoriesSet).sort(),
    };
  }, [postsData]);
  
  return filters;
}