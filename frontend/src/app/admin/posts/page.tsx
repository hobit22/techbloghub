'use client';

import { useState } from 'react';
import { Search, Filter, RefreshCw, FileText, Calendar, User, ExternalLink, Trash2, Download, Database, ScanSearch } from 'lucide-react';
import { usePosts, useSearchPosts, useDeletePost } from '@/lib/hooks/use-posts';
import { useProcessPost } from '@/lib/hooks/use-admin';
import { logger } from '@/lib/config';
import { AdminActionButton, AdminBadge, AdminPageHeader, AdminStatCard, AdminSurface } from '@/components/admin/admin-ui';

export default function AdminPostsPage() {
  const [currentPage, setCurrentPage] = useState(0);
  const [processingPostId, setProcessingPostId] = useState<number | null>(null);

  // Filter state
  const [filters, setFilters] = useState({
    keyword: '',
    blogId: '',
  });

  const [showFilters, setShowFilters] = useState(false);
  const [isSearchMode, setIsSearchMode] = useState(false);

  const pageSize = 20;

  // Use search or list query based on mode
  const {
    data: searchData,
    isLoading: searchLoading,
    refetch: refetchSearch,
  } = useSearchPosts(
    filters.keyword.trim(),
    {
      limit: pageSize,
      offset: currentPage * pageSize,
      ...(filters.blogId && { blog_ids: [parseInt(filters.blogId)] }),
    }
  );

  const {
    data: listData,
    isLoading: listLoading,
    refetch: refetchList,
  } = usePosts({
    skip: currentPage * pageSize,
    limit: pageSize,
    ...(filters.blogId && { blog_ids: [parseInt(filters.blogId)] }),
  });

  const deletePostMutation = useDeletePost();
  const processPostMutation = useProcessPost();

  const isLoading = isSearchMode ? searchLoading : listLoading;
  const posts = isSearchMode ? (searchData?.results || []) : (listData?.posts || []);
  const totalElements = isSearchMode ? (searchData?.total || 0) : (listData?.total || 0);

  const refetchCurrentPosts = () => {
    if (isSearchMode) {
      void refetchSearch();
      return;
    }

    void refetchList();
  };

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    setCurrentPage(0);
    setIsSearchMode(true);
  };

  const handleFilterChange = (key: string, value: string) => {
    setFilters((prev) => ({ ...prev, [key]: value }));
  };

  const handleReset = () => {
    setFilters({
      keyword: '',
      blogId: '',
    });
    setCurrentPage(0);
    setIsSearchMode(false);
  };

  const handleProcessPost = async (postId: number, postTitle: string) => {
    if (!confirm(`"${postTitle}"의 본문을 추출하시겠습니까?`)) return;

    try {
      setProcessingPostId(postId);
      const result = await processPostMutation.mutateAsync(postId);

      if (result.result.success) {
        alert('본문 추출이 완료되었습니다!');
        refetchCurrentPosts();
      } else {
        alert(`본문 추출 실패:\n${result.result.error || '알 수 없는 오류'}`);
        refetchCurrentPosts();
      }
    } catch (error) {
      alert('본문 추출 중 오류가 발생했습니다.');
      logger.error('Error processing post:', error);
    } finally {
      setProcessingPostId(null);
    }
  };

  const handleDeletePost = async (postId: number, postTitle: string) => {
    if (!confirm(`정말로 "${postTitle}" 포스트를 삭제하시겠습니까?`)) return;

    try {
      await deletePostMutation.mutateAsync(postId);
      alert('포스트가 삭제되었습니다.');
    } catch (error) {
      alert('포스트 삭제 중 오류가 발생했습니다.');
      logger.error('Error deleting post:', error);
    }
  };

  const formatDate = (dateString: string) => {
    if (!dateString) return '-';
    return new Date(dateString).toLocaleString('ko-KR');
  };

  const totalPages = Math.ceil(totalElements / pageSize);

  if (isLoading && posts.length === 0) {
    return (
      <div className="space-y-6">
        <AdminPageHeader title="포스트 관리" description="포스트 데이터를 불러오는 중입니다." />
        <AdminSurface className="p-8 text-center">
          <div className="mx-auto mb-4 h-8 w-8 animate-spin rounded-full border-4 border-cyan-400 border-t-transparent"></div>
          <p className="text-sm text-slate-300">포스트 목록을 불러오는 중...</p>
        </AdminSurface>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <AdminPageHeader
        eyebrow="Content Queue"
        title="포스트 관리"
        description="검색, 필터링, 본문 추출, 삭제까지 포스트 단위 운영 작업을 빠르게 실행할 수 있습니다."
        actions={
          <AdminActionButton
          onClick={() => setShowFilters(!showFilters)}
            tone="default"
          >
            <Filter className="mr-2 h-4 w-4" />
            {showFilters ? '필터 숨기기' : '필터 보기'}
          </AdminActionButton>
        }
      />

      <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
        <AdminStatCard label="현재 결과" value={totalElements.toLocaleString()} description="현재 조회 기준에 맞는 포스트 수" icon={<Database className="h-5 w-5" />} />
        <AdminStatCard label="페이지" value={`${currentPage + 1}/${Math.max(totalPages, 1)}`} description="현재 탐색 중인 결과 페이지" icon={<Calendar className="h-5 w-5" />} />
        <AdminStatCard label="검색 모드" value={isSearchMode ? 'ON' : 'OFF'} description={isSearchMode ? '키워드 기반 검색 결과를 보고 있습니다.' : '일반 목록 모드입니다.'} icon={<ScanSearch className="h-5 w-5" />} tone={isSearchMode ? 'success' : 'default'} />
      </div>

      {showFilters && (
        <AdminSurface className="p-5">
          <form onSubmit={handleSearch} className="space-y-4">
            <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
              <div>
                <label className="mb-2 block text-sm font-medium text-slate-200">
                  검색어
                </label>
                <input
                  type="text"
                  value={filters.keyword}
                  onChange={(e) => handleFilterChange('keyword', e.target.value)}
                  placeholder="제목으로 검색..."
                  className="w-full rounded-2xl border border-white/10 bg-white/5 px-4 py-3 text-slate-100 placeholder:text-slate-500 focus:border-cyan-400/40 focus:outline-none focus:ring-2 focus:ring-cyan-400/20"
                />
              </div>
              <div>
                <label className="mb-2 block text-sm font-medium text-slate-200">
                  블로그 ID
                </label>
                <input
                  type="number"
                  value={filters.blogId}
                  onChange={(e) => handleFilterChange('blogId', e.target.value)}
                  placeholder="블로그 ID"
                  className="w-full rounded-2xl border border-white/10 bg-white/5 px-4 py-3 text-slate-100 placeholder:text-slate-500 focus:border-cyan-400/40 focus:outline-none focus:ring-2 focus:ring-cyan-400/20"
                />
              </div>
            </div>
            <div className="flex flex-wrap gap-3">
              <AdminActionButton type="submit" tone="primary">
                <Search className="mr-2 h-4 w-4" />
                검색 실행
              </AdminActionButton>
              <AdminActionButton
                type="button"
                tone="default"
                onClick={handleReset}
              >
                <RefreshCw className="mr-2 h-4 w-4" />
                초기화
              </AdminActionButton>
            </div>
          </form>
        </AdminSurface>
      )}

      <AdminSurface className="overflow-hidden">
        <div className="flex flex-col gap-3 border-b border-white/10 px-6 py-5 lg:flex-row lg:items-center lg:justify-between">
          <div>
            <h2 className="text-xl font-semibold text-white">포스트 대기열</h2>
            <p className="mt-1 text-sm text-slate-400">제목, 출처, 작성자, 발행일을 빠르게 확인하고 필요한 포스트만 처리합니다.</p>
          </div>
          <AdminBadge>{posts.length} rows</AdminBadge>
        </div>
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-white/10">
            <thead className="bg-white/[0.03]">
              <tr>
                <th className="px-6 py-4 text-left text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">
                  제목
                </th>
                <th className="px-6 py-4 text-left text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">
                  블로그
                </th>
                <th className="px-6 py-4 text-left text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">
                  작성자
                </th>
                <th className="px-6 py-4 text-left text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">
                  발행일
                </th>
                <th className="px-6 py-4 text-right text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">
                  작업
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-white/5 bg-transparent">
              {posts.map((post) => (
                <tr key={post.id} className="transition hover:bg-white/[0.03]">
                  <td className="px-6 py-4">
                    <div className="flex items-center gap-3">
                      <div className="rounded-2xl border border-white/10 bg-white/5 p-2 text-slate-400">
                        <FileText className="h-4 w-4" />
                      </div>
                      <div className="max-w-md">
                        <div className="truncate text-sm font-semibold text-white">
                          {post.title}
                        </div>
                        <a
                          href={post.original_url}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="mt-1 flex items-center text-sm text-cyan-300 hover:text-cyan-200"
                        >
                          <ExternalLink className="mr-1 h-3 w-3" />
                          원문 보기
                        </a>
                      </div>
                    </div>
                  </td>
                  <td className="px-6 py-4">
                    <div className="text-sm text-slate-200">{post.blog?.name || '-'}</div>
                    <div className="text-sm text-slate-400">{post.blog?.company || '-'}</div>
                  </td>
                  <td className="px-6 py-4">
                    <div className="flex items-center text-sm text-slate-200">
                      <User className="mr-1 h-4 w-4 text-slate-500" />
                      {post.author || '-'}
                    </div>
                  </td>
                  <td className="px-6 py-4">
                    <div className="flex items-center text-sm text-slate-400">
                      <Calendar className="mr-1 h-4 w-4" />
                      {formatDate(post.published_at)}
                    </div>
                  </td>
                  <td className="px-6 py-4 text-right text-sm font-medium">
                    <div className="flex justify-end gap-2">
                      <AdminActionButton
                        onClick={() => handleProcessPost(post.id, post.title)}
                        disabled={processingPostId === post.id}
                        tone="primary"
                        className="h-10 w-10 rounded-2xl px-0"
                      >
                        <Download className={`h-4 w-4 ${processingPostId === post.id ? 'animate-spin' : ''}`} />
                      </AdminActionButton>
                      <AdminActionButton
                        onClick={() => handleDeletePost(post.id, post.title)}
                        tone="danger"
                        className="h-10 w-10 rounded-2xl px-0"
                      >
                        <Trash2 className="h-4 w-4" />
                      </AdminActionButton>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </AdminSurface>

      {totalPages > 1 && (
        <AdminSurface className="flex items-center justify-between px-4 py-3 sm:px-5">
          <div className="text-sm text-slate-300">
            페이지 {currentPage + 1} / {totalPages}
          </div>
          <div className="flex gap-2">
            <AdminActionButton
              onClick={() => setCurrentPage((p) => Math.max(0, p - 1))}
              disabled={currentPage === 0}
              tone="default"
            >
              이전
            </AdminActionButton>
            <AdminActionButton
              onClick={() => setCurrentPage((p) => Math.min(totalPages - 1, p + 1))}
              disabled={currentPage >= totalPages - 1}
              tone="default"
            >
              다음
            </AdminActionButton>
          </div>
        </AdminSurface>
      )}
    </div>
  );
}
