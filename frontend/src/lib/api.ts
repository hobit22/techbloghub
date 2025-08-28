import axios from 'axios';
import { Blog, Post, Tag, Category, SearchRequest, PageResponse } from '@/types';

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

export const blogApi = {
  getAll: (): Promise<Blog[]> => 
    api.get('/api/blogs').then(res => res.data),
  
  getById: (id: number): Promise<Blog> => 
    api.get(`/api/blogs/${id}`).then(res => res.data),
  
  getByCompany: (company: string): Promise<Blog[]> => 
    api.get(`/api/blogs/company/${company}`).then(res => res.data),
  
  crawl: (id: number): Promise<string> => 
    api.post(`/api/blogs/${id}/crawl`).then(res => res.data),
  
  crawlAll: (): Promise<string> => 
    api.post('/api/blogs/crawl-all').then(res => res.data),
};

export const postApi = {
  getAll: (params: {
    page?: number;
    size?: number;
    sortBy?: string;
    sortDirection?: string;
  } = {}): Promise<PageResponse<Post>> => 
    api.get('/api/posts', { params }).then(res => res.data),
  
  getById: (id: number): Promise<Post> => 
    api.get(`/api/posts/${id}`).then(res => res.data),
  
  getByBlog: (blogId: number, params: {
    page?: number;
    size?: number;
  } = {}): Promise<PageResponse<Post>> => 
    api.get(`/api/posts/blog/${blogId}`, { params }).then(res => res.data),
  
  search: (searchRequest: SearchRequest): Promise<PageResponse<Post>> => 
    api.post('/api/posts/search', searchRequest).then(res => res.data),
  
  getRecent: (params: {
    page?: number;
    size?: number;
  } = {}): Promise<PageResponse<Post>> => 
    api.get('/api/posts/recent', { params }).then(res => res.data),
};

export const tagApi = {
  getAll: (): Promise<Tag[]> => 
    api.get('/api/tags').then(res => res.data),
};

export const categoryApi = {
  getAll: (): Promise<Category[]> => 
    api.get('/api/categories').then(res => res.data),
};