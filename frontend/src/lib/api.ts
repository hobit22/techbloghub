import axios from "axios";
import {
  Blog,
  Post,
  SearchRequest,
  PageResponse,
  TagResponse,
  CategoryResponse,
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

// 배열 파라미터를 쉼표로 구분된 문자열로 변환하는 함수
function convertArrayParams(
  params: Record<string, unknown>
): Record<string, string | number | boolean> {
  const converted: Record<string, string | number | boolean> = {};

  Object.entries(params).forEach(([key, value]) => {
    if (Array.isArray(value)) {
      // 빈 배열이 아닌 경우에만 쉼표로 구분된 문자열로 변환
      if (value.length > 0) {
        converted[key] = value.join(",");
      }
      // 빈 배열은 쿼리 파라미터에서 제외
    } else if (
      value !== undefined &&
      value !== null &&
      (typeof value === "string" ||
        typeof value === "number" ||
        typeof value === "boolean")
    ) {
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

export const tagApi = {
  getAll: (): Promise<TagResponse[]> =>
    api.get("/api/tags").then((res) => res.data),

  search: (query?: string): Promise<TagResponse[]> =>
    api
      .get("/api/tags/search", { params: { q: query } })
      .then((res) => res.data),
};

export const categoryApi = {
  getAll: (): Promise<CategoryResponse[]> =>
    api.get("/api/categories").then((res) => res.data),
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
