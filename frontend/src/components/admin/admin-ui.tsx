'use client';

import type { ReactNode } from 'react';
import { clsx } from 'clsx';

export function AdminPageHeader({
  eyebrow,
  title,
  description,
  actions,
}: {
  eyebrow?: string;
  title: string;
  description?: string;
  actions?: ReactNode;
}) {
  return (
    <div className="flex flex-col gap-5 border-b border-white/10 pb-6 lg:flex-row lg:items-end lg:justify-between">
      <div className="space-y-2">
        {eyebrow && (
          <p className="text-xs font-semibold uppercase tracking-[0.24em] text-cyan-300/80">
            {eyebrow}
          </p>
        )}
        <div className="space-y-2">
          <h1 className="text-3xl font-semibold tracking-tight text-white lg:text-4xl">{title}</h1>
          {description && <p className="max-w-3xl text-sm leading-6 text-slate-300">{description}</p>}
        </div>
      </div>
      {actions && <div className="flex flex-wrap items-center gap-3">{actions}</div>}
    </div>
  );
}

export function AdminSurface({
  children,
  className,
}: {
  children: ReactNode;
  className?: string;
}) {
  return (
    <div
      className={clsx(
        'rounded-3xl border border-white/10 bg-slate-950/55 shadow-[0_24px_80px_-40px_rgba(15,23,42,0.9)] backdrop-blur',
        className
      )}
    >
      {children}
    </div>
  );
}

export function AdminStatCard({
  label,
  value,
  description,
  icon,
  tone = 'default',
}: {
  label: string;
  value: string | number;
  description: string;
  icon: ReactNode;
  tone?: 'default' | 'success' | 'warning' | 'danger';
}) {
  const toneClass = {
    default: 'from-cyan-500/20 to-blue-500/10 text-cyan-200 ring-cyan-400/30',
    success: 'from-emerald-500/20 to-teal-500/10 text-emerald-200 ring-emerald-400/30',
    warning: 'from-amber-500/20 to-orange-500/10 text-amber-100 ring-amber-400/30',
    danger: 'from-rose-500/20 to-red-500/10 text-rose-100 ring-rose-400/30',
  }[tone];

  return (
    <AdminSurface className="p-5">
      <div className="flex items-start justify-between gap-4">
        <div className="space-y-3">
          <p className="text-sm font-medium text-slate-300">{label}</p>
          <p className="text-3xl font-semibold tracking-tight text-white">{value}</p>
          <p className="text-sm text-slate-400">{description}</p>
        </div>
        <div className={clsx('rounded-2xl bg-gradient-to-br p-3 ring-1', toneClass)}>{icon}</div>
      </div>
    </AdminSurface>
  );
}

export function AdminBadge({
  children,
  tone = 'default',
}: {
  children: ReactNode;
  tone?: 'default' | 'success' | 'warning' | 'danger';
}) {
  const toneClass = {
    default: 'bg-slate-800 text-slate-200 ring-slate-700',
    success: 'bg-emerald-500/15 text-emerald-200 ring-emerald-400/25',
    warning: 'bg-amber-500/15 text-amber-100 ring-amber-400/25',
    danger: 'bg-rose-500/15 text-rose-100 ring-rose-400/25',
  }[tone];

  return (
    <span className={clsx('inline-flex items-center rounded-full px-3 py-1 text-xs font-semibold ring-1', toneClass)}>
      {children}
    </span>
  );
}

export function AdminActionButton({
  children,
  tone = 'default',
  className,
  ...props
}: React.ButtonHTMLAttributes<HTMLButtonElement> & {
  tone?: 'default' | 'primary' | 'success' | 'warning' | 'danger';
}) {
  const toneClass = {
    default: 'border border-white/10 bg-white/5 text-slate-200 hover:bg-white/10 focus:ring-cyan-400/40',
    primary: 'bg-cyan-400 text-slate-950 hover:bg-cyan-300 focus:ring-cyan-300/40',
    success: 'bg-emerald-400 text-slate-950 hover:bg-emerald-300 focus:ring-emerald-300/40',
    warning: 'bg-amber-300 text-slate-950 hover:bg-amber-200 focus:ring-amber-200/40',
    danger: 'bg-rose-400 text-slate-950 hover:bg-rose-300 focus:ring-rose-300/40',
  }[tone];

  return (
    <button
      {...props}
      className={clsx(
        'inline-flex items-center justify-center rounded-2xl px-4 py-2.5 text-sm font-semibold transition disabled:cursor-not-allowed disabled:opacity-50 focus:outline-none focus:ring-2 focus:ring-offset-0',
        toneClass,
        className
      )}
    >
      {children}
    </button>
  );
}
