import { useState, useCallback } from 'react';

interface SummaryState {
  excerpt: string;
  tldr: string;
  detailed: string;
  status: 'idle' | 'streaming' | 'completed' | 'error';
  error: string | null;
  currentType: 'excerpt' | 'tldr' | 'detailed' | null;
}

export function useSummaryStream(postId: number) {
  const [state, setState] = useState<SummaryState>({
    excerpt: '',
    tldr: '',
    detailed: '',
    status: 'idle',
    error: null,
    currentType: null,
  });

  const startStream = useCallback(async () => {
    setState(prev => ({ ...prev, status: 'streaming' }));

    try {
      // SSE 스트리밍 요청
      const response = await fetch(
        `http://localhost:8000/api/v1/summaries/stream/${postId}`,
        {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
        }
      );

      if (!response.ok) {
        throw new Error('스트리밍 요청 실패');
      }

      const reader = response.body?.getReader();
      const decoder = new TextDecoder();

      if (!reader) {
        throw new Error('ReadableStream을 읽을 수 없습니다');
      }

      let buffer = ''; // 버퍼 추가

      while (true) {
        const { done, value } = await reader.read();
        if (done) break;

        // 청크 디코딩 및 버퍼에 추가
        buffer += decoder.decode(value, { stream: true });

        // 완전한 라인들만 처리
        const lines = buffer.split('\n');
        // 마지막 불완전한 라인은 버퍼에 남김
        buffer = lines.pop() || '';

        for (const line of lines) {
          if (!line.trim()) continue;

          try {
            const data = JSON.parse(line);

            switch (data.type) {
              case 'excerpt':
                setState(prev => ({
                  ...prev,
                  excerpt: data.content,
                  currentType: 'excerpt',
                }));
                break;

              case 'start':
                setState(prev => ({
                  ...prev,
                  currentType: data.summary_type,
                }));
                break;

              case 'chunk':
                if (data.summary_type === 'tldr') {
                  setState(prev => ({
                    ...prev,
                    tldr: prev.tldr + data.content,
                  }));
                } else if (data.summary_type === 'detailed') {
                  setState(prev => ({
                    ...prev,
                    detailed: prev.detailed + data.content,
                  }));
                }
                break;

              case 'complete':
                setState(prev => ({ ...prev, currentType: null }));
                break;

              case 'done':
                setState(prev => ({ ...prev, status: 'completed' }));
                break;

              case 'error':
                setState(prev => ({
                  ...prev,
                  status: 'error',
                  error: data.message,
                }));
                break;
            }
          } catch (e) {
            console.error('JSON 파싱 오류:', e);
          }
        }
      }
    } catch (error) {
      setState(prev => ({
        ...prev,
        status: 'error',
        error: error instanceof Error ? error.message : '알 수 없는 오류',
      }));
    }
  }, [postId]);

  return { ...state, startStream };
}
