'use client';

import { useEffect } from 'react';
import { pageview, event } from '@/lib/analytics';

export const usePageView = (url: string) => {
  useEffect(() => {
    if (process.env.NEXT_PUBLIC_GA_ID) {
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
    if (process.env.NEXT_PUBLIC_GA_ID) {
      event({ action, category, label, value });
    }
  };

  return { trackEvent };
};