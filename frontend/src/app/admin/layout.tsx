'use client';

import { useEffect, useState } from 'react';
import { useRouter, usePathname } from 'next/navigation';
import { useQueryClient } from '@tanstack/react-query';
import { adminAuth } from '@/lib/utils/admin-auth';
import { Zap, LayoutDashboard, FileText, Globe, LogOut, ChevronRight, ShieldCheck } from 'lucide-react';
import Link from 'next/link';
import { clsx } from 'clsx';

export default function AdminLayout({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const pathname = usePathname();
  const queryClient = useQueryClient();
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    if (pathname !== '/admin/login' && !adminAuth.isLoggedIn()) {
      router.push('/admin/login');
    } else {
      setIsLoading(false);
    }
  }, [pathname, router]);

  const handleLogout = () => {
    // React Query 캐시 정리 후 로그아웃
    queryClient.clear();
    adminAuth.logout();
  };

  if (isLoading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-[#020617] text-slate-200">
        <div className="text-center">
          <div className="mx-auto mb-4 h-9 w-9 animate-spin rounded-full border-4 border-cyan-400 border-t-transparent"></div>
          <p className="text-sm text-slate-400">관리 콘솔을 불러오는 중...</p>
        </div>
      </div>
    );
  }

  // 로그인 페이지는 레이아웃 없이 표시
  if (pathname === '/admin/login') {
    return <>{children}</>;
  }

  const menuItems = [
    { href: '/admin/dashboard', label: '대시보드', icon: LayoutDashboard },
    { href: '/admin/posts', label: '포스트 관리', icon: FileText },
    { href: '/admin/blogs', label: '블로그 관리', icon: Globe },
  ];

  return (
    <div className="min-h-screen bg-[radial-gradient(circle_at_top,_rgba(34,211,238,0.12),_transparent_30%),linear-gradient(180deg,#020617_0%,#0f172a_45%,#111827_100%)] text-slate-100">
      <div className="fixed inset-y-0 left-0 z-50 hidden w-72 border-r border-white/10 bg-slate-950/80 backdrop-blur xl:block">
        <div className="flex h-full flex-col px-5 py-6">
          <div className="rounded-3xl border border-white/10 bg-white/5 p-5">
            <div className="flex items-center gap-3">
              <div className="flex h-11 w-11 items-center justify-center rounded-2xl bg-cyan-400 text-slate-950">
                <Zap className="h-5 w-5" />
              </div>
              <div>
                <h1 className="text-lg font-semibold text-white">TechBlogHub</h1>
                <p className="text-xs uppercase tracking-[0.22em] text-cyan-300/70">Admin Control</p>
              </div>
            </div>
            <div className="mt-5 rounded-2xl border border-white/10 bg-slate-950/70 p-4">
              <div className="flex items-center gap-2 text-sm font-medium text-white">
                <ShieldCheck className="h-4 w-4 text-emerald-300" />
                관리자 인증 활성화
              </div>
              <p className="mt-2 text-sm leading-6 text-slate-400">
                RSS 수집, 본문 처리, 소스 추가를 한 화면에서 제어하는 운영 콘솔입니다.
              </p>
            </div>
          </div>

          <nav className="mt-6 flex-1">
            <div className="px-3 pb-2 text-xs font-semibold uppercase tracking-[0.2em] text-slate-500">Navigation</div>
            <ul className="space-y-2">
            {menuItems.map((item) => {
              const isActive = pathname === item.href;
              const Icon = item.icon;
              return (
                <li key={item.href}>
                  <Link
                    href={item.href}
                    className={clsx(
                      'group flex items-center justify-between rounded-2xl border px-4 py-3 text-sm font-medium transition',
                      isActive
                        ? 'border-cyan-400/30 bg-cyan-400/10 text-white shadow-[0_12px_40px_-24px_rgba(34,211,238,0.85)]'
                        : 'border-white/5 bg-white/5 text-slate-300 hover:border-white/10 hover:bg-white/10 hover:text-white'
                    )}
                  >
                    <span className="flex items-center gap-3">
                      <span className={clsx('rounded-xl p-2', isActive ? 'bg-cyan-300/20 text-cyan-200' : 'bg-slate-900 text-slate-400 group-hover:text-slate-200')}>
                        <Icon className="h-4 w-4" />
                      </span>
                      {item.label}
                    </span>
                    <ChevronRight className={clsx('h-4 w-4 transition', isActive ? 'text-cyan-200' : 'text-slate-600 group-hover:text-slate-300')} />
                  </Link>
                </li>
              );
            })}
          </ul>
          </nav>

          <button
            onClick={handleLogout}
            className="mt-6 flex w-full items-center justify-center gap-2 rounded-2xl border border-white/10 bg-white/5 px-4 py-3 text-sm font-medium text-slate-300 transition hover:bg-white/10 hover:text-white"
          >
            <LogOut className="h-4 w-4" />
            로그아웃
          </button>
        </div>
      </div>

      <div className="xl:pl-72">
        <main className="min-h-screen px-4 py-6 sm:px-6 xl:px-8">
          {children}
        </main>
      </div>
    </div>
  );
}
