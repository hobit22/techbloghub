'use client';

import { useEffect, useState } from 'react';
import { adminBlogApi } from '@/lib/admin-api';
import { Blog } from '@/types';
import { RefreshCw, Globe, Activity, Calendar, AlertCircle, CheckCircle, Plus } from 'lucide-react';
import AddBlogModal from '@/components/admin/AddBlogModal';

export default function AdminBlogsPage() {
  const [blogs, setBlogs] = useState<Blog[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');
  const [triggering, setTriggering] = useState<number | null>(null);
  const [isAddModalOpen, setIsAddModalOpen] = useState(false);

  useEffect(() => {
    loadBlogs();
  }, []);

  const loadBlogs = async () => {
    try {
      setIsLoading(true);
      const response = await adminBlogApi.getAll({ page: 0, size: 100 });
      setBlogs(response.content || []);
      setError('');
    } catch (error) {
      setError('블로그 목록을 불러오는 중 오류가 발생했습니다.');
      console.error('Error loading blogs:', error);
    } finally {
      setIsLoading(false);
    }
  };

  const handleRecrawl = async (blogId: number) => {
    try {
      setTriggering(blogId);
      await adminBlogApi.triggerRecrawl(blogId);
      alert('재크롤링 요청이 성공적으로 전송되었습니다.');
    } catch (error) {
      alert('재크롤링 요청 중 오류가 발생했습니다.');
      console.error('Error triggering recrawl:', error);
    } finally {
      setTriggering(null);
    }
  };

  const handleAllRecrawl = async () => {
    if (!confirm('모든 블로그의 재크롤링을 요청하시겠습니까?')) return;

    try {
      setTriggering(-1); // 전체 재크롤링 표시용
      await adminBlogApi.triggerAllRecrawl();
      alert('전체 재크롤링 요청이 성공적으로 전송되었습니다.');
    } catch (error) {
      alert('전체 재크롤링 요청 중 오류가 발생했습니다.');
      console.error('Error triggering all recrawl:', error);
    } finally {
      setTriggering(null);
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
            onClick={loadBlogs}
            className="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            <RefreshCw className="w-4 h-4 mr-2 inline" />
            새로고침
          </button>
          <button
            onClick={handleAllRecrawl}
            disabled={triggering !== null}
            className="px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-md hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:opacity-50"
          >
            {triggering === -1 ? (
              <>
                <RefreshCw className="w-4 h-4 mr-2 inline animate-spin" />
                전체 재크롤링 중...
              </>
            ) : (
              <>
                <RefreshCw className="w-4 h-4 mr-2 inline" />
                전체 재크롤링
              </>
            )}
          </button>
        </div>
      </div>

      {/* 에러 메시지 */}
      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-md flex items-center">
          <AlertCircle className="w-5 h-5 mr-2" />
          {error}
        </div>
      )}

      {/* 통계 카드 */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div className="bg-white rounded-lg shadow p-6">
          <div className="flex items-center">
            <Globe className="h-8 w-8 text-blue-500" />
            <div className="ml-4">
              <p className="text-sm font-medium text-gray-600">총 블로그</p>
              <p className="text-2xl font-bold text-gray-900">{blogs.length}</p>
            </div>
          </div>
        </div>
        <div className="bg-white rounded-lg shadow p-6">
          <div className="flex items-center">
            <Activity className="h-8 w-8 text-green-500" />
            <div className="ml-4">
              <p className="text-sm font-medium text-gray-600">활성 블로그</p>
              <p className="text-2xl font-bold text-gray-900">
                {blogs.filter(blog => blog.status === 'ACTIVE').length}
              </p>
            </div>
          </div>
        </div>
        <div className="bg-white rounded-lg shadow p-6">
          <div className="flex items-center">
            <CheckCircle className="h-8 w-8 text-purple-500" />
            <div className="ml-4">
              <p className="text-sm font-medium text-gray-600">비활성 블로그</p>
              <p className="text-2xl font-bold text-gray-900">
                {blogs.filter(blog => blog.status === 'INACTIVE').length}
              </p>
            </div>
          </div>
        </div>
      </div>

      {/* 블로그 목록 테이블 */}
      <div className="bg-white rounded-lg shadow overflow-hidden">
        <div className="px-6 py-4 border-b border-gray-200">
          <h2 className="text-lg font-semibold text-gray-900">블로그 목록</h2>
        </div>

        {blogs.length === 0 ? (
          <div className="p-8 text-center">
            <Globe className="w-12 h-12 text-gray-400 mx-auto mb-4" />
            <p className="text-gray-500">등록된 블로그가 없습니다.</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    블로그 정보
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    상태
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    마지막 크롤링
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    작업
                  </th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                {blogs.map((blog) => (
                  <tr key={blog.id} className="hover:bg-gray-50">
                    <td className="px-6 py-4">
                      <div>
                        <div className="text-sm font-medium text-gray-900">
                          {blog.name}
                        </div>
                        <div className="text-sm text-gray-500">
                          {blog.company}
                        </div>
                        <div className="text-xs text-blue-600 hover:text-blue-800">
                          <a href={blog.siteUrl} target="_blank" rel="noopener noreferrer">
                            {blog.siteUrl}
                          </a>
                        </div>
                      </div>
                    </td>
                    <td className="px-6 py-4">
                      <span className={`inline-flex px-2 py-1 text-xs font-semibold rounded-full ${getStatusColor(blog.status || 'UNKNOWN')}`}>
                        {blog.status || 'UNKNOWN'}
                      </span>
                    </td>
                    <td className="px-6 py-4 text-sm text-gray-900">
                      <div className="flex items-center">
                        <Calendar className="w-4 h-4 text-gray-400 mr-2" />
                        {formatDate(blog.lastCrawledAt || '')}
                      </div>
                    </td>
                    <td className="px-6 py-4 text-sm font-medium">
                      <button
                        onClick={() => handleRecrawl(blog.id)}
                        disabled={triggering === blog.id}
                        className="text-blue-600 hover:text-blue-900 disabled:opacity-50 disabled:cursor-not-allowed"
                      >
                        {triggering === blog.id ? (
                          <>
                            <RefreshCw className="w-4 h-4 mr-1 inline animate-spin" />
                            재크롤링 중...
                          </>
                        ) : (
                          <>
                            <RefreshCw className="w-4 h-4 mr-1 inline" />
                            재크롤링
                          </>
                        )}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* 블로그 추가 모달 */}
      <AddBlogModal
        isOpen={isAddModalOpen}
        onClose={() => setIsAddModalOpen(false)}
        onSuccess={loadBlogs}
      />
    </div>
  );
}