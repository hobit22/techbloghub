export interface Blog {
  id: number;
  name: string;
  company: string;
  rssUrl: string;
  siteUrl: string;
  description?: string;
  logoUrl?: string;
  status: string;
  createdAt: string;
  updatedAt: string;
  lastCrawledAt?: string;
  postCount?: number;
}

export interface Post {
  id: number;
  title: string;
  content?: string;
  originalUrl: string;
  author?: string;
  publishedAt: string;
  createdAt: string;
  blog: BlogInfo;
  tags: string[];
  categories: string[];
}

export interface BlogInfo {
  id: number;
  name: string;
  company: string;
  siteUrl: string;
  logoUrl?: string;
}

export interface Tag {
  id: number;
  name: string;
  description?: string;
  createdAt: string;
  updatedAt: string;
}

export interface Category {
  id: number;
  name: string;
  description?: string;
  color?: string;
  createdAt: string;
  updatedAt: string;
}

export interface SearchRequest {
  query?: string;
  companies?: string[];
  tags?: string[];
  categories?: string[];
  sortBy?: string;
  sortDirection?: string;
  page?: number;
  size?: number;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}