'use client';

import { useEffect, useState } from 'react';
import { adminSchedulerApi } from '@/lib/admin-api';
import { FileText, Globe, TrendingUp, Clock, Users, Activity } from 'lucide-react';
import Link from 'next/link';

interface DashboardStats {
  totalPosts: number;
  totalBlogs: number;
  activeBlogsCount: number;
  pendingPosts: number;
  failedPosts: number;
}

export default function AdminDashboard() {
  const [stats, setStats] = useState<DashboardStats>({
    totalPosts: 0,
    totalBlogs: 0,
    activeBlogsCount: 0,
    pendingPosts: 0,
    failedPosts: 0,
  });
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const loadDashboardData = async () => {
      try {
        // Scheduler Stats API 사용
        const data = await adminSchedulerApi.getStats();

        const newStats: DashboardStats = {
          totalPosts: data.post_stats.total || 0,
          totalBlogs: data.blog_stats.total || 0,
          activeBlogsCount: data.blog_stats.active || 0,
          pendingPosts: data.post_stats.pending || 0,
          failedPosts: data.post_stats.failed || 0,
        };

        setStats(newStats);
      } catch (error) {
        console.error('Dashboard data loading error:', error);
        // 에러 발생시 기본값 유지
      } finally {
        setIsLoading(false);
      }
    };

    loadDashboardData();
  }, []);

  const statCards = [
    {
      title: '총 포스트',
      value: stats.totalPosts,
      icon: FileText,
      color: 'bg-blue-500',
      description: '전체 등록된 포스트 수',
    },
    {
      title: '총 블로그',
      value: stats.totalBlogs,
      icon: Globe,
      color: 'bg-green-500',
      description: '등록된 블로그 수',
    },
    {
      title: '활성 블로그',
      value: stats.activeBlogsCount,
      icon: Activity,
      color: 'bg-purple-500',
      description: '현재 크롤링 중인 블로그',
    },
    {
      title: '대기중 포스트',
      value: stats.pendingPosts,
      icon: Clock,
      color: 'bg-orange-500',
      description: '콘텐츠 처리 대기중',
    },
    {
      title: '실패 포스트',
      value: stats.failedPosts,
      icon: TrendingUp,
      color: 'bg-red-500',
      description: '콘텐츠 처리 실패',
    },
  ];

  if (isLoading) {
    return (
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <h1 className="text-2xl font-bold text-gray-900">대시보드</h1>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
          {[1, 2, 3, 4].map((i) => (
            <div key={i} className="bg-white rounded-lg shadow p-6 animate-pulse">
              <div className="h-4 bg-gray-200 rounded w-3/4 mb-4"></div>
              <div className="h-8 bg-gray-200 rounded w-1/2 mb-2"></div>
              <div className="h-3 bg-gray-200 rounded w-full"></div>
            </div>
          ))}
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* 헤더 */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">대시보드</h1>
          <p className="text-gray-600 mt-1">TechBlogHub 관리자 대시보드</p>
        </div>
        <div className="text-sm text-gray-500">
          마지막 업데이트: {new Date().toLocaleString('ko-KR')}
        </div>
      </div>

      {/* 통계 카드 */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        {statCards.map((card, index) => {
          const Icon = card.icon;
          return (
            <div key={index} className="bg-white rounded-lg shadow hover:shadow-md transition-shadow p-6">
              <div className="flex items-center">
                <div className={`p-3 rounded-lg ${card.color}`}>
                  <Icon className="h-6 w-6 text-white" />
                </div>
                <div className="ml-4 flex-1">
                  <p className="text-sm font-medium text-gray-600">{card.title}</p>
                  <p className="text-2xl font-bold text-gray-900">
                    {card.value.toLocaleString()}
                  </p>
                </div>
              </div>
              <p className="text-xs text-gray-500 mt-4">{card.description}</p>
            </div>
          );
        })}
      </div>

      {/* 최근 활동 */}
      <div className="bg-white rounded-lg shadow p-6">
        <h2 className="text-lg font-semibold text-gray-900 mb-4">시스템 상태</h2>
        <div className="space-y-4">
          <div className="flex items-center justify-between py-3 border-b border-gray-100">
            <div className="flex items-center">
              <div className="w-3 h-3 bg-green-500 rounded-full mr-3"></div>
              <span className="text-sm text-gray-700">크롤링 시스템</span>
            </div>
            <span className="text-sm font-medium text-green-600">정상 동작</span>
          </div>
          <div className="flex items-center justify-between py-3 border-b border-gray-100">
            <div className="flex items-center">
              <div className="w-3 h-3 bg-green-500 rounded-full mr-3"></div>
              <span className="text-sm text-gray-700">데이터베이스</span>
            </div>
            <span className="text-sm font-medium text-green-600">연결됨</span>
          </div>
          <div className="flex items-center justify-between py-3">
            <div className="flex items-center">
              <div className="w-3 h-3 bg-green-500 rounded-full mr-3"></div>
              <span className="text-sm text-gray-700">API 서버</span>
            </div>
            <span className="text-sm font-medium text-green-600">온라인</span>
          </div>
        </div>
      </div>

      {/* 빠른 작업 */}
      <div className="bg-white rounded-lg shadow p-6">
        <h2 className="text-lg font-semibold text-gray-900 mb-4">빠른 작업</h2>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <Link
            href="/admin/posts"
            className="p-4 border border-gray-200 rounded-lg hover:bg-gray-50 transition-colors text-left block"
          >
            <FileText className="h-6 w-6 text-blue-500 mb-2" />
            <h3 className="font-medium text-gray-900">포스트 관리</h3>
            <p className="text-sm text-gray-600">포스트 목록 보기 및 관리</p>
          </Link>
          <Link
            href="/admin/blogs"
            className="p-4 border border-gray-200 rounded-lg hover:bg-gray-50 transition-colors text-left block"
          >
            <Globe className="h-6 w-6 text-green-500 mb-2" />
            <h3 className="font-medium text-gray-900">블로그 관리</h3>
            <p className="text-sm text-gray-600">블로그 설정 및 크롤링 관리</p>
          </Link>
          <div className="p-4 border border-gray-200 rounded-lg bg-gray-50 text-left opacity-50">
            <TrendingUp className="h-6 w-6 text-gray-400 mb-2" />
            <h3 className="font-medium text-gray-600">통계 보기</h3>
            <p className="text-sm text-gray-500">준비 중...</p>
          </div>
        </div>
      </div>
    </div>
  );
}