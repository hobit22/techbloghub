import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // 실험적 기능들
  experimental: {
    // 서버 액션 최적화
    serverComponentsExternalPackages: [],
  },

  // 이미지 최적화
  images: {
    formats: ['image/avif', 'image/webp'],
    deviceSizes: [640, 768, 1024, 1280, 1600],
    imageSizes: [16, 32, 48, 64, 96, 128, 256, 384],
  },

  // 압축 활성화
  compress: true,

  // 개발 모드에서 React Strict Mode
  reactStrictMode: true,

  // 번들 분석기 (필요시 활성화)
  // webpack: (config, { isServer }) => {
  //   if (process.env.ANALYZE === 'true') {
  //     config.plugins.push(
  //       new (require('@next/bundle-analyzer'))({
  //         enabled: true,
  //       })
  //     )
  //   }
  //   return config
  // },

  // PoweredBy 헤더 제거
  poweredByHeader: false,

  // 트레일링 슬래시 설정
  trailingSlash: false,

  // 리다이렉트 설정
  async redirects() {
    return [];
  },

  // 헤더 설정
  async headers() {
    return [
      {
        source: '/(.*)',
        headers: [
          {
            key: 'X-Frame-Options',
            value: 'DENY',
          },
          {
            key: 'X-Content-Type-Options', 
            value: 'nosniff',
          },
          {
            key: 'Referrer-Policy',
            value: 'strict-origin-when-cross-origin',
          },
        ],
      },
      {
        source: '/api/:path*',
        headers: [
          {
            key: 'Cache-Control',
            value: 'public, max-age=300, s-maxage=300', // 5분 캐시
          },
        ],
      },
    ];
  },
};

export default nextConfig;
