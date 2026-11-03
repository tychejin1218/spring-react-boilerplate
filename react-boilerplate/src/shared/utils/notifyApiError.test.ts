import { MutationCache, QueryCache, QueryClient } from '@tanstack/react-query';
import { http, HttpResponse } from 'msw';
import { env } from '@/app/config/env';
import { useToastStore } from '@/app/stores/useToastStore';
import { server } from '@/mocks/server';
import api from '@/shared/apis/instance';
import { ERROR_MESSAGES } from '@/shared/constants/messages';
import { notifyApiError, notifyMutationError, notifyQueryError, resetApiErrorNotifyState } from '@/shared/utils/notifyApiError';
import { handleSessionExpired, registerSessionExpiredNavigator, resetSessionExpiredGuard } from '@/shared/utils/sessionExpired';

const BASE = env.VITE_API_BASE_URL;

const createClient = () =>
  new QueryClient({
    queryCache: new QueryCache({ onError: notifyQueryError }),
    mutationCache: new MutationCache({ onError: notifyMutationError }),
    defaultOptions: { queries: { retry: false } },
  });

const toastMessages = () => useToastStore.getState().toasts.map((t) => t.message);

describe('공통 오류 알림 (notifyApiError + QueryCache/MutationCache)', () => {
  beforeEach(() => {
    resetApiErrorNotifyState();
    resetSessionExpiredGuard();
    useToastStore.setState({ toasts: [] });
    vi.spyOn(console, 'error').mockImplementation(() => {});
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('여러 쿼리가 같은 오류로 동시에 실패해도 토스트는 한 번만 뜬다', async () => {
    server.use(http.get(`${BASE}/test-404`, () => HttpResponse.json({ statusCode: 'TODO_NOT_FOUND', message: '할 일을 찾을 수 없습니다.' }, { status: 404 })));
    const client = createClient();

    await Promise.allSettled(['a', 'b', 'c'].map((key) => client.fetchQuery({ queryKey: [key], queryFn: () => api.get('/test-404') })));

    expect(toastMessages()).toEqual(['할 일을 찾을 수 없습니다.']);
  });

  it('instance.ts 가 이미 알린 500 오류는 QueryCache 가 다시 알리지 않는다', async () => {
    server.use(http.get(`${BASE}/test-500`, () => HttpResponse.json({ statusCode: 'SYSTEM_INTERNAL_ERROR', message: '내부 오류' }, { status: 500 })));
    const client = createClient();

    await expect(client.fetchQuery({ queryKey: ['500'], queryFn: () => api.get('/test-500') })).rejects.toBeDefined();

    expect(toastMessages()).toEqual([ERROR_MESSAGES.SERVER_ERROR]);
  });

  it('meta.skipErrorNotify 를 지정한 쿼리와 mutation 은 알리지 않는다', async () => {
    const client = createClient();
    const bad = { statusCode: 'VALIDATION_FAILED', message: '입력값 오류', httpStatus: 400 };

    await expect(client.fetchQuery({ queryKey: ['skip'], queryFn: () => Promise.reject(bad), meta: { skipErrorNotify: true } })).rejects.toBe(bad);
    await expect(
      client
        .getMutationCache()
        .build(client, { mutationFn: () => Promise.reject(bad), meta: { skipErrorNotify: true } })
        .execute(undefined),
    ).rejects.toBe(bad);
    expect(toastMessages()).toEqual([]);

    await expect(
      client
        .getMutationCache()
        .build(client, { mutationFn: () => Promise.reject({ ...bad }) })
        .execute(undefined),
    ).rejects.toBeDefined();
    expect(toastMessages()).toEqual(['입력값 오류']);
  });

  it('네트워크 오류는 영문 axios 메시지 대신 안내 문구로 알린다', async () => {
    server.use(http.get(`${BASE}/test-network`, () => HttpResponse.error()));
    const client = createClient();

    await expect(client.fetchQuery({ queryKey: ['network'], queryFn: () => api.get('/test-network') })).rejects.toBeDefined();

    expect(toastMessages()).toEqual([ERROR_MESSAGES.NETWORK_ERROR]);
  });

  it('세션 만료 처리 중에는 다른 오류를 알리지 않는다', () => {
    const unregister = registerSessionExpiredNavigator(() => {});
    handleSessionExpired();
    useToastStore.setState({ toasts: [] });

    expect(notifyApiError({ statusCode: 'SYSTEM_INTERNAL_ERROR', message: '', httpStatus: 500 })).toBe(false);
    expect(toastMessages()).toEqual([]);
    unregister();
  });
});
