import { apiClient, adminClient } from '../client';
import type { Blog, BlogListResponse, BlogType } from '@/types';

export interface CreateBlogDto {
  name: string;
  company: string;
  rss_url: string;
  site_url: string;
  logo_url?: string;
  description?: string;
  blog_type?: BlogType;
}

export const blogsApi = {
  // Public APIs
  getAll: async (params?: { skip?: number; limit?: number }): Promise<BlogListResponse> =>
    apiClient.get('/api/v1/blogs', { params }).then((res) => res.data),

  getActive: async (): Promise<Blog[]> =>
    apiClient.get('/api/v1/blogs/active').then((res) => res.data),

  getById: async (id: number): Promise<Blog> =>
    apiClient.get(`/api/v1/blogs/${id}`).then((res) => res.data),

  // Admin APIs
  create: async (data: CreateBlogDto): Promise<Blog> =>
    adminClient.post('/api/v1/admin/blogs/', data).then((res) => res.data),

  update: async (id: number, data: Partial<Blog>): Promise<Blog> =>
    adminClient.patch(`/api/v1/admin/blogs/${id}`, data).then((res) => res.data),

  delete: async (id: number): Promise<void> =>
    adminClient.delete(`/api/v1/admin/blogs/${id}`).then(() => undefined),
};
