import { adminClient } from '../client';

// Admin Scheduler types
export interface RSSCollectResult {
  status: string;
  summary: {
    blogs_processed: number;
    new_posts: number;
    skipped_duplicates: number;
    errors: number;
  };
  details?: Array<{
    blog_id: number;
    blog_name: string;
    new_posts: number;
    skipped_duplicates: number;
    errors: string[];
  }>;
}

export interface BlogRSSCollectResult {
  status: string;
  blog_id: number;
  blog_name: string;
  summary: {
    new_posts: number;
    skipped_duplicates: number;
    errors: number;
  };
  errors: string[];
}

export interface ContentProcessResult {
  status: string;
  summary: {
    total_processed: number;
    completed: number;
    failed: number;
    errors: Array<{ post_id: number; error: string }>;
  };
}

export interface SchedulerStats {
  status: string;
  post_stats: {
    total: number;
    pending: number;
    completed: number;
    failed: number;
    error_rate: number;
  };
  blog_stats: {
    total: number;
    active: number;
    inactive: number;
  };
}

export const adminApi = {
  // RSS Collection
  collectAllRSS: async (): Promise<RSSCollectResult> =>
    adminClient.post('/api/v1/admin/scheduler/rss-collect').then((res) => res.data),

  collectBlogRSS: async (blogId: number): Promise<BlogRSSCollectResult> =>
    adminClient
      .post(`/api/v1/admin/scheduler/rss-collect/${blogId}`)
      .then((res) => res.data),

  // Content Processing
  processContent: async (batchSize?: number): Promise<ContentProcessResult> =>
    adminClient
      .post('/api/v1/admin/scheduler/content-process', null, {
        params: { batch_size: batchSize },
      })
      .then((res) => res.data),

  processSinglePost: async (postId: number): Promise<ContentProcessResult> =>
    adminClient
      .post(`/api/v1/admin/scheduler/content-process/${postId}`)
      .then((res) => res.data),

  processBlogPosts: async (
    blogId: number,
    batchSize?: number
  ): Promise<ContentProcessResult> =>
    adminClient
      .post(`/api/v1/admin/scheduler/content-process/blog/${blogId}`, null, {
        params: { batch_size: batchSize },
      })
      .then((res) => res.data),

  // Retry Failed
  retryFailed: async (batchSize?: number): Promise<ContentProcessResult> =>
    adminClient
      .post('/api/v1/admin/scheduler/retry-failed', null, {
        params: { batch_size: batchSize },
      })
      .then((res) => res.data),

  // Scheduler Stats
  getSchedulerStats: async (): Promise<SchedulerStats> =>
    adminClient.get('/api/v1/admin/scheduler/stats').then((res) => res.data),
};
