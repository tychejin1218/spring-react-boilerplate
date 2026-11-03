import { useToastStore } from '@/app/stores/useToastStore';
import { useUserStore } from '@/app/stores/useUserStore';
import { ERROR_MESSAGES } from '@/shared/constants/messages';
import {
  LOGIN_PATH,
  handleSessionExpired,
  isSessionExpiredHandled,
  isSessionExpiryNoise,
  registerSessionExpiredNavigator,
  resetSessionExpiredGuard,
} from '@/shared/utils/sessionExpired';

const refreshInvalid = {
  statusCode: 'AUTH_REFRESH_TOKEN_INVALID',
  message: '세션이 만료되었습니다. (BE)',
  method: 'POST',
  path: '/auth/token/refresh',
  timestamp: '',
  httpStatus: 401,
};

describe('handleSessionExpired', () => {
  const navigate = vi.fn();
  let unregister: () => void;

  beforeEach(() => {
    resetSessionExpiredGuard();
    useToastStore.setState({ toasts: [] });
    useUserStore.setState({ user: { userId: 'a', name: 'a', email: 'a@a.com', role: 'ROLE_USER' }, accessToken: 'at', refreshToken: 'rt' });
    navigate.mockClear();
    unregister = registerSessionExpiredNavigator(navigate);
  });

  afterEach(() => {
    unregister();
    vi.useRealTimers();
  });

  it('여러 번 호출돼도 안내와 이동은 한 번만 하고 사용자 정보는 지운다', () => {
    expect(handleSessionExpired(refreshInvalid)).toBe(true);
    expect(handleSessionExpired(refreshInvalid)).toBe(false);
    expect(handleSessionExpired()).toBe(false);

    expect(useUserStore.getState()).toMatchObject({ user: null, accessToken: null, refreshToken: null });
    expect(useToastStore.getState().toasts).toHaveLength(1);
    expect(navigate).toHaveBeenCalledTimes(1);
    expect(navigate).toHaveBeenCalledWith(LOGIN_PATH);
    expect(isSessionExpiredHandled()).toBe(true);
  });

  it('BE 메시지가 있으면 그 메시지를, 없으면 공통 세션 만료 문구를 보여 준다', () => {
    handleSessionExpired(refreshInvalid);
    expect(useToastStore.getState().toasts[0]).toMatchObject({ type: 'warning', message: '세션이 만료되었습니다. (BE)' });

    resetSessionExpiredGuard();
    useToastStore.setState({ toasts: [] });
    handleSessionExpired(new Error('Network Error'));
    expect(useToastStore.getState().toasts[0].message).toBe(ERROR_MESSAGES.SESSION_EXPIRED);
  });

  it('가드를 초기화하면(로그인 성공) 다음 만료를 다시 처리한다', () => {
    handleSessionExpired();
    resetSessionExpiredGuard();
    expect(handleSessionExpired()).toBe(true);
    expect(navigate).toHaveBeenCalledTimes(2);
  });

  it('만료 직후에는 모든 오류를, 그 뒤에는 401 만 알림 대상에서 뺀다', () => {
    vi.useFakeTimers();
    const serverError = { statusCode: 'SYSTEM_INTERNAL_ERROR', message: '', httpStatus: 500 };
    expect(isSessionExpiryNoise(serverError)).toBe(false);

    handleSessionExpired();
    expect(isSessionExpiryNoise(serverError)).toBe(true);

    vi.advanceTimersByTime(3000);
    expect(isSessionExpiryNoise(serverError)).toBe(false);
    expect(isSessionExpiryNoise(refreshInvalid)).toBe(true);
  });
});
