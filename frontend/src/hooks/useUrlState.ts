"use client";

import { useState, useEffect, useCallback } from "react";
import { useRouter, useSearchParams } from "next/navigation";

interface UrlState {
  keyword?: string;
  blogIds?: number[];
  tags?: string[];
  categories?: string[];
  page?: number;
}

export function useUrlState() {
  const router = useRouter();
  const searchParams = useSearchParams();

  // URL에서 상태 읽기
  const getStateFromUrl = useCallback((): UrlState => {
    const keyword = searchParams.get("keyword") || undefined;
    const blogIds =
      searchParams.get("blogIds")?.split(",").map(Number).filter(Boolean) || [];
    const tags = searchParams.get("tags")?.split(",").filter(Boolean) || [];
    const categories =
      searchParams.get("categories")?.split(",").filter(Boolean) || [];
    const page = parseInt(searchParams.get("page") || "0", 10);

    return {
      keyword,
      blogIds: blogIds.length > 0 ? blogIds : undefined,
      tags: tags.length > 0 ? tags : undefined,
      categories: categories.length > 0 ? categories : undefined,
      page,
    };
  }, [searchParams]);

  // URL에 상태 쓰기
  const updateUrl = useCallback(
    (newState: Partial<UrlState>) => {
      const currentState = getStateFromUrl();
      const updatedState = { ...currentState, ...newState };

      const params = new URLSearchParams();

      if (updatedState.keyword) {
        params.set("keyword", updatedState.keyword);
      }

      if (updatedState.blogIds && updatedState.blogIds.length > 0) {
        params.set("blogIds", updatedState.blogIds.join(","));
      }

      if (updatedState.tags && updatedState.tags.length > 0) {
        params.set("tags", updatedState.tags.join(","));
      }

      if (updatedState.categories && updatedState.categories.length > 0) {
        params.set("categories", updatedState.categories.join(","));
      }

      if (updatedState.page !== undefined && updatedState.page > 0) {
        params.set("page", updatedState.page.toString());
      }

      const queryString = params.toString();
      const newUrl = queryString ? `?${queryString}` : "/";

      router.push(newUrl, { scroll: false });
    },
    [router, getStateFromUrl]
  );

  // 초기 상태
  const [state, setState] = useState<UrlState>(getStateFromUrl);

  // URL 변경 감지
  useEffect(() => {
    setState(getStateFromUrl());
  }, [getStateFromUrl]);

  return {
    state,
    updateUrl,
    setKeyword: (keyword: string) => updateUrl({ keyword, page: 0 }),
    setBlogIds: (blogIds: number[]) => updateUrl({ blogIds, page: 0 }),
    setTags: (tags: string[]) => updateUrl({ tags, page: 0 }),
    setCategories: (categories: string[]) => updateUrl({ categories, page: 0 }),
    setPage: (page: number) => updateUrl({ page }),
    reset: () => router.push("/", { scroll: false }),
  };
}
