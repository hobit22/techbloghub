declare global {
  interface Window {
    gtag: (
      command: string,
      targetId: string,
      config?: Record<string, unknown>
    ) => void;
  }
}

export const pageview = (url: string) => {
  // 관리자 페이지에서는 GA 추적하지 않음
  if (url.startsWith('/admin')) {
    return;
  }

  if (typeof window !== "undefined" && window.gtag) {
    window.gtag("config", process.env.NEXT_PUBLIC_GA_ID!, {
      page_path: url,
    });
  }
};

export const event = ({
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

  if (typeof window !== "undefined" && window.gtag) {
    window.gtag("event", action, {
      event_category: category,
      event_label: label,
      value: value,
    });
  }
};
