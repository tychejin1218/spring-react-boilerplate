import { useEffect } from 'react';
import { useUserStore } from '@/app/stores/useUserStore';
import { useToast } from '@/shared/hooks/useToast';
import { env } from '@/app/config/env';

/** 백엔드 NotificationService 가 알림을 보낼 때 쓰는 SSE 이벤트 이름 */
const NOTIFICATION_EVENT = 'notification';

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
        // SSE 는 "event: 이름" 줄 다음에 "data: 내용" 줄이 오고, 빈 줄로 이벤트 하나가 끝난다
        let eventName = 'message';

        while (true) {
          const { done, value } = await reader.read();
          if (done) break;

          buffer += decoder.decode(value, { stream: true });

          const lines = buffer.split('\n');
          buffer = lines.pop() ?? '';

          for (const line of lines) {
            if (line.startsWith('event:')) {
              eventName = line.slice(6).trim();
            } else if (line.startsWith('data:')) {
              // 연결 확인용 connect 이벤트 등은 무시하고, 서버가 보내는 notification 이벤트만 토스트로 표시
              if (eventName !== NOTIFICATION_EVENT) continue;
              try {
                const data = JSON.parse(line.slice(5).trim()) as Partial<NotificationPayload>;
                if (typeof data.message === 'string' && data.message) toast.success(data.message);
              } catch {
                // 형식이 맞지 않는 데이터는 무시
              }
            } else if (line.trim() === '') {
              eventName = 'message';
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
