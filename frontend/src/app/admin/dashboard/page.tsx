'use client';

import { useEffect, useState } from 'react';
import { adminSchedulerApi } from '@/lib/admin-api';
import { FileText, Globe, TrendingUp, Clock, Users, Activity, Play, RotateCcw, Zap } from 'lucide-react';
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
  const [schedulerLoading, setSchedulerLoading] = useState<string | null>(null);

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

  const handleRSSCollect = async () => {
    if (!confirm('모든 활성 블로그의 RSS를 수집하시겠습니까?')) return;

    try {
      setSchedulerLoading('rss');
      const result = await adminSchedulerApi.collectAllRSS();
      alert(`RSS 수집 완료!\n- 처리된 블로그: ${result.summary.blogs_processed}개\n- 새 포스트: ${result.summary.new_posts}개\n- 중복 스킵: ${result.summary.skipped_duplicates}개`);
      loadDashboardData(); // 통계 새로고침
    } catch (error) {
      alert('RSS 수집 중 오류가 발생했습니다.');
      console.error('Error collecting RSS:', error);
    } finally {
      setSchedulerLoading(null);
    }
  };

  const handleContentProcess = async () => {
    if (!confirm('대기 중인 포스트의 본문 추출을 시작하시겠습니까?\n(최대 50개 처리)')) return;

    try {
      setSchedulerLoading('content');
      const result = await adminSchedulerApi.processContent(50);
      alert(`본문 추출 완료!\n- 처리된 포스트: ${result.summary.total_processed}개\n- 성공: ${result.summary.completed}개\n- 실패: ${result.summary.failed}개`);
      loadDashboardData(); // 통계 새로고침
    } catch (error) {
      alert('본문 추출 중 오류가 발생했습니다.');
      console.error('Error processing content:', error);
    } finally {
      setSchedulerLoading(null);
    }
  };

  const handleRetryFailed = async () => {
    if (!confirm('실패한 포스트를 재시도하시겠습니까?\n(최대 10개 처리)')) return;

    try {
      setSchedulerLoading('retry');
      const result = await adminSchedulerApi.retryFailed(10);
      alert(`재시도 완료!\n- 처리된 포스트: ${result.summary.total_processed}개\n- 성공: ${result.summary.completed}개\n- 실패: ${result.summary.failed}개`);
      loadDashboardData(); // 통계 새로고침
    } catch (error) {
      alert('재시도 중 오류가 발생했습니다.');
      console.error('Error retrying failed posts:', error);
    } finally {
      setSchedulerLoading(null);
    }
  };

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

      {/* 스케줄러 관리 */}
      <div className="bg-white rounded-lg shadow p-6">
        <h2 className="text-lg font-semibold text-gray-900 mb-4 flex items-center">
          <Zap className="w-5 h-5 mr-2 text-orange-500" />
          스케줄러 관리
        </h2>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <button
            onClick={handleRSSCollect}
            disabled={schedulerLoading !== null}
            className="p-4 border-2 border-blue-200 rounded-lg hover:bg-blue-50 transition-colors text-left disabled:opacity-50 disabled:cursor-not-allowed"
          >
            <div className="flex items-center mb-2">
              {schedulerLoading === 'rss' ? (
                <div className="w-6 h-6 border-2 border-blue-600 border-t-transparent rounded-full animate-spin mr-2"></div>
              ) : (
                <Play className="h-6 w-6 text-blue-600 mr-2" />
              )}
              <h3 className="font-medium text-gray-900">RSS 수집</h3>
            </div>
            <p className="text-sm text-gray-600">모든 활성 블로그의 RSS 피드를 수집합니다</p>
            {stats.activeBlogsCount > 0 && (
              <p className="text-xs text-blue-600 mt-2">활성 블로그: {stats.activeBlogsCount}개</p>
            )}
          </button>

          <button
            onClick={handleContentProcess}
            disabled={schedulerLoading !== null}
            className="p-4 border-2 border-green-200 rounded-lg hover:bg-green-50 transition-colors text-left disabled:opacity-50 disabled:cursor-not-allowed"
          >
            <div className="flex items-center mb-2">
              {schedulerLoading === 'content' ? (
                <div className="w-6 h-6 border-2 border-green-600 border-t-transparent rounded-full animate-spin mr-2"></div>
              ) : (
                <FileText className="h-6 w-6 text-green-600 mr-2" />
              )}
              <h3 className="font-medium text-gray-900">본문 추출</h3>
            </div>
            <p className="text-sm text-gray-600">대기 중인 포스트의 본문을 추출합니다</p>
            {stats.pendingPosts > 0 && (
              <p className="text-xs text-green-600 mt-2">대기 포스트: {stats.pendingPosts}개</p>
            )}
          </button>

          <button
            onClick={handleRetryFailed}
            disabled={schedulerLoading !== null}
            className="p-4 border-2 border-orange-200 rounded-lg hover:bg-orange-50 transition-colors text-left disabled:opacity-50 disabled:cursor-not-allowed"
          >
            <div className="flex items-center mb-2">
              {schedulerLoading === 'retry' ? (
                <div className="w-6 h-6 border-2 border-orange-600 border-t-transparent rounded-full animate-spin mr-2"></div>
              ) : (
                <RotateCcw className="h-6 w-6 text-orange-600 mr-2" />
              )}
              <h3 className="font-medium text-gray-900">실패 재시도</h3>
            </div>
            <p className="text-sm text-gray-600">실패한 포스트를 다시 처리합니다</p>
            {stats.failedPosts > 0 && (
              <p className="text-xs text-orange-600 mt-2">실패 포스트: {stats.failedPosts}개</p>
            )}
          </button>
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