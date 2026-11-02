import { http, HttpResponse } from 'msw';
import { env } from '@/app/config/env';
import { useToastStore } from '@/app/stores/useToastStore';
import { server } from '@/mocks/server';
import api from '@/shared/apis/instance';
import { ERROR_MESSAGES } from '@/shared/constants/messages';

const BASE = env.VITE_API_BASE_URL;

describe('api 응답 인터셉터 공통 에러 토스트', () => {
  beforeEach(() => {
    useToastStore.setState({ toasts: [] });
    vi.spyOn(console, 'error').mockImplementation(() => {});
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('500 응답이 동시에 여러 번 와도 서버 오류 토스트는 한 번만 표시된다', async () => {
    server.use(http.get(`${BASE}/test-500`, () => HttpResponse.json({ message: 'boom' }, { status: 500 })));

    await Promise.allSettled([api.get('/test-500'), api.get('/test-500'), api.get('/test-500')]);

    const { toasts } = useToastStore.getState();
    expect(toasts).toHaveLength(1);
    expect(toasts[0]).toMatchObject({ type: 'error', message: ERROR_MESSAGES.SERVER_ERROR });
    expect(console.error).toHaveBeenCalled();
  });

  it('처리 대상이 아닌 상태 코드(400)는 토스트를 띄우지 않는다', async () => {
    server.use(http.get(`${BASE}/test-400`, () => HttpResponse.json({ message: 'bad' }, { status: 400 })));

    await expect(api.get('/test-400')).rejects.toMatchObject({ message: 'bad' });

    expect(useToastStore.getState().toasts).toHaveLength(0);
  });
});
