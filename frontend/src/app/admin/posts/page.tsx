'use client';

import { useState } from 'react';
import { Search, Filter, RefreshCw, FileText, Calendar, User, ExternalLink, Trash2, Download } from 'lucide-react';
import { usePosts, useSearchPosts, useDeletePost } from '@/lib/hooks/use-posts';
import { useProcessPost } from '@/lib/hooks/use-admin';
import { logger } from '@/lib/config';

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
    }
  );

  const {
    data: listData,
    isLoading: listLoading,
    refetch: refetchList,
  } = usePosts({
    skip: currentPage * pageSize,
    limit: pageSize,
    ...(filters.blogId && { blog_id: parseInt(filters.blogId) }),
  });

  const deletePostMutation = useDeletePost();
  const processPostMutation = useProcessPost();

  const isLoading = isSearchMode ? searchLoading : listLoading;
  const posts = isSearchMode ? (searchData?.results || []) : (listData?.posts || []);
  const totalElements = isSearchMode ? (searchData?.total || 0) : (listData?.total || 0);

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

      if (result.summary.completed > 0) {
        alert('본문 추출이 완료되었습니다!');
        if (isSearchMode) {
          refetchSearch();
        } else {
          refetchList();
        }
      } else {
        alert(`본문 추출 실패:\n${result.summary.errors[0]?.error || '알 수 없는 오류'}`);
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
        <div className="flex items-center justify-between">
          <h1 className="text-2xl font-bold text-gray-900">포스트 관리</h1>
        </div>
        <div className="bg-white rounded-lg shadow p-8 text-center">
          <div className="w-8 h-8 border-4 border-blue-600 border-t-transparent rounded-full animate-spin mx-auto mb-4"></div>
          <p className="text-gray-600">포스트 목록을 불러오는 중...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">포스트 관리</h1>
          <p className="text-gray-600 mt-1">
            총 {totalElements.toLocaleString()}개의 포스트
          </p>
        </div>
        <button
          onClick={() => setShowFilters(!showFilters)}
          className="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50"
        >
          <Filter className="w-4 h-4 mr-2 inline" />
          필터
        </button>
      </div>

      {/* Filters */}
      {showFilters && (
        <div className="bg-white rounded-lg shadow p-4">
          <form onSubmit={handleSearch} className="space-y-4">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  검색어
                </label>
                <input
                  type="text"
                  value={filters.keyword}
                  onChange={(e) => handleFilterChange('keyword', e.target.value)}
                  placeholder="제목으로 검색..."
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  블로그 ID
                </label>
                <input
                  type="number"
                  value={filters.blogId}
                  onChange={(e) => handleFilterChange('blogId', e.target.value)}
                  placeholder="블로그 ID"
                  className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>
            </div>
            <div className="flex space-x-2">
              <button
                type="submit"
                className="px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-md hover:bg-blue-700"
              >
                <Search className="w-4 h-4 mr-2 inline" />
                검색
              </button>
              <button
                type="button"
                onClick={handleReset}
                className="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50"
              >
                <RefreshCw className="w-4 h-4 mr-2 inline" />
                초기화
              </button>
            </div>
          </form>
        </div>
      )}

      {/* Posts Table */}
      <div className="bg-white rounded-lg shadow overflow-hidden">
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                  제목
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                  블로그
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                  작성자
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">
                  발행일
                </th>
                <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase">
                  작업
                </th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {posts.map((post) => (
                <tr key={post.id} className="hover:bg-gray-50">
                  <td className="px-6 py-4">
                    <div className="flex items-center">
                      <FileText className="w-5 h-5 text-gray-400 mr-2" />
                      <div className="max-w-md">
                        <div className="text-sm font-medium text-gray-900 truncate">
                          {post.title}
                        </div>
                        <a
                          href={post.original_url}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="text-sm text-blue-600 hover:text-blue-800 flex items-center mt-1"
                        >
                          <ExternalLink className="w-3 h-3 mr-1" />
                          원문 보기
                        </a>
                      </div>
                    </div>
                  </td>
                  <td className="px-6 py-4">
                    <div className="text-sm text-gray-900">{post.blog?.name || '-'}</div>
                    <div className="text-sm text-gray-500">{post.blog?.company || '-'}</div>
                  </td>
                  <td className="px-6 py-4">
                    <div className="flex items-center text-sm text-gray-900">
                      <User className="w-4 h-4 mr-1 text-gray-400" />
                      {post.author || '-'}
                    </div>
                  </td>
                  <td className="px-6 py-4">
                    <div className="flex items-center text-sm text-gray-500">
                      <Calendar className="w-4 h-4 mr-1" />
                      {formatDate(post.published_at)}
                    </div>
                  </td>
                  <td className="px-6 py-4 text-right text-sm font-medium space-x-2">
                    <button
                      onClick={() => handleProcessPost(post.id, post.title)}
                      disabled={processingPostId === post.id}
                      className="text-purple-600 hover:text-purple-900 disabled:opacity-50"
                    >
                      <Download
                        className={`w-4 h-4 inline ${
                          processingPostId === post.id ? 'animate-spin' : ''
                        }`}
                      />
                    </button>
                    <button
                      onClick={() => handleDeletePost(post.id, post.title)}
                      className="text-red-600 hover:text-red-900"
                    >
                      <Trash2 className="w-4 h-4 inline" />
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="flex items-center justify-between bg-white px-4 py-3 rounded-lg shadow">
          <div className="text-sm text-gray-700">
            페이지 {currentPage + 1} / {totalPages}
          </div>
          <div className="flex space-x-2">
            <button
              onClick={() => setCurrentPage((p) => Math.max(0, p - 1))}
              disabled={currentPage === 0}
              className="px-3 py-1 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50 disabled:opacity-50"
            >
              이전
            </button>
            <button
              onClick={() => setCurrentPage((p) => Math.min(totalPages - 1, p + 1))}
              disabled={currentPage >= totalPages - 1}
              className="px-3 py-1 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50 disabled:opacity-50"
            >
              다음
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
