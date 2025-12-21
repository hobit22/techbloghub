"use client";

import { useQuery } from "@tanstack/react-query";
import { blogsApi } from "@/lib/api/endpoints/blogs";
import { Blog } from "@/types";

export function useBlogs(initialData?: Blog[]) {
  return useQuery({
    queryKey: ["blogs"],
    queryFn: blogsApi.getActive,
    // 서버에서 받은 초기 데이터가 있으면 사용
    ...(initialData && {
      initialData,
      staleTime: 30 * 60 * 1000, // 30분 (정적 데이터)
    }),
  });
}
