import axios from "axios";
import {
  Blog,
  Post,
  BlogListResponse,
  PostListResponse,
  SearchResultResponse,
} from "@/types";

// API Base URL 설정: 프로덕션에서는 ECS 환경변수, 개발에서는 localhost
const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL;

// 환경 확인을 위한 로그 (개발환경에서만)
if (process.env.NODE_ENV === "development") {
  console.log("API_BASE_URL:", API_BASE_URL);
}

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    "Content-Type": "application/json",
  },
});

export const blogApi = {
  getAll: (
    params: {
      skip?: number;
      limit?: number;
    } = {}
  ): Promise<BlogListResponse> =>
    api.get("/api/v1/blogs", { params }).then((res) => res.data),

  getActive: (): Promise<Blog[]> =>
    api.get("/api/v1/blogs/active").then((res) => res.data),

  getById: (id: number): Promise<Blog> =>
    api.get(`/api/v1/blogs/${id}`).then((res) => res.data),
};


export const searchApi = {
  searchPosts: (
    keyword: string,
    params: {
      limit?: number;
      offset?: number;
    } = {}
  ): Promise<SearchResultResponse> => {
    return api
      .get("/api/v1/posts/search", {
        params: {
          q: keyword,
          limit: params.limit || 20,
          offset: params.offset || 0
        }
      })
      .then((res) => res.data);
  },
};

export const postApi = {
  getAll: (
    params: {
      skip?: number;
      limit?: number;
      blog_id?: number;
    } = {}
  ): Promise<PostListResponse> =>
    api.get("/api/v1/posts", { params }).then((res) => res.data),

  getById: (id: number): Promise<Post> =>
    api.get(`/api/v1/posts/${id}`).then((res) => res.data),
};
