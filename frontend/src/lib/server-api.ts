import { Blog, Post, PageResponse } from "@/types";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

// 서버에서 사용할 fetch 헬퍼 함수
async function serverFetch<T>(url: string, options?: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${url}`, {
    headers: {
      "Content-Type": "application/json",
      ...options?.headers,
    },
    // 서버에서 캐싱 설정
    next: { revalidate: 300 }, // 5분 캐싱
    ...options,
  });

  if (!response.ok) {
    console.error(`Server API Error: ${response.status} - ${url}`);
    // 에러 발생시 빈 데이터 반환으로 graceful degradation
    throw new Error(`API call failed: ${response.status}`);
  }

  return response.json();
}

// URL 파라미터를 API 요청 파라미터로 변환
interface SearchParams {
  keyword?: string;
  blogIds?: string;
  tags?: string;
  categories?: string;
  page?: string;
}

function parseSearchParams(searchParams: SearchParams) {
  return {
    keyword: searchParams.keyword || undefined,
    blogIds: searchParams.blogIds
      ? searchParams.blogIds.split(",").map(Number).filter(Boolean)
      : undefined,
    tags: searchParams.tags
      ? searchParams.tags.split(",").filter(Boolean)
      : undefined,
    categories: searchParams.categories
      ? searchParams.categories.split(",").filter(Boolean)
      : undefined,
    page: parseInt(searchParams.page || "0", 10),
    sortBy: "publishedAt" as const,
    sortDirection: "desc" as const,
  };
}

// 배열 파라미터를 쿼리 스트링으로 변환
function buildQueryString(params: Record<string, unknown>): string {
  const searchParams = new URLSearchParams();

  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null) {
      if (Array.isArray(value) && value.length > 0) {
        searchParams.set(key, value.join(","));
      } else if (!Array.isArray(value)) {
        searchParams.set(key, String(value));
      }
    }
  });

  return searchParams.toString();
}

// 서버 사이드 데이터 페칭 함수들
export const serverApi = {
  // 활성 블로그 목록 가져오기
  async getActiveBlogs(): Promise<Blog[]> {
    try {
      return await serverFetch<Blog[]>("/api/blogs/active");
    } catch (error) {
      console.error("Failed to fetch blogs:", error);
      return [];
    }
  },

  // 포스트 검색
  async searchPosts(
    searchParams: SearchParams,
    size: number = 20
  ): Promise<PageResponse<Post>> {
    try {
      const parsedParams = parseSearchParams(searchParams);
      const queryString = buildQueryString({
        ...parsedParams,
        size,
      });

      return await serverFetch<PageResponse<Post>>(
        `/api/search/posts?${queryString}`
      );
    } catch (error) {
      console.error("Failed to fetch posts:", error);
      // 빈 응답 반환으로 graceful degradation
      return {
        content: [],
        totalElements: 0,
        totalPages: 0,
        size: size,
        number: 0,
        first: true,
        last: true,
      };
    }
  },

  // 사용 가능한 필터 데이터 가져오기 (카테고리, 태그)
  async getAvailableFilters(): Promise<{
    categories: string[];
    tags: string[];
  }> {
    try {
      // 실제 API 엔드포인트가 있다면 사용, 없다면 포스트에서 추출
      // 우선 임시로 빈 배열 반환
      return { categories: [], tags: [] };
    } catch (error) {
      console.error("Failed to fetch filters:", error);
      return { categories: [], tags: [] };
    }
  },
};

// 메인 서버 사이드 데이터 페칭 함수
export interface ServerSideData {
  initialPosts: PageResponse<Post>;
  blogs: Blog[];
  categories: string[];
  tags: string[];
  hasFilters: boolean;
  searchSummary: {
    keyword?: string;
    totalResults: number;
    appliedFiltersCount: number;
  };
}

export async function fetchServerSideData(
  searchParams: SearchParams
): Promise<ServerSideData> {
  try {
    // 병렬로 데이터 페칭
    const [initialPosts, blogs, filters] = await Promise.all([
      serverApi.searchPosts(searchParams),
      serverApi.getActiveBlogs(),
      serverApi.getAvailableFilters(),
    ]);

    const parsedParams = parseSearchParams(searchParams);
    const hasFilters = Boolean(
      parsedParams.keyword ||
        (parsedParams.blogIds && parsedParams.blogIds.length > 0) ||
        (parsedParams.tags && parsedParams.tags.length > 0) ||
        (parsedParams.categories && parsedParams.categories.length > 0)
    );

    const appliedFiltersCount =
      (parsedParams.blogIds?.length || 0) +
      (parsedParams.tags?.length || 0) +
      (parsedParams.categories?.length || 0);

    return {
      initialPosts,
      blogs,
      categories: filters.categories,
      tags: filters.tags,
      hasFilters,
      searchSummary: {
        keyword: parsedParams.keyword,
        totalResults: initialPosts.totalElements,
        appliedFiltersCount,
      },
    };
  } catch (error) {
    console.error("Failed to fetch server-side data:", error);

    // 에러 발생시 기본값 반환
    return {
      initialPosts: {
        content: [],
        totalElements: 0,
        totalPages: 0,
        size: 20,
        number: 0,
        first: true,
        last: true,
      },
      blogs: [],
      categories: [],
      tags: [],
      hasFilters: false,
      searchSummary: {
        totalResults: 0,
        appliedFiltersCount: 0,
      },
    };
  }
}
