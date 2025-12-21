import { useQuery, useMutation } from '@tanstack/react-query';
import { adminApi } from '@/lib/api/endpoints/admin';
import { queryKeys } from '@/lib/api/queries';

// Get scheduler stats
export function useSchedulerStats() {
  return useQuery({
    queryKey: queryKeys.admin.scheduler.stats,
    queryFn: () => adminApi.getSchedulerStats(),
    refetchInterval: 30000, // Refetch every 30 seconds
  });
}

// Process single post
export function useProcessPost() {
  return useMutation({
    mutationFn: (postId: number) => adminApi.processSinglePost(postId),
  });
}

// Process blog posts
export function useProcessBlogPosts() {
  return useMutation({
    mutationFn: ({ blogId, batchSize }: { blogId: number; batchSize?: number }) =>
      adminApi.processBlogPosts(blogId, batchSize),
  });
}

// Collect all RSS
export function useCollectAllRSS() {
  return useMutation({
    mutationFn: () => adminApi.collectAllRSS(),
  });
}

// Collect blog RSS
export function useCollectBlogRSS() {
  return useMutation({
    mutationFn: (blogId: number) => adminApi.collectBlogRSS(blogId),
  });
}

// Process content
export function useProcessContent() {
  return useMutation({
    mutationFn: (batchSize?: number) => adminApi.processContent(batchSize),
  });
}

// Retry failed
export function useRetryFailed() {
  return useMutation({
    mutationFn: (batchSize?: number) => adminApi.retryFailed(batchSize),
  });
}
