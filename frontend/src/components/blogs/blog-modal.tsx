'use client';

import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { BlogForm } from '@/components/forms/blog-form';
import type { Blog } from '@/types';
import type { BlogFormData } from '@/lib/utils/validation';

interface BlogModalProps {
  blog?: Blog;
  isOpen: boolean;
  onClose: () => void;
  onSubmit: (data: BlogFormData) => Promise<void>;
  isLoading?: boolean;
}

export function BlogModal({ blog, isOpen, onClose, onSubmit, isLoading }: BlogModalProps) {
  return (
    <Dialog open={isOpen} onOpenChange={onClose}>
      <DialogContent className="max-h-[90vh] max-w-2xl overflow-y-auto border border-white/10 bg-slate-950 text-slate-100 shadow-[0_40px_120px_-50px_rgba(8,47,73,0.85)]">
        <DialogHeader>
          <DialogTitle className="text-xl font-semibold text-white">
            {blog ? '블로그 수정' : '새 블로그 추가'}
          </DialogTitle>
        </DialogHeader>
        <BlogForm
          blog={blog}
          onSubmit={onSubmit}
          onCancel={onClose}
          isLoading={isLoading}
        />
      </DialogContent>
    </Dialog>
  );
}
