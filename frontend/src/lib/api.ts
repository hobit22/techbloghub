import axios from "axios";
import { Blog, Post, SearchRequest, PageResponse } from "@/types";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    "Content-Type": "application/json",
  },
});

// 배열 파라미터를 쉼표로 구분된 문자열로 변환하는 함수
function convertArrayParams(params: any): any {
  const converted: any = {};

  Object.keys(params).forEach((key) => {
    const value = params[key];

    if (Array.isArray(value) && value.length > 0) {
      // 배열을 쉼표로 구분된 문자열로 변환
      converted[key] = value.join(",");
    } else if (value !== undefined && value !== null) {
      converted[key] = value;
    }
  });

  return converted;
}

export const blogApi = {
  getAll: (
    params: {
      page?: number;
      size?: number;
    } = {}
  ): Promise<Blog[]> =>
    api.get("/api/blogs", { params }).then((res) => res.data),

  getActive: (): Promise<Blog[]> =>
    api.get("/api/blogs/active").then((res) => res.data),
};

export const searchApi = {
  searchPosts: (
    searchRequest: SearchRequest,
    params: {
      page?: number;
      size?: number;
    } = {}
  ): Promise<PageResponse<Post>> => {
    const queryParams = {
      ...searchRequest,
      ...params,
    };

    // 배열 파라미터를 변환
    const convertedParams = convertArrayParams(queryParams);

    return api
      .get("/api/search/posts", { params: convertedParams })
      .then((res) => res.data);
  },
};
