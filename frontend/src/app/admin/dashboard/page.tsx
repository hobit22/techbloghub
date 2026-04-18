'use client';

import { FileText, Globe, TrendingUp, Clock, Activity, Play, RotateCcw, ArrowRight, CircleAlert } from 'lucide-react';
import Link from 'next/link';
import { useSchedulerStats, useCollectAllRSS, useProcessContent, useRetryFailed } from '@/lib/hooks/use-admin';
import { PostStatus, BlogStatus } from '@/types';
import { logger } from '@/lib/config';
import { AdminBadge, AdminPageHeader, AdminStatCard, AdminSurface } from '@/components/admin/admin-ui';

export default function AdminDashboard() {
  // Use React Query hooks
  const { data: statsData, isLoading } = useSchedulerStats();
  const collectRSSMutation = useCollectAllRSS();
  const processContentMutation = useProcessContent();
  const retryFailedMutation = useRetryFailed();

  // Calculate totals from all statuses
  const totalPosts = statsData?.post_stats
    ? Object.values(statsData.post_stats).reduce((sum: number, val) => sum + (typeof val === 'number' ? val : 0), 0)
    : 0;

  const totalBlogs = statsData?.blog_stats
    ? Object.values(statsData.blog_stats).reduce((sum: number, val) => sum + (typeof val === 'number' ? val : 0), 0)
    : 0;

  const stats = {
    totalPosts,
    totalBlogs,
    activeBlogsCount: statsData?.blog_stats?.[BlogStatus.ACTIVE] || 0,
    pendingPosts: statsData?.post_stats?.[PostStatus.PENDING] || 0,
    failedPosts: statsData?.post_stats?.[PostStatus.FAILED] || 0,
  };

  const handleRSSCollect = async () => {
    if (!confirm('모든 활성 블로그의 RSS를 수집하시겠습니까?')) return;

    try {
      const result = await collectRSSMutation.mutateAsync();
      alert(`RSS 수집 완료!\n- 처리된 블로그: ${result.summary.blogs_processed}개\n- 새 포스트: ${result.summary.new_posts}개\n- 중복 스킵: ${result.summary.skipped_duplicates}개`);
    } catch (error) {
      alert('RSS 수집 중 오류가 발생했습니다.');
      logger.error('Error collecting RSS:', error);
    }
  };

  const handleContentProcess = async () => {
    if (!confirm('대기 중인 포스트의 본문 추출을 시작하시겠습니까?\n(최대 50개 처리)')) return;

    try {
      const result = await processContentMutation.mutateAsync(50);
      alert(`본문 추출 완료!\n- 처리된 포스트: ${result.summary.total_processed}개\n- 성공: ${result.summary.completed}개\n- 실패: ${result.summary.failed}개`);
    } catch (error) {
      alert('본문 추출 중 오류가 발생했습니다.');
      logger.error('Error processing content:', error);
    }
  };

  const handleRetryFailed = async () => {
    if (!confirm('실패한 포스트를 재시도하시겠습니까?\n(최대 10개 처리)')) return;

    try {
      const result = await retryFailedMutation.mutateAsync(10);
      alert(`재시도 완료!\n- 처리된 포스트: ${result.summary.total_processed}개\n- 성공: ${result.summary.completed}개\n- 실패: ${result.summary.failed}개`);
    } catch (error) {
      alert('재시도 중 오류가 발생했습니다.');
      logger.error('Error retrying failed posts:', error);
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
        <AdminPageHeader title="운영 대시보드" description="스케줄러와 처리 상태를 불러오는 중입니다." />
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-5">
          {[1, 2, 3, 4].map((i) => (
            <AdminSurface key={i} className="animate-pulse p-6">
              <div className="h-4 w-24 rounded bg-white/10"></div>
              <div className="mt-4 h-10 w-20 rounded bg-white/10"></div>
              <div className="mt-4 h-3 w-full rounded bg-white/10"></div>
            </AdminSurface>
          ))}
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <AdminPageHeader
        eyebrow="Operations"
        title="운영 대시보드"
        description="수집 파이프라인 상태, 블로그 운영 현황, 처리 대기열을 한 화면에서 보고 즉시 작업을 실행합니다."
        actions={<AdminBadge>마지막 확인 {new Date().toLocaleTimeString('ko-KR')}</AdminBadge>}
      />

      <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-5">
        {statCards.map((card, index) => {
          const Icon = card.icon;
          const tone = card.title === '실패 포스트' ? 'danger' : card.title === '대기중 포스트' ? 'warning' : card.title === '활성 블로그' ? 'success' : 'default';
          return (
            <AdminStatCard
              key={index}
              label={card.title}
              value={card.value.toLocaleString()}
              description={card.description}
              tone={tone}
              icon={<Icon className="h-5 w-5" />}
            />
          );
        })}
      </div>

      <div className="grid gap-6 xl:grid-cols-[1.15fr_0.85fr]">
        <AdminSurface className="p-6">
          <div className="mb-6 flex items-start justify-between gap-4">
            <div>
              <p className="text-xs font-semibold uppercase tracking-[0.2em] text-cyan-300/75">Automation</p>
              <h2 className="mt-2 text-2xl font-semibold text-white">스케줄러 실행 센터</h2>
              <p className="mt-2 text-sm leading-6 text-slate-400">크롤링과 본문 처리 작업을 운영 상황에 맞춰 수동으로 실행합니다.</p>
            </div>
            <AdminBadge tone={stats.failedPosts > 0 ? 'warning' : 'success'}>
              실패 {stats.failedPosts.toLocaleString()}건
            </AdminBadge>
          </div>
          <div className="grid gap-4 md:grid-cols-3">
          <button
            onClick={handleRSSCollect}
            disabled={collectRSSMutation.isPending || processContentMutation.isPending || retryFailedMutation.isPending}
            className="rounded-3xl border border-cyan-400/20 bg-cyan-400/10 p-5 text-left transition hover:bg-cyan-400/15 disabled:cursor-not-allowed disabled:opacity-50"
          >
            <div className="flex items-center mb-2">
              {collectRSSMutation.isPending ? (
                <div className="mr-2 h-6 w-6 animate-spin rounded-full border-2 border-cyan-200 border-t-transparent"></div>
              ) : (
                <Play className="mr-2 h-6 w-6 text-cyan-200" />
              )}
              <h3 className="font-medium text-white">RSS 수집</h3>
            </div>
            <p className="text-sm text-slate-300">모든 활성 블로그의 RSS 피드를 수집합니다.</p>
            {stats.activeBlogsCount > 0 && (
              <p className="mt-3 text-xs font-medium text-cyan-100">활성 블로그 {stats.activeBlogsCount}개</p>
            )}
          </button>

          <button
            onClick={handleContentProcess}
            disabled={collectRSSMutation.isPending || processContentMutation.isPending || retryFailedMutation.isPending}
            className="rounded-3xl border border-emerald-400/20 bg-emerald-400/10 p-5 text-left transition hover:bg-emerald-400/15 disabled:cursor-not-allowed disabled:opacity-50"
          >
            <div className="flex items-center mb-2">
              {processContentMutation.isPending ? (
                <div className="mr-2 h-6 w-6 animate-spin rounded-full border-2 border-emerald-200 border-t-transparent"></div>
              ) : (
                <FileText className="mr-2 h-6 w-6 text-emerald-200" />
              )}
              <h3 className="font-medium text-white">본문 추출</h3>
            </div>
            <p className="text-sm text-slate-300">대기 중인 포스트의 본문을 추출합니다.</p>
            {stats.pendingPosts > 0 && (
              <p className="mt-3 text-xs font-medium text-emerald-100">대기 포스트 {stats.pendingPosts}개</p>
            )}
          </button>

          <button
            onClick={handleRetryFailed}
            disabled={collectRSSMutation.isPending || processContentMutation.isPending || retryFailedMutation.isPending}
            className="rounded-3xl border border-amber-400/20 bg-amber-400/10 p-5 text-left transition hover:bg-amber-400/15 disabled:cursor-not-allowed disabled:opacity-50"
          >
            <div className="flex items-center mb-2">
              {retryFailedMutation.isPending ? (
                <div className="mr-2 h-6 w-6 animate-spin rounded-full border-2 border-amber-100 border-t-transparent"></div>
              ) : (
                <RotateCcw className="mr-2 h-6 w-6 text-amber-100" />
              )}
              <h3 className="font-medium text-white">실패 재시도</h3>
            </div>
            <p className="text-sm text-slate-300">실패한 포스트를 다시 처리합니다.</p>
            {stats.failedPosts > 0 && (
              <p className="mt-3 text-xs font-medium text-amber-50">실패 포스트 {stats.failedPosts}개</p>
            )}
          </button>
          </div>

          <div className="mt-6 rounded-3xl border border-white/10 bg-white/5 p-4 text-sm text-slate-300">
            작업은 현재 페이지를 벗어나지 않고 즉시 실행되며, 완료 후 최신 상태를 다시 불러옵니다.
          </div>
        </AdminSurface>

        <div className="space-y-6">
          <AdminSurface className="p-6">
            <div className="mb-5 flex items-center justify-between">
              <div>
                <p className="text-xs font-semibold uppercase tracking-[0.2em] text-cyan-300/75">Health</p>
                <h2 className="mt-2 text-2xl font-semibold text-white">시스템 상태</h2>
              </div>
              <AdminBadge tone="success">정상 동작</AdminBadge>
            </div>
            <div className="space-y-3">
              {[
                ['크롤링 시스템', '정상 동작'],
                ['데이터베이스', '연결됨'],
                ['API 서버', '온라인'],
              ].map(([label, value]) => (
                <div key={label} className="flex items-center justify-between rounded-2xl border border-white/10 bg-white/5 px-4 py-3">
                  <div className="flex items-center gap-3 text-sm text-slate-300">
                    <span className="h-2.5 w-2.5 rounded-full bg-emerald-400" />
                    {label}
                  </div>
                  <span className="text-sm font-medium text-emerald-200">{value}</span>
                </div>
              ))}
            </div>
          </AdminSurface>

          <AdminSurface className="p-6">
            <div className="mb-5 flex items-center gap-2 text-white">
              <CircleAlert className="h-5 w-5 text-cyan-200" />
              <h2 className="text-xl font-semibold">빠른 이동</h2>
            </div>
            <div className="grid gap-3">
          <Link
            href="/admin/posts"
            className="group rounded-3xl border border-white/10 bg-white/5 p-4 transition hover:bg-white/10"
          >
            <FileText className="mb-3 h-6 w-6 text-cyan-200" />
            <h3 className="font-medium text-white">포스트 관리</h3>
            <p className="mt-1 text-sm text-slate-400">포스트 대기열과 원문 처리 상태를 관리합니다.</p>
            <span className="mt-4 inline-flex items-center gap-1 text-sm font-medium text-cyan-200">바로 이동 <ArrowRight className="h-4 w-4" /></span>
          </Link>
          <Link
            href="/admin/blogs"
            className="group rounded-3xl border border-white/10 bg-white/5 p-4 transition hover:bg-white/10"
          >
            <Globe className="mb-3 h-6 w-6 text-emerald-200" />
            <h3 className="font-medium text-white">블로그 관리</h3>
            <p className="mt-1 text-sm text-slate-400">소스 추가, 검증 소스 가져오기, 단일 재수집을 제어합니다.</p>
            <span className="mt-4 inline-flex items-center gap-1 text-sm font-medium text-emerald-200">바로 이동 <ArrowRight className="h-4 w-4" /></span>
          </Link>
            </div>
          </AdminSurface>
        </div>
      </div>
    </div>
  );
}
