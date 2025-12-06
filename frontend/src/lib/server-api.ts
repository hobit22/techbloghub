import {
  Blog,
  Post,
} from "@/types";

// API Base URL 설정: 프로덕션에서는 ECS 환경변수, 개발에서는 localhost
const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL;

// 환경 확인을 위한 로그 (개발환경에서만)
if (process.env.NODE_ENV === "development") {
  console.log("Server API_BASE_URL:", API_BASE_URL);
}

// 서버에서 사용할 fetch 헬퍼 함수
async function serverFetch<T>(url: string, options?: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${url}`, {
    headers: {
      "Content-Type": "application/json",
      ...options?.headers,
    },
    // 서버에서 캐싱 설정
    next: {
      revalidate: 1800,
    },
    ...options,
  });

  if (!response.ok) {
    console.error(`Server API Error: ${response.status} - ${url}`);
    // 에러 발생시 빈 데이터 반환으로 graceful degradation
    throw new Error(`API call failed: ${response.status}`);
  }

  return response.json();
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
      return await serverFetch<Blog[]>("/api/v1/blogs/active");
    } catch (error) {
      console.error("Failed to fetch blogs:", error);
      return [];
    }
  },

  // 포스트 목록 가져오기 (검색 기능은 현재 FastAPI에서 제한적)
  async getPosts(
    params: {
      skip?: number;
      limit?: number;
      blog_id?: number;
    } = {}
  ): Promise<{ posts: Post[]; total: number }> {
    try {
      const queryString = buildQueryString({
        skip: params.skip || 0,
        limit: params.limit || 20,
        blog_id: params.blog_id,
      });

      const response = await serverFetch<{ total: number; posts: Post[] }>(
        `/api/v1/posts?${queryString}`
      );

      return response;
    } catch (error) {
      console.error("Failed to fetch posts:", error);
      return {
        posts: [],
        total: 0,
      };
    }
  },

  // 키워드 검색
  async searchPosts(
    keyword: string,
    params: {
      limit?: number;
      offset?: number;
    } = {}
  ): Promise<{ results: Post[]; total: number }> {
    try {
      const queryString = buildQueryString({
        q: keyword,
        limit: params.limit || 20,
        offset: params.offset || 0,
      });

      const response = await serverFetch<{ total: number; results: Post[] }>(
        `/api/v1/posts/search?${queryString}`
      );

      return response;
    } catch (error) {
      console.error("Failed to search posts:", error);
      return {
        results: [],
        total: 0,
      };
    }
  },

  // 개별 포스트 가져오기 (메타데이터 생성용)
  async getPost(id: number): Promise<Post | null> {
    try {
      return await serverFetch<Post>(`/api/v1/posts/${id}`);
    } catch (error) {
      console.error(`Failed to fetch post ${id}:`, error);
      return null;
    }
  },

};

// 메인 서버 사이드 데이터 페칭 함수
export interface ServerSideData {
  initialPosts: { posts: Post[]; total: number };
  blogs: Blog[];
}

export async function fetchServerSideData(): Promise<ServerSideData> {
  try {
    // 블로그 목록만 페칭
    const blogs = await serverApi.getActiveBlogs();

    // 포스트 데이터는 빈 상태로 설정 (클라이언트에서 로드)
    const initialPosts = {
      posts: [],
      total: 0,
    };

    return {
      initialPosts,
      blogs,
    };
  } catch (error) {
    console.error("Failed to fetch server-side data:", error);

    // 에러 발생시 기본값 반환
    return {
      initialPosts: {
        posts: [],
        total: 0,
      },
      blogs: [],
    };
  }
}
