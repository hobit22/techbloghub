import { MetadataRoute } from 'next'
import { BASE_URL } from '@/lib/config'

export default function robots(): MetadataRoute.Robots {
  return {
    rules: {
      userAgent: '*',
      allow: '/',
      disallow: ['/api/', '/admin/', '/_next/'],
    },
    sitemap: `${BASE_URL}/sitemap.xml`,
    host: BASE_URL,
  }
}