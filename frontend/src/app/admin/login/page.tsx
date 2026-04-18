'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { ArrowRight, ShieldCheck, Sparkles, Zap } from 'lucide-react';
import { adminApi } from '@/lib/api/endpoints/admin';
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
      await adminApi.getSchedulerStats();

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
    <div className="min-h-screen bg-[radial-gradient(circle_at_top,_rgba(34,211,238,0.18),_transparent_28%),linear-gradient(180deg,#020617_0%,#0f172a_55%,#111827_100%)] p-4 text-white">
      <div className="mx-auto grid min-h-[calc(100vh-2rem)] w-full max-w-6xl items-center gap-8 lg:grid-cols-[1.05fr_0.95fr]">
        <div className="hidden rounded-[2rem] border border-white/10 bg-white/5 p-10 backdrop-blur lg:block">
          <div className="inline-flex items-center gap-2 rounded-full border border-cyan-400/20 bg-cyan-400/10 px-4 py-2 text-sm font-medium text-cyan-200">
            <Sparkles className="h-4 w-4" />
            TechBlogHub Admin Control Room
          </div>
          <div className="mt-8 max-w-xl space-y-5">
            <h1 className="text-5xl font-semibold leading-tight tracking-tight">
              수집, 요약, 운영 상태를 한 번에 관리하는 관리자 콘솔
            </h1>
            <p className="text-base leading-7 text-slate-300">
              TechBlogHub의 RSS 수집, 본문 처리, 소스 확장 작업을 실시간 운영 흐름에 맞춰 다루는 내부 인터페이스입니다.
            </p>
          </div>
          <div className="mt-10 grid gap-4 sm:grid-cols-2">
            <div className="rounded-3xl border border-white/10 bg-slate-950/50 p-5">
              <ShieldCheck className="h-5 w-5 text-emerald-300" />
              <h2 className="mt-4 text-lg font-semibold">보호된 운영 경로</h2>
              <p className="mt-2 text-sm leading-6 text-slate-400">관리자 인증 후에만 블로그 소스와 스케줄러 작업을 제어합니다.</p>
            </div>
            <div className="rounded-3xl border border-white/10 bg-slate-950/50 p-5">
              <Zap className="h-5 w-5 text-cyan-300" />
              <h2 className="mt-4 text-lg font-semibold">즉시 실행 중심</h2>
              <p className="mt-2 text-sm leading-6 text-slate-400">RSS 수집, 본문 처리, 재시도, 검증 소스 추가까지 즉시 실행할 수 있습니다.</p>
            </div>
          </div>
        </div>

        <div className="w-full max-w-xl justify-self-center rounded-[2rem] border border-white/10 bg-slate-950/70 p-8 shadow-[0_40px_120px_-50px_rgba(8,47,73,0.85)] backdrop-blur">
          <div className="mb-8 space-y-4">
            <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-cyan-400 text-slate-950">
              <Zap className="h-7 w-7" />
            </div>
            <div>
              <p className="text-xs font-semibold uppercase tracking-[0.22em] text-cyan-300/80">Admin Sign In</p>
              <h1 className="mt-2 text-3xl font-semibold tracking-tight text-white">관리자 계정으로 로그인</h1>
              <p className="mt-3 text-sm leading-6 text-slate-400">현재 운영 중인 블로그 수집 파이프라인과 관리 화면에 접근합니다.</p>
            </div>
          </div>

          <form onSubmit={handleSubmit} className="space-y-5">
            <div className="space-y-2">
              <label htmlFor="username" className="block text-sm font-medium text-slate-200">
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
                className="w-full rounded-2xl border border-white/10 bg-white/5 px-4 py-3 text-slate-100 placeholder:text-slate-500 focus:border-cyan-400/40 focus:outline-none focus:ring-2 focus:ring-cyan-400/20 disabled:cursor-not-allowed disabled:opacity-50"
              />
            </div>
            <div className="space-y-2">
              <label htmlFor="password" className="block text-sm font-medium text-slate-200">
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
                className="w-full rounded-2xl border border-white/10 bg-white/5 px-4 py-3 text-slate-100 placeholder:text-slate-500 focus:border-cyan-400/40 focus:outline-none focus:ring-2 focus:ring-cyan-400/20 disabled:cursor-not-allowed disabled:opacity-50"
              />
            </div>
            {error && (
              <div className="rounded-2xl border border-rose-400/20 bg-rose-500/10 px-4 py-3 text-sm text-rose-100">
                {error}
              </div>
            )}
            <button
              type="submit"
              disabled={isLoading}
              className="flex w-full items-center justify-center gap-2 rounded-2xl bg-cyan-400 px-4 py-3 font-semibold text-slate-950 transition hover:bg-cyan-300 focus:outline-none focus:ring-2 focus:ring-cyan-300/40 disabled:cursor-not-allowed disabled:opacity-60"
            >
              {isLoading ? '로그인 중...' : '관리 콘솔 입장'}
              {!isLoading && <ArrowRight className="h-4 w-4" />}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}
