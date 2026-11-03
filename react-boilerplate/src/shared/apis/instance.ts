import axios, { type InternalAxiosRequestConfig } from 'axios';
import type { ErrorResponse } from '@/shared/types/api';
import { ERROR_MESSAGES } from '@/shared/constants/messages';
import { notifyApiError } from '@/shared/utils/notifyApiError';
import { handleSessionExpired } from '@/shared/utils/sessionExpired';
// 예외: 인증 인터셉터(cross-cutting concern)로 app 레이어 참조
import { useUserStore } from '@/app/stores/useUserStore';
import { env } from '@/app/config/env';

const api = axios.create({
  baseURL: env.VITE_API_BASE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

/** 재시도 여부를 기록하기 위한 요청 설정 확장 */
interface RetryableRequestConfig extends InternalAxiosRequestConfig {
  _retry?: boolean;
}

/**
 * 진행 중인 토큰 갱신 요청 (single-flight).
 * 여러 요청이 동시에 401 을 받아도 refresh 는 한 번만 호출하고, 나머지는 그 결과를 기다린다.
 * 백엔드가 Refresh Token Rotation(갱신 때마다 refresh 토큰 교체 + 재사용 감지)을 쓰므로
 * refresh 를 두 번 보내면 두 번째가 재사용으로 거부되어 강제 로그아웃된다.
 */
let refreshPromise: Promise<string> | null = null;

const refreshAccessToken = (): Promise<string> => {
  if (!refreshPromise) {
    const { refreshToken } = useUserStore.getState();
    refreshPromise = axios
      .post(`${env.VITE_API_BASE_URL}/auth/token/refresh`, { refreshToken }, { headers: { 'Content-Type': 'application/json' }, timeout: 10000 })
      .then((response) => {
        const { accessToken: newAccessToken, refreshToken: newRefreshToken } = response.data.data;
        useUserStore.getState().setAccessToken(newAccessToken);
        useUserStore.getState().setRefreshToken(newRefreshToken);
        return newAccessToken as string;
      })
      .finally(() => {
        refreshPromise = null;
      });
  }
  return refreshPromise;
};

/**
 * 화면으로 reject 할 오류 값.
 * BE ErrorResponse 본문이면 HTTP 상태 코드를 붙여 넘기고(4xx/5xx 구분용),
 * 본문이 JSON 이 아니거나(프록시 502 HTML 등) 응답이 없으면 axios 오류를 그대로 넘긴다.
 * 어느 쪽이든 getErrorPresentation 이 사용자 문구로 바꾼다.
 */
const toRejectedError = (error: unknown): unknown => {
  if (!axios.isAxiosError(error) || !error.response) return error;
  const { status, data } = error.response;
  if (typeof data === 'object' && data !== null && typeof (data as ErrorResponse).statusCode === 'string') {
    return { ...(data as ErrorResponse), httpStatus: status } satisfies ErrorResponse;
  }
  return error;
};

/**
 * 401 이어도 세션 만료로 처리하지 않는 요청
 * - 로그인: 401 은 자격 증명 오류(AUTH_LOGIN_FAILED)이고 화면이 직접 안내한다
 * - Authorization 헤더 없이 보낸 요청: 만료될 세션 자체가 없다
 */
const isSessionExemptRequest = (config: InternalAxiosRequestConfig) => Boolean(config.url?.includes('/auth/login')) || !config.headers?.Authorization;

/** 요청 인터셉터: Authorization 헤더 주입 */
api.interceptors.request.use(
  (config) => {
    const { accessToken } = useUserStore.getState();
    if (accessToken) {
      config.headers.Authorization = `Bearer ${accessToken}`;
    }
    return config;
  },
  (error) => Promise.reject(error),
);

/**
 * 응답 인터셉터: 공통 에러 처리
 *
 * 알림 책임 분담 (이중 알림 방지)
 * - 403, 5xx: 여기서 알린다. useQuery/useMutation 을 거치지 않는 직접 호출(로그인·회원가입 화면, 파일 업로드)도
 *   있어 인터셉터가 모든 호출을 빠짐없이 덮는 유일한 지점이기 때문이다.
 * - 그 외(4xx, 네트워크 오류 등): QueryProvider 의 QueryCache/MutationCache 가 알린다. 직접 호출하는 화면은
 *   setError 로 직접 안내하므로 여기서 알리면 오히려 중복이 된다.
 * 여기서 알린 오류가 QueryCache 로 다시 들어와도 notifyApiError 가 같은 오류 객체를 기억해 두 번 알리지 않는다.
 */
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (!axios.isAxiosError(error) || !error.response) {
      return Promise.reject(error);
    }

    const { status } = error.response;
    const rejected = toRejectedError(error);

    if (status === 401) {
      const originalRequest = error.config as RetryableRequestConfig | undefined;

      if (!originalRequest || isSessionExemptRequest(originalRequest)) {
        return Promise.reject(rejected);
      }

      // 토큰 갱신 요청 자체에서 401 이 오거나, 이미 한 번 재시도한 요청이면 세션 만료
      if (originalRequest.url?.includes('/auth/token/refresh') || originalRequest._retry) {
        handleSessionExpired(rejected);
        return Promise.reject(rejected);
      }

      if (!useUserStore.getState().refreshToken) {
        console.error(`[401] ${ERROR_MESSAGES.UNAUTHORIZED}`, rejected);
        handleSessionExpired(rejected);
        return Promise.reject(rejected);
      }

      try {
        const newAccessToken = await refreshAccessToken();
        originalRequest._retry = true;
        originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;
        return api(originalRequest);
      } catch (refreshError) {
        // 동시에 갱신을 기다리던 요청이 모두 여기로 오지만 handleSessionExpired 가 처음 한 번만 처리한다.
        // 안내 문구는 갱신 응답(예: AUTH_REFRESH_TOKEN_INVALID)의 BE 메시지를 쓴다
        handleSessionExpired(toRejectedError(refreshError));
        return Promise.reject(rejected);
      }
    }

    if (status === 403 || status >= 500) {
      console.error(`[${status}]`, rejected);
      notifyApiError(rejected);
    }
    if (status === 429) {
      console.error(`[429] ${ERROR_MESSAGES.RATE_LIMIT}`, rejected);
    }

    return Promise.reject(rejected);
  },
);

export default api;
