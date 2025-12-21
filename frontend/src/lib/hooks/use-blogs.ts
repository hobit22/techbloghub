import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { blogsApi, type CreateBlogDto } from '@/lib/api/endpoints/blogs';
import { queryKeys } from '@/lib/api/queries';
import type { Blog } from '@/types';

// Get all blogs
export function useBlogs(params?: { skip?: number; limit?: number }) {
  return useQuery({
    queryKey: queryKeys.blogs.list(params || {}),
    queryFn: () => blogsApi.getAll(params),
  });
}

// Get active blogs
export function useActiveBlogs() {
  return useQuery({
    queryKey: queryKeys.blogs.active,
    queryFn: () => blogsApi.getActive(),
  });
}

// Get blog by ID
export function useBlog(id: number) {
  return useQuery({
    queryKey: queryKeys.blogs.detail(id),
    queryFn: () => blogsApi.getById(id),
    enabled: !!id,
  });
}

// Create blog
export function useCreateBlog() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: CreateBlogDto) => blogsApi.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.blogs.all });
    },
  });
}

// Update blog
export function useUpdateBlog() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: Partial<Blog> }) =>
      blogsApi.update(id, data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.blogs.all });
      queryClient.invalidateQueries({ queryKey: queryKeys.blogs.detail(variables.id) });
    },
  });
}

// Delete blog
export function useDeleteBlog() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: number) => blogsApi.delete(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.blogs.all });
    },
  });
}
