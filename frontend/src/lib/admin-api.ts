import axios from "axios";
import { Blog, Post, BlogListResponse, PostListResponse } from "@/types";

// API Base URL 설정
const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL;

// Admin API 클라이언트 생성
const createAdminApi = () => {
  const api = axios.create({
    baseURL: API_BASE_URL,
    headers: {
      "Content-Type": "application/json",
    },
  });

  // 요청 인터셉터: Basic Auth 헤더 자동 추가
  api.interceptors.request.use(
    (config) => {
      if (typeof window !== 'undefined') {
        const auth = localStorage.getItem('admin-auth');
        if (auth) {
          config.headers.Authorization = `Basic ${auth}`;
        }
      }
      return config;
    },
    (error) => Promise.reject(error)
  );

  // 응답 인터셉터: 401 에러 시 로그인 페이지로 리다이렉트
  api.interceptors.response.use(
    (response) => response,
    (error) => {
      if (error.response?.status === 401 && typeof window !== 'undefined') {
        localStorage.removeItem('admin-auth');
        window.location.href = '/admin/login';
      }
      return Promise.reject(error);
    }
  );

  return api;
};

const adminApi = createAdminApi();

// 관리자 포스트 API (FastAPI v1)
export const adminPostApi = {
  getAll: (params: {
    skip?: number;
    limit?: number;
    blog_id?: number;
  } = {}): Promise<PostListResponse> =>
    adminApi.get("/api/v1/admin/posts", { params }).then((res) => res.data),

  getById: (id: number): Promise<Post> =>
    adminApi.get(`/api/v1/admin/posts/${id}`).then((res) => res.data),

  update: (id: number, data: Partial<Post>): Promise<Post> =>
    adminApi.patch(`/api/v1/admin/posts/${id}`, data).then((res) => res.data),

  delete: (id: number): Promise<void> =>
    adminApi.delete(`/api/v1/admin/posts/${id}`).then(() => {}),
};

// 관리자 블로그 API (FastAPI v1)
export const adminBlogApi = {
  getAll: (params: {
    skip?: number;
    limit?: number;
  } = {}): Promise<BlogListResponse> =>
    adminApi.get("/api/v1/admin/blogs", { params }).then((res) => res.data),

  getById: (id: number): Promise<Blog> =>
    adminApi.get(`/api/v1/admin/blogs/${id}`).then((res) => res.data),

  create: (data: {
    name: string;
    company: string;
    rss_url: string;
    site_url: string;
    logo_url?: string;
    description?: string;
    blog_type?: string;
  }): Promise<Blog> =>
    adminApi.post("/api/v1/admin/blogs", data).then((res) => res.data),

  update: (id: number, data: Partial<Blog>): Promise<Blog> =>
    adminApi.patch(`/api/v1/admin/blogs/${id}`, data).then((res) => res.data),

  delete: (id: number): Promise<void> =>
    adminApi.delete(`/api/v1/admin/blogs/${id}`).then(() => {}),
};

// 관리자 스케줄러 API (FastAPI v1)
export const adminSchedulerApi = {
  // RSS 수집
  collectAllRSS: (): Promise<any> =>
    adminApi.post("/api/v1/admin/scheduler/rss-collect").then((res) => res.data),

  collectBlogRSS: (blogId: number): Promise<any> =>
    adminApi.post(`/api/v1/admin/scheduler/rss-collect/${blogId}`).then((res) => res.data),

  // 콘텐츠 처리
  processContent: (batchSize?: number): Promise<any> =>
    adminApi.post("/api/v1/admin/scheduler/content-process", null, {
      params: { batch_size: batchSize }
    }).then((res) => res.data),

  processSinglePost: (postId: number): Promise<any> =>
    adminApi.post(`/api/v1/admin/scheduler/content-process/${postId}`).then((res) => res.data),

  processBlogPosts: (blogId: number, batchSize?: number): Promise<any> =>
    adminApi.post(`/api/v1/admin/scheduler/content-process/blog/${blogId}`, null, {
      params: { batch_size: batchSize }
    }).then((res) => res.data),

  // 실패 재시도
  retryFailed: (batchSize?: number): Promise<any> =>
    adminApi.post("/api/v1/admin/scheduler/retry-failed", null, {
      params: { batch_size: batchSize }
    }).then((res) => res.data),

  // 통계
  getStats: (): Promise<{
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
  }> =>
    adminApi.get("/api/v1/admin/scheduler/stats").then((res) => res.data),
};

// 인증 관련 유틸리티
export const adminAuth = {
  isLoggedIn: (): boolean => {
    if (typeof window === 'undefined') return false;
    return !!localStorage.getItem('admin-auth');
  },

  logout: (): void => {
    if (typeof window !== 'undefined') {
      localStorage.removeItem('admin-auth');
      window.location.href = '/admin/login';
    }
  },

  getAuth: (): string | null => {
    if (typeof window === 'undefined') return null;
    return localStorage.getItem('admin-auth');
  },
};

export default adminApi;