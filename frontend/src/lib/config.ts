/**
 * Application Configuration
 *
 * Centralized configuration for environment variables and constants.
 * All environment-specific values should be accessed through this file.
 */

/**
 * API Base URL - Backend FastAPI server
 * @default 'https://api.teckbloghub.kr'
 */
export const API_URL = process.env.NEXT_PUBLIC_API_URL || 'https://api.teckbloghub.kr';

/**
 * Application Base URL - Frontend application URL
 * @default 'https://teckbloghub.kr'
 */
export const BASE_URL = process.env.NEXT_PUBLIC_BASE_URL || 'https://teckbloghub.kr';

/**
 * Google Analytics Measurement ID
 */
export const GA_MEASUREMENT_ID = process.env.NEXT_PUBLIC_GA_ID;

/**
 * Environment check
 */
export const IS_PRODUCTION = process.env.NODE_ENV === 'production';
export const IS_DEVELOPMENT = process.env.NODE_ENV === 'development';

/**
 * Site metadata
 */
export const SITE_NAME = 'TeckBlogHub';
export const SITE_DESCRIPTION = '국내 주요 기업 기술 블로그를 한곳에서 모아보는 플랫폼';
export const TWITTER_HANDLE = '@teckbloghub';

/**
 * Generate absolute URL for a given path
 * @param path - Path to append to base URL (e.g., '/posts/123')
 * @returns Absolute URL
 */
export function getAbsoluteUrl(path: string): string {
  // Remove leading slash if present to avoid double slashes
  const cleanPath = path.startsWith('/') ? path.slice(1) : path;
  return `${BASE_URL}/${cleanPath}`;
}

/**
 * Generate post detail URL
 * @param postId - Post ID
 * @returns Absolute URL for post detail page
 */
export function getPostUrl(postId: number): string {
  return getAbsoluteUrl(`posts/${postId}`);
}
