'use client';

import { useState } from 'react';
import { Blog, BlogType } from '@/types';
import { RefreshCw, Globe, Activity, Calendar, Plus, Download, Trash2, Edit, Sparkles, Radio, ShieldCheck } from 'lucide-react';
import { BlogModal } from '@/components/blogs/blog-modal';
import { useCreateBlog, useUpdateBlog, useDeleteBlog } from '@/lib/hooks/use-blogs';
import { useAdminBlogs } from '@/lib/hooks/use-admin';
import { adminApi } from '@/lib/api/endpoints/admin';
import type { BlogFormData } from '@/lib/utils/validation';
import { logger } from '@/lib/config';
import { AdminActionButton, AdminBadge, AdminPageHeader, AdminStatCard, AdminSurface } from '@/components/admin/admin-ui';

export default function AdminBlogsPage() {
  const { data: blogsData, isLoading, refetch } = useAdminBlogs(0, 100);
  const createBlogMutation = useCreateBlog();
  const updateBlogMutation = useUpdateBlog();
  const deleteBlogMutation = useDeleteBlog();

  const [triggering, setTriggering] = useState<number | null>(null);
  const [isAddModalOpen, setIsAddModalOpen] = useState(false);
  const [processingBlogId, setProcessingBlogId] = useState<number | null>(null);
  const [editingBlog, setEditingBlog] = useState<Blog | null>(null);
  const [isImportingVerified, setIsImportingVerified] = useState(false);

  const blogs = blogsData?.blogs || [];
  const activeBlogs = blogs.filter((blog) => blog.status === 'ACTIVE').length;
  const suspendedBlogs = blogs.filter((blog) => blog.status === 'SUSPENDED').length;
  const totalPosts = blogs.reduce((sum, blog) => sum + (blog.post_count ?? 0), 0);

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

  const handleImportVerifiedBlogs = async () => {
    if (!confirm('Velopers 기반 검증 RSS 블로그들을 일괄 추가하시겠습니까?')) return;

    try {
      setIsImportingVerified(true);
      const result = await adminApi.importVerifiedVelopersBlogs();
      alert(
        `검증 블로그 가져오기 완료!\n- 요청: ${result.summary.requested}개\n- 생성: ${result.summary.created}개\n- 중복 스킵: ${result.summary.skipped}개`
      );
      refetch();
    } catch (error) {
      alert('검증 블로그 가져오기 중 오류가 발생했습니다.');
      logger.error('Error importing verified blogs:', error);
    } finally {
      setIsImportingVerified(false);
    }
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
        <AdminPageHeader title="블로그 관리" description="등록된 블로그와 검증 소스를 불러오는 중입니다." />
        <AdminSurface className="p-8 text-center">
          <div className="mx-auto mb-4 h-8 w-8 animate-spin rounded-full border-4 border-cyan-400 border-t-transparent"></div>
          <p className="text-sm text-slate-300">블로그 목록을 불러오는 중...</p>
        </AdminSurface>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <AdminPageHeader
        eyebrow="Sources"
        title="블로그 관리"
        description="등록된 기술 블로그를 운영하고, Velopers 기준으로 검증한 소스를 일괄 가져오며, 블로그 단위 RSS/본문 작업을 실행합니다."
        actions={(
          <>
            <AdminActionButton
            onClick={handleImportVerifiedBlogs}
            disabled={isImportingVerified}
              tone="warning"
          >
            <Sparkles className={`w-4 h-4 mr-2 inline ${isImportingVerified ? 'animate-spin' : ''}`} />
            Velopers 검증 소스 추가
            </AdminActionButton>
            <AdminActionButton
            onClick={() => setIsAddModalOpen(true)}
              tone="success"
          >
            <Plus className="w-4 h-4 mr-2 inline" />
            블로그 추가
            </AdminActionButton>
            <AdminActionButton
            onClick={handleAllRecrawl}
            disabled={triggering !== null}
              tone="primary"
          >
            <RefreshCw className={`w-4 h-4 mr-2 inline ${triggering === -1 ? 'animate-spin' : ''}`} />
            전체 RSS 수집
            </AdminActionButton>
          </>
        )}
      />

      <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-4">
        <AdminStatCard label="등록 블로그" value={blogs.length.toLocaleString()} description="현재 어드민에 등록된 전체 블로그 수" icon={<Globe className="h-5 w-5" />} />
        <AdminStatCard label="활성 블로그" value={activeBlogs.toLocaleString()} description="정상 수집 대상 상태의 블로그" icon={<Radio className="h-5 w-5" />} tone="success" />
        <AdminStatCard label="누적 포스트" value={totalPosts.toLocaleString()} description="연결된 블로그들이 보유한 전체 포스트" icon={<Activity className="h-5 w-5" />} />
        <AdminStatCard label="주의 필요" value={suspendedBlogs.toLocaleString()} description="중단 상태이거나 재확인이 필요한 블로그" icon={<ShieldCheck className="h-5 w-5" />} tone={suspendedBlogs > 0 ? 'warning' : 'default'} />
      </div>

      <AdminSurface className="overflow-hidden">
        <div className="flex flex-col gap-3 border-b border-white/10 px-6 py-5 lg:flex-row lg:items-center lg:justify-between">
          <div>
            <h2 className="text-xl font-semibold text-white">운영 중인 블로그</h2>
            <p className="mt-1 text-sm text-slate-400">RSS 주소, 상태, 포스트 수, 마지막 크롤링 시점을 빠르게 확인할 수 있습니다.</p>
          </div>
          <AdminBadge>{blogs.length} sources</AdminBadge>
        </div>
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-white/10">
            <thead className="bg-white/[0.03]">
              <tr>
                <th className="px-6 py-4 text-left text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">블로그</th>
                <th className="px-6 py-4 text-left text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">회사</th>
                <th className="px-6 py-4 text-left text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">상태</th>
                <th className="px-6 py-4 text-left text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">포스트</th>
                <th className="px-6 py-4 text-left text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">마지막 크롤링</th>
                <th className="px-6 py-4 text-right text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">작업</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-white/5 bg-transparent">
              {blogs.map((blog) => {
                const postCount = blog.post_count ?? 0;
                return (
                  <tr key={blog.id} className="transition hover:bg-white/[0.03]">
                    <td className="px-6 py-4">
                      <div className="flex items-center gap-3">
                        <div className="rounded-2xl border border-white/10 bg-white/5 p-2 text-slate-400">
                          <Globe className="h-4 w-4" />
                        </div>
                        <div>
                          <div className="text-sm font-semibold text-white">{blog.name}</div>
                          <div className="text-sm text-slate-400">{blog.rss_url}</div>
                        </div>
                      </div>
                    </td>
                    <td className="px-6 py-4">
                      <div className="text-sm text-slate-200">{blog.company}</div>
                    </td>
                    <td className="px-6 py-4">
                      <span className={`rounded-full px-3 py-1 text-xs font-semibold ${getStatusColor(blog.status)}`}>
                        {blog.status}
                      </span>
                    </td>
                    <td className="px-6 py-4">
                      <div className="flex items-center text-sm text-slate-200">
                        <Activity className="mr-1 h-4 w-4 text-slate-500" />
                        {postCount.toLocaleString()}
                      </div>
                    </td>
                    <td className="px-6 py-4">
                      <div className="flex items-center text-sm text-slate-400">
                        <Calendar className="mr-1 h-4 w-4" />
                        {formatDate(blog.last_crawled_at || '')}
                      </div>
                    </td>
                    <td className="px-6 py-4 text-right text-sm font-medium">
                      <div className="flex justify-end gap-2">
                      <AdminActionButton
                        onClick={() => setEditingBlog(blog)}
                        tone="default"
                        className="h-10 w-10 rounded-2xl px-0"
                      >
                        <Edit className="h-4 w-4" />
                      </AdminActionButton>
                      <AdminActionButton
                        onClick={() => handleBlogRecrawl(blog.id, blog.name)}
                        disabled={triggering === blog.id}
                        tone="success"
                        className="h-10 w-10 rounded-2xl px-0"
                      >
                        <RefreshCw className={`h-4 w-4 ${triggering === blog.id ? 'animate-spin' : ''}`} />
                      </AdminActionButton>
                      <AdminActionButton
                        onClick={() => handleProcessBlogPosts(blog.id, blog.name)}
                        disabled={processingBlogId === blog.id}
                        tone="primary"
                        className="h-10 w-10 rounded-2xl px-0"
                      >
                        <Download className={`h-4 w-4 ${processingBlogId === blog.id ? 'animate-spin' : ''}`} />
                      </AdminActionButton>
                      <AdminActionButton
                        onClick={() => handleDeleteBlog(blog.id, blog.name)}
                        tone="danger"
                        className="h-10 w-10 rounded-2xl px-0"
                      >
                        <Trash2 className="h-4 w-4" />
                      </AdminActionButton>
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </AdminSurface>

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
