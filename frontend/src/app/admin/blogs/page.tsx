'use client';

import { useState } from 'react';
import { Blog, BlogType } from '@/types';
import { RefreshCw, Globe, Activity, Calendar, Plus, Download, Trash2, Edit } from 'lucide-react';
import { BlogModal } from '@/components/blogs/blog-modal';
import { useCreateBlog, useUpdateBlog, useDeleteBlog } from '@/lib/hooks/use-blogs';
import { useAdminBlogs } from '@/lib/hooks/use-admin';
import { adminApi } from '@/lib/api/endpoints/admin';
import type { BlogFormData } from '@/lib/utils/validation';
import { logger } from '@/lib/config';

export default function AdminBlogsPage() {
  const { data: blogsData, isLoading, refetch } = useAdminBlogs(0, 100);
  const createBlogMutation = useCreateBlog();
  const updateBlogMutation = useUpdateBlog();
  const deleteBlogMutation = useDeleteBlog();

  const [triggering, setTriggering] = useState<number | null>(null);
  const [isAddModalOpen, setIsAddModalOpen] = useState(false);
  const [processingBlogId, setProcessingBlogId] = useState<number | null>(null);
  const [editingBlog, setEditingBlog] = useState<Blog | null>(null);

  const blogs = blogsData?.blogs || [];

  const handleCreateBlog = async (data: BlogFormData) => {
    await createBlogMutation.mutateAsync({
      ...data,
      blog_type: BlogType.COMPANY,
    });
    setIsAddModalOpen(false);
  };

  const handleUpdateBlog = async (data: BlogFormData) => {
    if (!editingBlog) return;
    await updateBlogMutation.mutateAsync({
      id: editingBlog.id,
      data,
    });
    setEditingBlog(null);
  };

  const handleAllRecrawl = async () => {
    if (!confirm('모든 블로그의 RSS 수집을 시작하시겠습니까?')) return;

    try {
      setTriggering(-1);
      const result = await adminApi.collectAllRSS();
      alert(`전체 RSS 수집 완료!\n- 처리된 블로그: ${result.summary.blogs_processed}개\n- 새 포스트: ${result.summary.new_posts}개\n- 중복 스킵: ${result.summary.skipped_duplicates}개`);
      refetch();
    } catch (error) {
      alert('전체 재크롤링 요청 중 오류가 발생했습니다.');
      logger.error('Error triggering all recrawl:', error);
    } finally {
      setTriggering(null);
    }
  };

  const handleBlogRecrawl = async (blogId: number, blogName: string) => {
    if (!confirm(`"${blogName}"의 RSS 수집을 시작하시겠습니까?`)) return;

    try {
      setTriggering(blogId);
      const result = await adminApi.collectBlogRSS(blogId);
      alert(`RSS 수집 완료!\n- 새 포스트: ${result.summary.new_posts}개\n- 중복 스킵: ${result.summary.skipped_duplicates}개`);
      refetch();
    } catch (error) {
      alert('RSS 수집 요청 중 오류가 발생했습니다.');
      logger.error('Error triggering blog recrawl:', error);
    } finally {
      setTriggering(null);
    }
  };

  const handleDeleteBlog = async (blogId: number, blogName: string) => {
    if (!confirm(`정말로 "${blogName}" 블로그를 삭제하시겠습니까?\n관련된 모든 포스트도 함께 삭제됩니다.`)) return;

    try {
      await deleteBlogMutation.mutateAsync(blogId);
      alert('블로그가 삭제되었습니다.');
    } catch (error) {
      alert('블로그 삭제 중 오류가 발생했습니다.');
      logger.error('Error deleting blog:', error);
    }
  };

  const handleProcessBlogPosts = async (blogId: number, blogName: string) => {
    if (!confirm(`"${blogName}"의 pending posts 본문을 추출하시겠습니까?`)) return;

    try {
      setProcessingBlogId(blogId);
      const result = await adminApi.processBlogPosts(blogId);

      if (result.summary.completed > 0) {
        alert(`본문 추출 완료!\n처리: ${result.summary.total_processed}개\n성공: ${result.summary.completed}개\n실패: ${result.summary.failed}개`);
        refetch();
      } else {
        const errorMsg = result.summary.errors[0]?.error || '알 수 없는 오류';
        alert(`본문 추출 실패:\n${errorMsg}`);
      }
    } catch (error) {
      alert('본문 추출 중 오류가 발생했습니다.');
      logger.error('Error processing blog posts:', error);
    } finally {
      setProcessingBlogId(null);
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'ACTIVE':
        return 'text-green-600 bg-green-100';
      case 'INACTIVE':
        return 'text-red-600 bg-red-100';
      default:
        return 'text-gray-600 bg-gray-100';
    }
  };

  const formatDate = (dateString: string) => {
    if (!dateString) return '-';
    return new Date(dateString).toLocaleString('ko-KR');
  };

  if (isLoading) {
    return (
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <h1 className="text-2xl font-bold text-gray-900">블로그 관리</h1>
        </div>
        <div className="bg-white rounded-lg shadow p-8 text-center">
          <div className="w-8 h-8 border-4 border-blue-600 border-t-transparent rounded-full animate-spin mx-auto mb-4"></div>
          <p className="text-gray-600">블로그 목록을 불러오는 중...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* 헤더 */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">블로그 관리</h1>
          <p className="text-gray-600 mt-1">등록된 블로그 목록 및 크롤링 관리</p>
        </div>
        <div className="flex space-x-3">
          <button
            onClick={() => setIsAddModalOpen(true)}
            className="px-4 py-2 text-sm font-medium text-white bg-green-600 rounded-md hover:bg-green-700 focus:outline-none focus:ring-2 focus:ring-green-500"
          >
            <Plus className="w-4 h-4 mr-2 inline" />
            블로그 추가
          </button>
          <button
            onClick={handleAllRecrawl}
            disabled={triggering !== null}
            className="px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-md hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:opacity-50"
          >
            <RefreshCw className={`w-4 h-4 mr-2 inline ${triggering === -1 ? 'animate-spin' : ''}`} />
            전체 RSS 수집
          </button>
        </div>
      </div>

      {/* 블로그 목록 */}
      <div className="bg-white rounded-lg shadow overflow-hidden">
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">블로그</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">회사</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">상태</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">포스트 수</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">마지막 크롤링</th>
                <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase">작업</th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {blogs.map((blog) => {
                const postCount = blog.post_count ?? 0;
                return (
                  <tr key={blog.id} className="hover:bg-gray-50">
                    <td className="px-6 py-4">
                      <div className="flex items-center">
                        <Globe className="w-5 h-5 text-gray-400 mr-2" />
                        <div>
                          <div className="text-sm font-medium text-gray-900">{blog.name}</div>
                          <div className="text-sm text-gray-500">{blog.rss_url}</div>
                        </div>
                      </div>
                    </td>
                    <td className="px-6 py-4">
                      <div className="text-sm text-gray-900">{blog.company}</div>
                    </td>
                    <td className="px-6 py-4">
                      <span className={`px-2 py-1 text-xs font-medium rounded-full ${getStatusColor(blog.status)}`}>
                        {blog.status}
                      </span>
                    </td>
                    <td className="px-6 py-4">
                      <div className="flex items-center text-sm text-gray-900">
                        <Activity className="w-4 h-4 mr-1 text-gray-400" />
                        {postCount.toLocaleString()}
                      </div>
                    </td>
                    <td className="px-6 py-4">
                      <div className="flex items-center text-sm text-gray-500">
                        <Calendar className="w-4 h-4 mr-1" />
                        {formatDate(blog.last_crawled_at || '')}
                      </div>
                    </td>
                    <td className="px-6 py-4 text-right text-sm font-medium space-x-2">
                      <button
                        onClick={() => setEditingBlog(blog)}
                        className="text-blue-600 hover:text-blue-900"
                      >
                        <Edit className="w-4 h-4 inline" />
                      </button>
                      <button
                        onClick={() => handleBlogRecrawl(blog.id, blog.name)}
                        disabled={triggering === blog.id}
                        className="text-green-600 hover:text-green-900 disabled:opacity-50"
                      >
                        <RefreshCw className={`w-4 h-4 inline ${triggering === blog.id ? 'animate-spin' : ''}`} />
                      </button>
                      <button
                        onClick={() => handleProcessBlogPosts(blog.id, blog.name)}
                        disabled={processingBlogId === blog.id}
                        className="text-purple-600 hover:text-purple-900 disabled:opacity-50"
                      >
                        <Download className={`w-4 h-4 inline ${processingBlogId === blog.id ? 'animate-spin' : ''}`} />
                      </button>
                      <button
                        onClick={() => handleDeleteBlog(blog.id, blog.name)}
                        className="text-red-600 hover:text-red-900"
                      >
                        <Trash2 className="w-4 h-4 inline" />
                      </button>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>

      {/* Modals */}
      <BlogModal
        isOpen={isAddModalOpen}
        onClose={() => setIsAddModalOpen(false)}
        onSubmit={handleCreateBlog}
        isLoading={createBlogMutation.isPending}
      />

      <BlogModal
        blog={editingBlog || undefined}
        isOpen={!!editingBlog}
        onClose={() => setEditingBlog(null)}
        onSubmit={handleUpdateBlog}
        isLoading={updateBlogMutation.isPending}
      />
    </div>
  );
}
