export interface Blog {
  id: number;
  name: string;
  company: string;
  rss_url: string;
  site_url: string;
  description?: string;
  logo_url?: string;
  status: string;
  blog_type?: string;
  created_at: string;
  updated_at: string;
  last_crawled_at?: string;
  failure_count?: number;
  postCount?: number;
}

export interface Post {
  id: number;
  title: string;
  content?: string;
  original_url: string;
  normalized_url?: string;
  author?: string;
  published_at: string;
  created_at: string;
  updated_at: string;
  blog_id: number;
  blog: BlogInfo;
  keywords?: string[];
  tags?: string[] | null;
  categories?: string[] | null;
  totalContent?: string;
  summaryContent?: string;
}

export interface BlogInfo {
  id: number;
  name: string;
  company: string;
  site_url: string;
  logo_url?: string;
}

export interface Tag {
  id: number;
  name: string;
  description?: string;
  created_at: string;
  updated_at: string;
}

export interface Category {
  id: number;
  name: string;
  description?: string;
  color?: string;
  created_at: string;
  updated_at: string;
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
  created_at: string;
  updated_at: string;
}

export interface CategoryResponse {
  id: number;
  name: string;
  description?: string;
  color?: string;
  created_at: string;
  updated_at: string;
}

export interface BlogListResponse {
  total: number;
  blogs: Blog[];
}

export interface PostListResponse {
  total: number;
  posts: Post[];
}

export interface PostSearchResponse extends Post {
  rank: number;
}

export interface SearchResultResponse {
  total: number;
  results: PostSearchResponse[];
}
