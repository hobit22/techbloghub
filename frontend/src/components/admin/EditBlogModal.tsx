'use client';

import { useState, useEffect } from 'react';
import { adminBlogApi } from '@/lib/admin-api';
import { Blog } from '@/types';
import { X, Edit, Globe, Building2, Rss, ExternalLink, FileText, Image } from 'lucide-react';

interface EditBlogModalProps {
  blog: Blog | null;
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void;
}

export default function EditBlogModal({ blog, isOpen, onClose, onSuccess }: EditBlogModalProps) {
  const [formData, setFormData] = useState({
    name: '',
    company: '',
    rssUrl: '',
    siteUrl: '',
    logoUrl: '',
    description: '',
    status: 'ACTIVE',
  });
  const [isLoading, setIsLoading] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    if (blog) {
      setFormData({
        name: blog.name || '',
        company: blog.company || '',
        rssUrl: blog.rss_url || '',
        siteUrl: blog.site_url || '',
        logoUrl: blog.logo_url || '',
        description: blog.description || '',
        status: blog.status || 'ACTIVE',
      });
    }
  }, [blog]);

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));

    if (errors[name]) {
      setErrors(prev => ({ ...prev, [name]: '' }));
    }
  };

  const validateForm = () => {
    const newErrors: Record<string, string> = {};

    if (!formData.name.trim()) {
      newErrors.name = '블로그 이름은 필수입니다.';
    }

    if (!formData.company.trim()) {
      newErrors.company = '회사명은 필수입니다.';
    }

    if (!formData.rssUrl.trim()) {
      newErrors.rssUrl = 'RSS URL은 필수입니다.';
    } else if (!formData.rssUrl.match(/^https?:\/\/.+/)) {
      newErrors.rssUrl = '유효한 RSS URL을 입력해주세요.';
    }

    if (!formData.siteUrl.trim()) {
      newErrors.siteUrl = '사이트 URL은 필수입니다.';
    } else if (!formData.siteUrl.match(/^https?:\/\/.+/)) {
      newErrors.siteUrl = '유효한 사이트 URL을 입력해주세요.';
    }

    if (formData.logoUrl.trim() && !formData.logoUrl.match(/^https?:\/\/.+/)) {
      newErrors.logoUrl = '유효한 로고 URL을 입력해주세요.';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!blog || !validateForm()) {
      return;
    }

    setIsLoading(true);

    try {
      await adminBlogApi.update(blog.id, {
        name: formData.name.trim(),
        company: formData.company.trim(),
        rss_url: formData.rssUrl.trim(),
        site_url: formData.siteUrl.trim(),
        logo_url: formData.logoUrl.trim() || undefined,
        description: formData.description.trim() || undefined,
        status: formData.status,
      });

      setErrors({});
      onSuccess();
      onClose();
    } catch (error) {
      console.error('Error updating blog:', error);
      setErrors({ submit: '블로그 수정 중 오류가 발생했습니다.' });
    } finally {
      setIsLoading(false);
    }
  };

  const handleClose = () => {
    if (!isLoading) {
      setErrors({});
      onClose();
    }
  };

  if (!isOpen || !blog) return null;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg shadow-xl w-full max-w-md mx-4 max-h-[90vh] overflow-y-auto">
        {/* 헤더 */}
        <div className="flex items-center justify-between p-6 border-b">
          <div className="flex items-center space-x-2">
            <Edit className="w-5 h-5 text-green-600" />
            <h2 className="text-lg font-semibold text-gray-900">블로그 편집</h2>
          </div>
          <button
            onClick={handleClose}
            disabled={isLoading}
            className="text-gray-400 hover:text-gray-600 disabled:opacity-50"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* 폼 */}
        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          {/* 블로그 이름 */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              <Globe className="w-4 h-4 inline mr-1" />
              블로그 이름 *
            </label>
            <input
              type="text"
              name="name"
              value={formData.name}
              onChange={handleInputChange}
              placeholder="예: 우아한형제들 기술블로그"
              className={`w-full px-3 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-green-500 text-slate-800 ${
                errors.name ? 'border-red-500' : 'border-gray-300'
              }`}
              disabled={isLoading}
            />
            {errors.name && <p className="text-red-500 text-xs mt-1">{errors.name}</p>}
          </div>

          {/* 회사명 */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              <Building2 className="w-4 h-4 inline mr-1" />
              회사명 *
            </label>
            <input
              type="text"
              name="company"
              value={formData.company}
              onChange={handleInputChange}
              placeholder="예: 우아한형제들"
              className={`w-full px-3 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-green-500 text-slate-800 ${
                errors.company ? 'border-red-500' : 'border-gray-300'
              }`}
              disabled={isLoading}
            />
            {errors.company && <p className="text-red-500 text-xs mt-1">{errors.company}</p>}
          </div>

          {/* RSS URL */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              <Rss className="w-4 h-4 inline mr-1" />
              RSS URL *
            </label>
            <input
              type="url"
              name="rssUrl"
              value={formData.rssUrl}
              onChange={handleInputChange}
              placeholder="https://techblog.woowahan.com/feed"
              className={`w-full px-3 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-green-500 text-slate-800 ${
                errors.rssUrl ? 'border-red-500' : 'border-gray-300'
              }`}
              disabled={isLoading}
            />
            {errors.rssUrl && <p className="text-red-500 text-xs mt-1">{errors.rssUrl}</p>}
          </div>

          {/* 사이트 URL */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              <ExternalLink className="w-4 h-4 inline mr-1" />
              사이트 URL *
            </label>
            <input
              type="url"
              name="siteUrl"
              value={formData.siteUrl}
              onChange={handleInputChange}
              placeholder="https://techblog.woowahan.com"
              className={`w-full px-3 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-green-500 text-slate-800 ${
                errors.siteUrl ? 'border-red-500' : 'border-gray-300'
              }`}
              disabled={isLoading}
            />
            {errors.siteUrl && <p className="text-red-500 text-xs mt-1">{errors.siteUrl}</p>}
          </div>

          {/* 로고 URL */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              <Image className="w-4 h-4 inline mr-1" />
              로고 URL (선택)
            </label>
            <input
              type="url"
              name="logoUrl"
              value={formData.logoUrl}
              onChange={handleInputChange}
              placeholder="https://techblog.woowahan.com/logo.png"
              className={`w-full px-3 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-green-500 text-slate-800 ${
                errors.logoUrl ? 'border-red-500' : 'border-gray-300'
              }`}
              disabled={isLoading}
            />
            {errors.logoUrl && <p className="text-red-500 text-xs mt-1">{errors.logoUrl}</p>}
          </div>

          {/* 설명 */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              <FileText className="w-4 h-4 inline mr-1" />
              설명 (선택)
            </label>
            <textarea
              name="description"
              value={formData.description}
              onChange={handleInputChange}
              placeholder="블로그에 대한 간단한 설명을 입력하세요."
              rows={3}
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-green-500 resize-none text-slate-800"
              disabled={isLoading}
            />
          </div>

          {/* 상태 */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              상태
            </label>
            <select
              name="status"
              value={formData.status}
              onChange={handleInputChange}
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-green-500 text-slate-800"
              disabled={isLoading}
            >
              <option value="ACTIVE">활성</option>
              <option value="INACTIVE">비활성</option>
              <option value="SUSPENDED">일시중지</option>
            </select>
          </div>

          {/* 에러 메시지 */}
          {errors.submit && (
            <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-md text-sm">
              {errors.submit}
            </div>
          )}

          {/* 버튼 */}
          <div className="flex space-x-3 pt-4">
            <button
              type="button"
              onClick={handleClose}
              disabled={isLoading}
              className="flex-1 px-4 py-2 text-sm font-medium text-gray-700 bg-gray-100 border border-gray-300 rounded-md hover:bg-gray-200 focus:outline-none focus:ring-2 focus:ring-gray-500 disabled:opacity-50"
            >
              취소
            </button>
            <button
              type="submit"
              disabled={isLoading}
              className="flex-1 px-4 py-2 text-sm font-medium text-white bg-green-600 rounded-md hover:bg-green-700 focus:outline-none focus:ring-2 focus:ring-green-500 disabled:opacity-50"
            >
              {isLoading ? (
                <>
                  <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin inline mr-2"></div>
                  수정 중...
                </>
              ) : (
                '수정하기'
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
