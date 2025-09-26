"use client";

import { useQuery } from "@tanstack/react-query";
import { postApi } from "@/lib/api";

export function usePost(postId: number, enabled = true) {
  return useQuery({
    queryKey: ["post", postId],
    queryFn: () => postApi.getById(postId),
    enabled,
  });
}