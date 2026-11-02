import { useEffect } from 'react';
import { useUserStore } from '@/app/stores/useUserStore';
import { useToast } from '@/shared/hooks/useToast';
import { env } from '@/app/config/env';

interface NotificationPayload {
  todoId: number;
  todoTitle: string;
  message: string;
}

/**
 * 실시간 알림 훅
 *
 * - 로그인 상태일 때 SSE 연결을 맺고 알림 이벤트를 Toast로 표시합니다.
 * - EventSource는 Authorization 헤더를 지원하지 않으므로 fetch + ReadableStream을 사용합니다.
 * - 컴포넌트 언마운트 시 AbortController로 연결을 종료합니다.
 */
export const useNotifications = () => {
  const accessToken = useUserStore((s) => s.accessToken);
  const toast = useToast();

  useEffect(() => {
    if (!accessToken) return;

    const controller = new AbortController();

    (async () => {
      try {
        const response = await fetch(`${env.VITE_API_BASE_URL}/notifications/subscribe`, {
          headers: { Authorization: `Bearer ${accessToken}` },
          signal: controller.signal,
        });

        const reader = response.body?.getReader();
        if (!reader) return;

        const decoder = new TextDecoder();
        let buffer = '';

        while (true) {
          const { done, value } = await reader.read();
          if (done) break;

          buffer += decoder.decode(value, { stream: true });

          const lines = buffer.split('\n');
          buffer = lines.pop() ?? '';

          for (const line of lines) {
            if (line.startsWith('data:')) {
              try {
                const data = JSON.parse(line.slice(5).trim()) as NotificationPayload;
                toast.success(data.message);
              } catch {
                // 파싱 실패한 이벤트는 무시 (connect 이벤트 등)
              }
            }
          }
        }
      } catch {
        // AbortError 또는 네트워크 오류 무시
      }
    })();

    return () => controller.abort();
  }, [accessToken, toast]);
};
