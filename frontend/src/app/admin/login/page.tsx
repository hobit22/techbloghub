'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { Zap } from 'lucide-react';
import { postsApi } from '@/lib/api/endpoints/posts';
import { adminAuth } from '@/lib/utils/admin-auth';
import { AxiosError } from 'axios';
import { logger } from '@/lib/config';

export default function AdminLoginPage() {
  const [credentials, setCredentials] = useState({ username: '', password: '' });
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');
  const router = useRouter();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    setError('');

    try {
      // Basic Auth 헤더 생성
      const auth = btoa(`${credentials.username}:${credentials.password}`);

      // 임시로 인증 정보 저장 (API 호출을 위해)
      adminAuth.setAuth(auth);

      // 관리자 API 테스트 호출
      await postsApi.getAll({ skip: 0, limit: 1 });

      // 성공하면 대시보드로 이동
      router.push('/admin/dashboard');
    } catch (error) {
      // 실패하면 인증 정보 제거
      adminAuth.removeAuth();

      if (error instanceof AxiosError && error.response?.status === 401) {
        setError('아이디 또는 비밀번호가 잘못되었습니다.');
      } else {
        setError('로그인 중 오류가 발생했습니다.');
      }
      logger.error('Login error:', error);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-50 flex items-center justify-center p-4">
      <div className="w-full max-w-md bg-white rounded-lg shadow-md">
        <div className="p-6 text-center border-b">
          <div className="flex items-center justify-center mb-4">
            <div className="w-12 h-12 bg-gradient-to-br from-blue-500 to-blue-600 rounded-lg flex items-center justify-center">
              <Zap className="h-7 w-7 text-white" />
            </div>
          </div>
          <h1 className="text-2xl font-bold text-gray-900">TechBlogHub 관리자</h1>
          <p className="text-gray-600 mt-2">관리자 계정으로 로그인하세요</p>
        </div>
        <div className="p-6">
          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="space-y-2">
              <label htmlFor="username" className="block text-sm font-medium text-gray-700">
                아이디
              </label>
              <input
                id="username"
                type="text"
                value={credentials.username}
                onChange={(e) => setCredentials(prev => ({ ...prev, username: e.target.value }))}
                placeholder="관리자 아이디를 입력하세요"
                required
                disabled={isLoading}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 disabled:bg-gray-100 
                text-slate-800"
              />
            </div>
            <div className="space-y-2">
              <label htmlFor="password" className="block text-sm font-medium text-gray-700">
                비밀번호
              </label>
              <input
                id="password"
                type="password"
                value={credentials.password}
                onChange={(e) => setCredentials(prev => ({ ...prev, password: e.target.value }))}
                placeholder="비밀번호를 입력하세요"
                required
                disabled={isLoading}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 disabled:bg-gray-100 text-slate-800"
              />
            </div>
            {error && (
              <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-md">
                {error}
              </div>
            )}
            <button
              type="submit"
              disabled={isLoading}
              className="w-full bg-blue-600 text-white py-2 px-4 rounded-md hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 disabled:bg-blue-400 disabled:cursor-not-allowed"
            >
              {isLoading ? '로그인 중...' : '로그인'}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}