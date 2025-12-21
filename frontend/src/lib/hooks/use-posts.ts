import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { postsApi } from '@/lib/api/endpoints/posts';
import { queryKeys } from '@/lib/api/queries';
import type { Post } from '@/types';

// Get all posts
export function usePosts(params?: {
  skip?: number;
  limit?: number;
  blog_id?: number;
}) {
  return useQuery({
    queryKey: queryKeys.posts.list(params || {}),
    queryFn: () => postsApi.getAll(params),
  });
}

// Get post by ID
export function usePost(id: number) {
  return useQuery({
    queryKey: queryKeys.posts.detail(id),
    queryFn: () => postsApi.getById(id),
    enabled: !!id,
  });
}

// Search posts
export function useSearchPosts(
  keyword: string,
  params?: { limit?: number; offset?: number }
) {
  return useQuery({
    queryKey: queryKeys.posts.search(keyword, params || {}),
    queryFn: () => postsApi.search(keyword, params),
    enabled: !!keyword.trim(),
  });
}

// Update post
export function useUpdatePost() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: Partial<Post> }) =>
      postsApi.update(id, data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.posts.all });
      queryClient.invalidateQueries({ queryKey: queryKeys.posts.detail(variables.id) });
    },
  });
}

// Delete post
export function useDeletePost() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: number) => postsApi.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.posts.all });
    },
  });
}
