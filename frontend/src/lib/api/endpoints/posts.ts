import { apiClient, adminClient } from '../client';
import type { Post, PostListResponse, SearchResultResponse } from '@/types';

export const postsApi = {
  // Public APIs
  getAll: async (params?: {
    skip?: number;
    limit?: number;
    blog_id?: number;
  }): Promise<PostListResponse> =>
    apiClient.get('/api/v1/posts', { params }).then((res) => res.data),

  getById: async (id: number): Promise<Post> =>
    apiClient.get(`/api/v1/posts/${id}`).then((res) => res.data),

  search: async (
    keyword: string,
    params?: { limit?: number; offset?: number }
  ): Promise<SearchResultResponse> =>
    apiClient
      .get('/api/v1/posts/search', {
        params: {
          q: keyword,
          limit: params?.limit || 20,
          offset: params?.offset || 0,
        },
      })
      .then((res) => res.data),

  // Admin APIs
  update: async (id: number, data: Partial<Post>): Promise<Post> =>
    adminClient.patch(`/api/v1/admin/posts/${id}`, data).then((res) => res.data),

  delete: async (id: number): Promise<void> =>
    adminClient.delete(`/api/v1/admin/posts/${id}`).then(() => undefined),
};
