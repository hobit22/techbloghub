import { z } from 'zod';
import { BlogType } from '@/types';

export const blogFormSchema = z.object({
  name: z.string().min(1, '블로그 이름은 필수입니다.'),
  company: z.string().min(1, '회사명은 필수입니다.'),
  rss_url: z
    .string()
    .min(1, 'RSS URL은 필수입니다.')
    .regex(/^https?:\/\/.+/, '유효한 RSS URL을 입력해주세요.'),
  site_url: z
    .string()
    .min(1, '사이트 URL은 필수입니다.')
    .regex(/^https?:\/\/.+/, '유효한 사이트 URL을 입력해주세요.'),
  logo_url: z
    .string()
    .regex(/^https?:\/\/.+/, '유효한 로고 URL을 입력해주세요.')
    .optional()
    .or(z.literal('')),
  description: z.string().optional(),
  blog_type: z.nativeEnum(BlogType).optional(),
});

export type BlogFormData = z.infer<typeof blogFormSchema>;
