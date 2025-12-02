'use client';

import { useEffect } from 'react';
import { useSummaryStream } from '@/hooks/useSummaryStream';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';

interface PostSummaryProps {
  postId: number;
}

export function PostSummary({ postId }: PostSummaryProps) {
  const { tldr, detailed, status, error, currentType, startStream } =
    useSummaryStream(postId);

  useEffect(() => {
    // 컴포넌트 마운트 시 스트리밍 시작
    startStream();
  }, [startStream]);

  if (status === 'error') {
    return (
      <div className="p-4 bg-red-50 border border-red-200 rounded-lg">
        <p className="text-red-600">요약 생성 중 오류가 발생했습니다: {error}</p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* TL;DR 요약 */}
      <section className="p-4 bg-blue-50 rounded-lg">
        <h2 className="text-lg font-semibold mb-3 text-blue-700 flex items-center gap-2">
          TL;DR
          {currentType === 'tldr' && (
            <span className="inline-block w-2 h-2 bg-blue-500 rounded-full animate-pulse " />
          )}
        </h2>
        <div className="prose prose-slate max-w-none text-slate-700">
          {tldr ? (
            <ReactMarkdown remarkPlugins={[remarkGfm]}>{tldr}</ReactMarkdown>
          ) : (
            <div className="flex items-center gap-2 text-gray-500">
              <span>요약 생성 중...</span>
              <div className="animate-spin h-4 w-4 border-2 border-blue-500 border-t-transparent rounded-full" />
            </div>
          )}
        </div>
      </section>

      {/* 상세 요약 */}
      <section className="p-4 bg-green-50 rounded-lg">
        <h2 className="text-lg font-semibold mb-3 text-green-700 flex items-center gap-2">
          상세 요약
          {currentType === 'detailed' && (
            <span className="inline-block w-2 h-2 bg-green-500 rounded-full animate-pulse" />
          )}
        </h2>
        <div className="prose prose-slate max-w-none text-slate-700">
          {detailed ? (
            <ReactMarkdown remarkPlugins={[remarkGfm]}>{detailed}</ReactMarkdown>
          ) : (
            <div className="flex items-center gap-2 text-gray-500">
              <span>상세 요약 대기 중...</span>
            </div>
          )}
        </div>
      </section>
    </div>
  );
}
