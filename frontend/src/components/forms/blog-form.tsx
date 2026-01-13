'use client';

import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { Globe, Building2, Rss, ExternalLink, FileText, Image as ImageIcon } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { blogFormSchema, type BlogFormData } from '@/lib/utils/validation';
import { BlogType, type Blog } from '@/types';

interface BlogFormProps {
  blog?: Blog;
  onSubmit: (data: BlogFormData) => Promise<void>;
  onCancel: () => void;
  isLoading?: boolean;
}

export function BlogForm({ blog, onSubmit, onCancel, isLoading = false }: BlogFormProps) {
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<BlogFormData>({
    resolver: zodResolver(blogFormSchema),
    defaultValues: blog
      ? {
          name: blog.name,
          company: blog.company,
          rss_url: blog.rss_url,
          site_url: blog.site_url,
          logo_url: blog.logo_url || '',
          description: blog.description || '',
          blog_type: blog.blog_type || BlogType.COMPANY,
        }
      : {
          name: '',
          company: '',
          rss_url: '',
          site_url: '',
          logo_url: '',
          description: '',
          blog_type: BlogType.COMPANY,
        },
  });

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
      {/* Blog Name */}
      <div className="space-y-2">
        <Label htmlFor="name" className="flex items-center gap-2">
          <Globe className="w-4 h-4" />
          블로그 이름 <span className="text-destructive">*</span>
        </Label>
        <Input
          id="name"
          {...register('name')}
          placeholder="예: Tech Blog"
          disabled={isLoading}
        />
        {errors.name && (
          <p className="text-sm text-destructive">{errors.name.message}</p>
        )}
      </div>

      {/* Company */}
      <div className="space-y-2">
        <Label htmlFor="company" className="flex items-center gap-2">
          <Building2 className="w-4 h-4" />
          회사명 <span className="text-destructive">*</span>
        </Label>
        <Input
          id="company"
          {...register('company')}
          placeholder="예: Example Inc"
          disabled={isLoading}
        />
        {errors.company && (
          <p className="text-sm text-destructive">{errors.company.message}</p>
        )}
      </div>

      {/* RSS URL */}
      <div className="space-y-2">
        <Label htmlFor="rss_url" className="flex items-center gap-2">
          <Rss className="w-4 h-4" />
          RSS URL <span className="text-destructive">*</span>
        </Label>
        <Input
          id="rss_url"
          {...register('rss_url')}
          placeholder="https://example.com/rss"
          disabled={isLoading}
        />
        {errors.rss_url && (
          <p className="text-sm text-destructive">{errors.rss_url.message}</p>
        )}
      </div>

      {/* Site URL */}
      <div className="space-y-2">
        <Label htmlFor="site_url" className="flex items-center gap-2">
          <ExternalLink className="w-4 h-4" />
          사이트 URL <span className="text-destructive">*</span>
        </Label>
        <Input
          id="site_url"
          {...register('site_url')}
          placeholder="https://example.com"
          disabled={isLoading}
        />
        {errors.site_url && (
          <p className="text-sm text-destructive">{errors.site_url.message}</p>
        )}
      </div>

      {/* Logo URL */}
      <div className="space-y-2">
        <Label htmlFor="logo_url" className="flex items-center gap-2">
          <ImageIcon className="w-4 h-4" />
          로고 URL
        </Label>
        <Input
          id="logo_url"
          {...register('logo_url')}
          placeholder="https://example.com/logo.png (선택)"
          disabled={isLoading}
        />
        {errors.logo_url && (
          <p className="text-sm text-destructive">{errors.logo_url.message}</p>
        )}
      </div>

      {/* Description */}
      <div className="space-y-2">
        <Label htmlFor="description" className="flex items-center gap-2">
          <FileText className="w-4 h-4" />
          설명
        </Label>
        <textarea
          id="description"
          {...register('description')}
          placeholder="블로그에 대한 간단한 설명 (선택)"
          rows={3}
          disabled={isLoading}
          className="flex min-h-[80px] w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
        />
        {errors.description && (
          <p className="text-sm text-destructive">{errors.description.message}</p>
        )}
      </div>

      {/* Actions */}
      <div className="flex justify-end gap-3 pt-4">
        <Button type="button" variant="outline" onClick={onCancel} disabled={isLoading}>
          취소
        </Button>
        <Button type="submit" disabled={isLoading}>
          {isLoading ? (blog ? '수정 중...' : '생성 중...') : blog ? '수정' : '생성'}
        </Button>
      </div>
    </form>
  );
}
