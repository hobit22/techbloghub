import axios from "axios";
import { Blog, Post, PageResponse } from "@/types";

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

// 관리자 포스트 API
export const adminPostApi = {
  getAll: (params: {
    keyword?: string;
    blogId?: number;
    tag?: string;
    category?: string;
    page?: number;
    size?: number;
  } = {}): Promise<PageResponse<Post>> =>
    adminApi.get("/api/admin/posts", { params }).then((res) => res.data),

  getById: (id: number): Promise<Post> =>
    adminApi.get(`/api/admin/posts/${id}`).then((res) => res.data),

  update: (id: number, data: Partial<Post>): Promise<Post> =>
    adminApi.put(`/api/admin/posts/${id}`, data).then((res) => res.data),

  delete: (id: number): Promise<void> =>
    adminApi.delete(`/api/admin/posts/${id}`).then(() => {}),

  deleteBatch: (ids: number[]): Promise<void> =>
    adminApi.post("/api/admin/posts/batch/delete", { ids }).then(() => {}),
};

// 관리자 블로그 API
export const adminBlogApi = {
  getAll: (params: {
    page?: number;
    size?: number;
  } = {}): Promise<PageResponse<Blog>> =>
    adminApi.get("/api/admin/blogs", { params }).then((res) => res.data),

  getActive: (): Promise<Blog[]> =>
    adminApi.get("/api/admin/blogs/active").then((res) => res.data),

  getById: (id: number): Promise<Blog> =>
    adminApi.get(`/api/admin/blogs/${id}`).then((res) => res.data),

  triggerRecrawl: (id: number): Promise<string> =>
    adminApi.post(`/api/admin/blogs/${id}/recrawl`).then((res) => res.data),

  triggerAllRecrawl: (): Promise<string> =>
    adminApi.post("/api/admin/blogs/recrawl/all").then((res) => res.data),

  getStats: (id: number): Promise<Record<string, unknown>> =>
    adminApi.get(`/api/admin/blogs/${id}/stats`).then((res) => res.data),
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