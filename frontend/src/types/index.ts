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
  tags?: string[] | null;
  categories?: string[] | null;
  totalContent?: string;
  summaryContent?: string;
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
  keyword?: string;
  tags?: string[];
  categories?: string[];
  blogIds?: number[];
  publishedAfter?: string;
  publishedBefore?: string;
  sortBy?: string;
  sortDirection?: string;
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

export interface TagResponse {
  id: number;
  name: string;
  description?: string;
  createdAt: string;
  updatedAt: string;
}

export interface CategoryResponse {
  id: number;
  name: string;
  description?: string;
  color?: string;
  createdAt: string;
  updatedAt: string;
}
