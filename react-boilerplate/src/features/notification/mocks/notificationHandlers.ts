import { http, HttpResponse } from 'msw';
import { env } from '@/app/config/env';

const BASE = env.VITE_API_BASE_URL;

const encoder = new TextEncoder();

const sseEvent = (name: string, data: unknown) => encoder.encode(`event: ${name}\ndata: ${JSON.stringify(data)}\n\n`);

export const notificationHandlers = [
  http.get(`${BASE}/notifications/subscribe`, () => {
    let timerId: ReturnType<typeof setTimeout>;

    const stream = new ReadableStream({
      start(controller) {
        controller.enqueue(sseEvent('connect', 'connected'));

        // 3초 후 테스트 알림 발송
        timerId = setTimeout(() => {
          controller.enqueue(
            sseEvent('notification', {
              todoId: 1,
              todoTitle: '테스트 할 일',
              message: '"테스트 할 일" 완료!',
            }),
          );
          controller.close();
        }, 3000);
      },
      cancel() {
        clearTimeout(timerId);
      },
    });

    return new HttpResponse(stream, {
      headers: {
        'Content-Type': 'text/event-stream',
        'Cache-Control': 'no-cache',
      },
    });
  }),
];
