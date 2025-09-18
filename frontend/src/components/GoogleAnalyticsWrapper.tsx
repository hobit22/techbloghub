'use client';

import { usePathname } from 'next/navigation';
import { GoogleAnalytics } from '@next/third-parties/google';

interface GoogleAnalyticsWrapperProps {
  gaId: string;
}

export default function GoogleAnalyticsWrapper({ gaId }: GoogleAnalyticsWrapperProps) {
  const pathname = usePathname();

  // 관리자 페이지에서는 GA를 로드하지 않음
  const isAdminPage = pathname?.startsWith('/admin');

  // GA ID가 없거나 관리자 페이지인 경우 GA를 로드하지 않음
  if (!gaId || isAdminPage) {
    return null;
  }

  return <GoogleAnalytics gaId={gaId} />;
}