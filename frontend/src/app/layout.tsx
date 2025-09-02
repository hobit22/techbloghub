import type { Metadata } from "next";
import { Inter } from "next/font/google";
import "./globals.css";
import { ReactQueryProvider } from "@/lib/react-query";

const inter = Inter({ 
  subsets: ["latin"],
  display: "swap",
  variable: "--font-inter",
});

export const metadata: Metadata = {
  title: {
    default: "TechBlogHub - 기술 블로그 모음",
    template: "%s | TechBlogHub"
  },
  description: "국내 IT 대기업 기술 블로그를 한 곳에서 모아보세요. 네이버, 카카오, 쿠팡, 당근마켓 등 주요 IT 기업의 최신 기술 동향과 개발 인사이트를 실시간으로 확인하세요.",
  keywords: ["기술블로그", "개발블로그", "IT블로그", "네이버", "카카오", "쿠팡", "당근마켓", "개발자", "기술동향"],
  authors: [{ name: "Hobit22" }],
  creator: "Hobit22",
  publisher: "TechBlogHub",
  formatDetection: {
    email: false,
    address: false,
    telephone: false,
  },
  metadataBase: new URL(process.env.NEXT_PUBLIC_BASE_URL || 'https://techbloghub.com'),
  openGraph: {
    title: "TechBlogHub - 기술 블로그 모음",
    description: "국내 IT 대기업 기술 블로그를 한 곳에서 모아보세요",
    url: "/",
    siteName: "TechBlogHub",
    type: "website",
    locale: "ko_KR",
    images: [
      {
        url: "/og-image.png",
        width: 1200,
        height: 630,
        alt: "TechBlogHub - 기술 블로그 모음",
      },
    ],
  },
  twitter: {
    card: "summary_large_image",
    title: "TechBlogHub - 기술 블로그 모음",
    description: "국내 IT 대기업 기술 블로그를 한 곳에서 모아보세요",
    images: ["/og-image.png"],
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
    "name": "TechBlogHub",
    "url": process.env.NEXT_PUBLIC_BASE_URL || "https://techbloghub.com",
    "description": "국내 IT 대기업 기술 블로그를 한 곳에서 모아보세요",
    "publisher": {
      "@type": "Organization",
      "name": "TechBlogHub",
    },
    "potentialAction": {
      "@type": "SearchAction",
      "target": {
        "@type": "EntryPoint",
        "urlTemplate": `${process.env.NEXT_PUBLIC_BASE_URL || "https://techbloghub.com"}/?keyword={search_term_string}`
      },
      "query-input": "required name=search_term_string"
    }
  };

  return (
    <html lang="ko" className={inter.variable}>
      <head>
        <script
          type="application/ld+json"
          dangerouslySetInnerHTML={{ __html: JSON.stringify(jsonLd) }}
        />
      </head>
      <body className={`${inter.className} antialiased`}>
        <ReactQueryProvider>
          {children}
        </ReactQueryProvider>
      </body>
    </html>
  );
}
