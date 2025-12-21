import { useState, useCallback, useEffect, useRef } from 'react';
import { API_URL } from '@/lib/config';

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

  const eventSourceRef = useRef<EventSource | null>(null);

  const startStream = useCallback(() => {
    setState(prev => ({ ...prev, status: 'streaming' }));

    // EventSource 생성 (SSE)
    const eventSource = new EventSource(
      `${API_URL}/api/v1/summaries/stream/${postId}`
    );

    eventSourceRef.current = eventSource;

    // 메시지 수신
    eventSource.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data);

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
            eventSource.close();
            break;

          case 'error':
            setState(prev => ({
              ...prev,
              status: 'error',
              error: data.message,
            }));
            eventSource.close();
            break;
        }
      } catch (e) {
        console.error('JSON 파싱 오류:', e);
      }
    };

    // 에러 처리
    eventSource.onerror = (error) => {
      console.error('SSE 오류:', error);
      setState(prev => ({
        ...prev,
        status: 'error',
        error: 'SSE 연결 오류',
      }));
      eventSource.close();
    };
  }, [postId]);

  // 컴포넌트 언마운트 시 연결 종료
  useEffect(() => {
    return () => {
      if (eventSourceRef.current) {
        eventSourceRef.current.close();
      }
    };
  }, []);

  return { ...state, startStream };
}
