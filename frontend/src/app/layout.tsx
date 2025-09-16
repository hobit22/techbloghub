import type { Metadata } from "next";
import { Inter } from "next/font/google";
import "./globals.css";
import { ReactQueryProvider } from "@/lib/react-query";
import { GoogleAnalytics } from "@next/third-parties/google";

const inter = Inter({ 
  subsets: ["latin"],
  display: "swap",
  variable: "--font-inter",
});

export const metadata: Metadata = {
  title: {
    default: "TeckBlogHub - 기술 블로그 모음",
    template: "%s | TeckBlogHub"
  },
  description: "국내 IT 대기업 기술 블로그를 한 곳에서 모아보세요. 네이버, 카카오, 쿠팡, 당근마켓 등 주요 IT 기업의 최신 기술 동향과 개발 인사이트를 실시간으로 확인하세요.",
  keywords: ["기술블로그", "개발블로그", "IT블로그", "네이버", "카카오", "쿠팡", "당근마켓", "개발자", "기술동향"],
  authors: [{ name: "Hobit22" }],
  creator: "Hobit22",
  publisher: "TeckBlogHub",
  formatDetection: {
    email: false,
    address: false,
    telephone: false,
  },
  metadataBase: new URL(process.env.NEXT_PUBLIC_BASE_URL || 'https://teckbloghub.kr'),
  icons: {
    icon: [
      { url: '/icon.svg', type: 'image/svg+xml' },
      { url: '/favicon.ico', sizes: '32x32' },
    ],
    apple: [
      { url: '/apple-touch-icon.png', sizes: '180x180' },
    ],
  },
  openGraph: {
    title: "TechBlogHub - 기술 블로그 모음",
    description: "국내 IT 대기업 기술 블로그를 한 곳에서 모아보세요",
    url: "/",
    siteName: "TeckBlogHub",
    type: "website",
    locale: "ko_KR",
    images: [
      {
        url: "/og-image.svg",
        width: 1200,
        height: 630,
        alt: "TechBlogHub - 기술 블로그 모음",
        type: "image/svg+xml",
      },
    ],
  },
  twitter: {
    card: "summary_large_image",
    title: "TechBlogHub - 기술 블로그 모음",
    description: "국내 IT 대기업 기술 블로그를 한 곳에서 모아보세요",
    images: ["/og-image.svg"],
    creator: "@teckbloghub",
  },
  robots: {
    index: true,
    follow: true,
    googleBot: {
      index: true,
      follow: true,
      'max-video-preview': -1,
      'max-image-preview': 'large',
      'max-snippet': -1,
    },
  },
  verification: {
    google: process.env.NEXT_PUBLIC_GOOGLE_VERIFICATION,
    other: {
      'naver-site-verification': process.env.NEXT_PUBLIC_NAVER_VERIFICATION || '',
    },
  },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  const jsonLd = {
    "@context": "https://schema.org",
    "@type": "WebSite",
    "name": "TeckBlogHub",
    "url": process.env.NEXT_PUBLIC_BASE_URL || "https://teckbloghub.kr",
    "description": "국내 IT 대기업 기술 블로그를 한 곳에서 모아보세요",
    "publisher": {
      "@type": "Organization",
      "name": "TeckBlogHub",
    },
    "potentialAction": {
      "@type": "SearchAction",
      "target": {
        "@type": "EntryPoint",
        "urlTemplate": `${process.env.NEXT_PUBLIC_BASE_URL || "https://teckbloghub.kr"}/?keyword={search_term_string}`
      },
      "query-input": "required name=search_term_string"
    }
  };

  return (
    <html lang="ko" className={inter.variable}>
      <head>
        <link rel="manifest" href="/manifest.json" />
        <meta name="theme-color" content="#3B82F6" />
        <meta name="mobile-web-app-capable" content="yes" />
        <meta name="apple-mobile-web-app-capable" content="yes" />
        <meta name="apple-mobile-web-app-status-bar-style" content="default" />
        <meta name="apple-mobile-web-app-title" content="TeckBlogHub" />
        
        <script
          type="application/ld+json"
          dangerouslySetInnerHTML={{ __html: JSON.stringify(jsonLd) }}
        />
        {process.env.NEXT_PUBLIC_GA_ID && (
          <GoogleAnalytics gaId={process.env.NEXT_PUBLIC_GA_ID} />
        )}
      </head>
      <body className={`${inter.className} antialiased`}>
        <ReactQueryProvider>
          {children}
        </ReactQueryProvider>
      </body>
    </html>
  );
}
