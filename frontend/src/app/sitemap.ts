import { MetadataRoute } from "next";

interface Category {
  id: number;
  name: string;
  description: string;
  createdAt: string;
  updatedAt: string;
}

interface Blog {
  id: number;
  name: string;
  company: string;
  rssUrl: string;
  siteUrl: string;
  status: string;
}

async function getCategories(): Promise<Category[]> {
  try {
    const response = await fetch(
      `${
        process.env.NEXT_PUBLIC_API_URL || "https://teckbloghub.kr"
      }/api/categories`,
      {
        next: { revalidate: 3600 }, // 1시간마다 재검증
      }
    );
    if (!response.ok) {
      console.warn("Failed to fetch categories for sitemap");
      return [];
    }
    return await response.json();
  } catch (error) {
    console.warn("Error fetching categories for sitemap:", error);
    return [];
  }
}

async function getActiveBlogs(): Promise<Blog[]> {
  try {
    const response = await fetch(
      `${
        process.env.NEXT_PUBLIC_API_URL || "https://teckbloghub.kr"
      }/api/blogs/active`,
      {
        next: { revalidate: 3600 }, // 1시간마다 재검증
      }
    );
    if (!response.ok) {
      console.warn("Failed to fetch blogs for sitemap");
      return [];
    }
    return await response.json();
  } catch (error) {
    console.warn("Error fetching blogs for sitemap:", error);
    return [];
  }
}

export default async function sitemap(): Promise<MetadataRoute.Sitemap> {
  const baseUrl = process.env.NEXT_PUBLIC_BASE_URL || "https://teckbloghub.kr";

  // 병렬로 데이터 가져오기
  const [categories, blogs] = await Promise.all([
    getCategories(),
    getActiveBlogs(),
  ]);

  const staticPages = [
    {
      url: baseUrl,
      lastModified: new Date(),
      changeFrequency: "hourly" as const,
      priority: 1,
    },
    {
      url: `${baseUrl}/about`,
      lastModified: new Date(),
      changeFrequency: "monthly" as const,
      priority: 0.5,
    },
  ];

  const categoryPages = categories.map((category) => ({
    url: `${baseUrl}/?category=${category.name.toLowerCase()}`,
    lastModified: new Date(),
    changeFrequency: "daily" as const,
    priority: 0.8,
  }));

  const blogPages = blogs.map((blog) => ({
    url: `${baseUrl}/?blogIds=${blog.id}`,
    lastModified: new Date(),
    changeFrequency: "daily" as const,
    priority: 0.8,
  }));

  return [...staticPages, ...categoryPages, ...blogPages];
}
