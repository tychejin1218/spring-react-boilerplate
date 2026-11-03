import { http, HttpResponse } from 'msw';
import { env } from '@/app/config/env';
import { useToastStore } from '@/app/stores/useToastStore';
import { useUserStore } from '@/app/stores/useUserStore';
import { server } from '@/mocks/server';
import api from '@/shared/apis/instance';
import { ERROR_MESSAGES } from '@/shared/constants/messages';
import { resetApiErrorNotifyState } from '@/shared/utils/notifyApiError';
import { LOGIN_PATH, registerSessionExpiredNavigator, resetSessionExpiredGuard } from '@/shared/utils/sessionExpired';

const BASE = env.VITE_API_BASE_URL;

beforeEach(() => {
  resetApiErrorNotifyState();
  resetSessionExpiredGuard();
  useToastStore.setState({ toasts: [] });
  vi.spyOn(console, 'error').mockImplementation(() => {});
});

afterEach(() => {
  vi.restoreAllMocks();
  useUserStore.getState().clearUser();
});

describe('api 응답 인터셉터 공통 에러 토스트', () => {
  it('500 응답이 동시에 여러 번 와도 서버 오류 토스트는 한 번만 표시된다', async () => {
    server.use(http.get(`${BASE}/test-500`, () => HttpResponse.json({ statusCode: 'SYSTEM_INTERNAL_ERROR', message: 'boom' }, { status: 500 })));

    await Promise.allSettled([api.get('/test-500'), api.get('/test-500'), api.get('/test-500')]);

    const { toasts } = useToastStore.getState();
    expect(toasts).toHaveLength(1);
    expect(toasts[0]).toMatchObject({ type: 'error', message: ERROR_MESSAGES.SERVER_ERROR });
    expect(console.error).toHaveBeenCalled();
  });

  it('reject 값은 BE ErrorResponse 본문에 httpStatus 를 붙인 객체다', async () => {
    server.use(http.get(`${BASE}/test-400`, () => HttpResponse.json({ statusCode: 'VALIDATION_FAILED', message: 'bad' }, { status: 400 })));

    await expect(api.get('/test-400')).rejects.toMatchObject({ statusCode: 'VALIDATION_FAILED', message: 'bad', httpStatus: 400 });
  });

  it('처리 대상이 아닌 상태 코드(400)는 토스트를 띄우지 않는다 (QueryCache 또는 화면이 처리)', async () => {
    server.use(http.get(`${BASE}/test-400`, () => HttpResponse.json({ statusCode: 'VALIDATION_FAILED', message: 'bad' }, { status: 400 })));

    await expect(api.get('/test-400')).rejects.toMatchObject({ message: 'bad' });

    expect(useToastStore.getState().toasts).toHaveLength(0);
  });
});

describe('api 응답 인터셉터 세션 만료 처리', () => {
  const navigate = vi.fn();
  let unregister: () => void;
  let refreshCalls = 0;

  beforeEach(() => {
    navigate.mockClear();
    refreshCalls = 0;
    unregister = registerSessionExpiredNavigator(navigate);
    useUserStore.setState({ user: { userId: 'a', name: 'a', email: 'a@a.com', role: 'ROLE_USER' }, accessToken: 'expired', refreshToken: 'rt' });
    server.use(
      http.get(`${BASE}/test-401`, () => HttpResponse.json({ statusCode: 'AUTH_UNAUTHORIZED', message: '인증 정보가 유효하지 않습니다.' }, { status: 401 })),
      http.post(`${BASE}/auth/token/refresh`, () => {
        refreshCalls += 1;
        return HttpResponse.json({ statusCode: 'AUTH_REFRESH_TOKEN_INVALID', message: '세션이 만료되었습니다. 다시 로그인해주세요.' }, { status: 401 });
      }),
    );
  });

  afterEach(() => unregister());

  it('동시에 여러 요청이 갱신에 실패해도 갱신은 한 번, 안내와 이동도 한 번만 한다', async () => {
    const results = await Promise.allSettled([api.get('/test-401'), api.get('/test-401'), api.get('/test-401')]);

    expect(results.every((r) => r.status === 'rejected')).toBe(true);
    expect(refreshCalls).toBe(1);
    expect(navigate).toHaveBeenCalledTimes(1);
    expect(navigate).toHaveBeenCalledWith(LOGIN_PATH);
    expect(useUserStore.getState().accessToken).toBeNull();
    expect(useToastStore.getState().toasts).toEqual([expect.objectContaining({ type: 'warning', message: '세션이 만료되었습니다. 다시 로그인해주세요.' })]);
  });

  it('세션 만료 직후 함께 실패한 500 은 토스트를 띄우지 않는다', async () => {
    server.use(http.get(`${BASE}/test-500`, () => HttpResponse.json({ statusCode: 'SYSTEM_INTERNAL_ERROR', message: '' }, { status: 500 })));

    await Promise.allSettled([api.get('/test-401'), api.get('/test-401')]);
    await Promise.allSettled([api.get('/test-500')]);

    expect(useToastStore.getState().toasts).toHaveLength(1);
    expect(useToastStore.getState().toasts[0].type).toBe('warning');
  });

  it('로그인 요청의 401 은 세션 만료로 처리하지 않는다', async () => {
    server.use(
      http.post(`${BASE}/auth/login`, () =>
        HttpResponse.json({ statusCode: 'AUTH_LOGIN_FAILED', message: '이메일 또는 비밀번호가 올바르지 않습니다.' }, { status: 401 }),
      ),
    );

    await expect(api.post('/auth/login', { email: 'a', password: 'b' })).rejects.toMatchObject({ statusCode: 'AUTH_LOGIN_FAILED', httpStatus: 401 });

    expect(refreshCalls).toBe(0);
    expect(navigate).not.toHaveBeenCalled();
    expect(useUserStore.getState().accessToken).toBe('expired');
  });
});
