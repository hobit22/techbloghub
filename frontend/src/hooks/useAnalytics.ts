'use client';

import { useEffect } from 'react';
import { pageview, event } from '@/lib/analytics';

export const usePageView = (url: string) => {
  useEffect(() => {
    // 관리자 페이지에서는 GA 추적하지 않음
    if (process.env.NEXT_PUBLIC_GA_ID && !url.startsWith('/admin')) {
      pageview(url);
    }
  }, [url]);
};

export const useAnalytics = () => {
  const trackEvent = ({
    action,
    category,
    label,
    value,
  }: {
    action: string;
    category?: string;
    label?: string;
    value?: number;
  }) => {
    // 관리자 페이지에서는 GA 이벤트 추적하지 않음
    if (typeof window !== "undefined" && window.location.pathname.startsWith('/admin')) {
      return;
    }

    if (process.env.NEXT_PUBLIC_GA_ID) {
      event({ action, category, label, value });
    }
  };

  return { trackEvent };
};