import type { SearchRequest } from '@/types';

/**
 * React Query key factory
 * Provides type-safe query keys for cache management
 */
export const queryKeys = {
  // Blogs
  blogs: {
    all: ['blogs'] as const,
    active: ['blogs', 'active'] as const,
    lists: () => [...queryKeys.blogs.all, 'list'] as const,
    list: (filters: { skip?: number; limit?: number }) =>
      [...queryKeys.blogs.lists(), filters] as const,
    details: () => [...queryKeys.blogs.all, 'detail'] as const,
    detail: (id: number) => [...queryKeys.blogs.details(), id] as const,
  },

  // Posts
  posts: {
    all: ['posts'] as const,
    lists: () => [...queryKeys.posts.all, 'list'] as const,
    list: (filters: { skip?: number; limit?: number; blog_id?: number }) =>
      [...queryKeys.posts.lists(), filters] as const,
    infinites: () => [...queryKeys.posts.all, 'infinite'] as const,
    infinite: (filters: SearchRequest) =>
      [...queryKeys.posts.infinites(), filters] as const,
    searches: () => [...queryKeys.posts.all, 'search'] as const,
    search: (keyword: string, filters: { limit?: number; offset?: number }) =>
      [...queryKeys.posts.searches(), keyword, filters] as const,
    details: () => [...queryKeys.posts.all, 'detail'] as const,
    detail: (id: number) => [...queryKeys.posts.details(), id] as const,
  },

  // Admin
  admin: {
    all: ['admin'] as const,
    blogs: ['admin', 'blogs'] as const,
    scheduler: {
      stats: ['admin', 'scheduler', 'stats'] as const,
    },
  },
} as const;
