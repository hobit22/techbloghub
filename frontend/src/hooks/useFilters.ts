"use client";

import { useQuery } from "@tanstack/react-query";
import { blogApi } from "@/lib/api";
import { useMemo } from "react";
import { Blog, TagResponse, CategoryResponse } from "@/types";

export function useBlogs(initialData?: Blog[]) {
  return useQuery({
    queryKey: ["blogs"],
    queryFn: blogApi.getActive,
    // 서버에서 받은 초기 데이터가 있으면 사용
    ...(initialData && {
      initialData,
      staleTime: 30 * 60 * 1000, // 30분 (정적 데이터)
    }),
  });
}

// export function useTags(initialData?: TagResponse[]) {
//   return useQuery({
//     queryKey: ["tags"],
//     queryFn: tagApi.getAll,
//     ...(initialData && {
//       initialData,
//       staleTime: 30 * 60 * 1000, // 30분 (정적 데이터)
//     }),
//   });
// }

// export function useCategories(initialData?: CategoryResponse[]) {
//   return useQuery({
//     queryKey: ["categories"],
//     queryFn: categoryApi.getAll,
//     ...(initialData && {
//       initialData,
//       staleTime: 30 * 60 * 1000, // 30분 (정적 데이터)
//     }),
//   });
// }

// export function useAvailableFilters(
//   initialTags?: string[],
//   initialCategories?: string[]
// ) {
//   // 초기 데이터를 적절한 형태로 변환하여 전달
//   const initialTagsData = initialTags?.map((name) => ({
//     id: 0,
//     name,
//     description: undefined,
//     createdAt: "",
//     updatedAt: "",
//   }));
//   const initialCategoriesData = initialCategories?.map((name) => ({
//     id: 0,
//     name,
//     description: undefined,
//     color: undefined,
//     createdAt: "",
//     updatedAt: "",
//   }));

// const { data: tagsData } = useTags(initialTagsData);
// const { data: categoriesData } = useCategories(initialCategoriesData);

//   const filters = useMemo(() => {
//     // 서버에서 받은 초기 데이터가 있으면 우선 사용
//     if (initialTags && initialCategories && (!tagsData || !categoriesData)) {
//       return {
//         tags: initialTags,
//         categories: initialCategories,
//       };
//     }

//     return {
//       tags: (tagsData || []).map((tag) => tag.name).sort(),
//       categories: (categoriesData || [])
//         .map((category) => category.name)
//         .sort(),
//     };
//   }, [tagsData, categoriesData, initialTags, initialCategories]);

//   return filters;
// }

// 태그 검색을 위한 프론트엔드 필터링 훅
// export function useTagSearch(query?: string) {
//   const { data: allTags } = useTags();

//   const filteredTags = useMemo(() => {
//     if (!allTags || !query || query.trim() === "") {
//       return allTags || [];
//     }

//     const searchTerm = query.trim().toLowerCase();

//     return allTags
//       .filter((tag) => tag.name.toLowerCase().includes(searchTerm))
//       .slice(0, 20); // 성능을 위해 최대 20개로 제한
//   }, [allTags, query]);

//   return filteredTags;
// }
