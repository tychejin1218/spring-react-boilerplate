import axios, { type InternalAxiosRequestConfig } from 'axios';
import type { ErrorResponse } from '@/shared/types/api';
import { ERROR_MESSAGES } from '@/shared/constants/messages';
// 예외: 인증 인터셉터(cross-cutting concern)로 app 레이어 참조
import { useUserStore } from '@/app/stores/useUserStore';
// 예외: 전역 에러 토스트(cross-cutting concern)로 app 레이어 참조. 컴포넌트 밖이므로 훅 대신 getState() 로 호출
import { useToastStore } from '@/app/stores/useToastStore';
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

/** 같은 메시지의 토스트를 다시 띄우지 않는 간격 (동시에 여러 요청이 실패할 때 중복 방지) */
const TOAST_DEDUPE_MS = 2000;
const TOAST_DURATION_MS = 3000;
const lastToastAt = new Map<string, number>();

/** 공통 에러 토스트 표시. 같은 메시지가 TOAST_DEDUPE_MS 안에 이미 표시됐으면 건너뛴다 */
const showErrorToast = (message: string) => {
  const now = Date.now();
  const last = lastToastAt.get(message);
  if (last !== undefined && now - last < TOAST_DEDUPE_MS) return;
  lastToastAt.set(message, now);
  useToastStore.getState().add({ type: 'error', message, duration: TOAST_DURATION_MS });
};

/**
 * 토스트로 알리는 상태 코드별 공통 메시지
 * - 401: 토큰 갱신 흐름에서 처리하므로 제외
 * - 429: 요청 횟수 제한은 로그인, 회원가입에만 걸리고 두 화면이 재시도 카운트다운을 직접 보여 주므로 제외
 */
const TOAST_MESSAGES: Partial<Record<number, string>> = {
  403: ERROR_MESSAGES.FORBIDDEN,
  500: ERROR_MESSAGES.SERVER_ERROR,
};

const logoutAndRedirect = () => {
  useUserStore.getState().clearUser();
  window.location.href = '/';
};

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

/** 응답 인터셉터: 공통 에러 처리 */
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (axios.isAxiosError(error) && error.response) {
      const { status, data } = error.response;
      const errorResponse = data as ErrorResponse;

      if (status === 401) {
        const originalRequest = error.config as RetryableRequestConfig | undefined;

        // 토큰 갱신 요청 자체에서 401 이 오거나, 이미 한 번 재시도한 요청이면 바로 로그아웃
        if (originalRequest?.url?.includes('/auth/token/refresh') || originalRequest?._retry) {
          logoutAndRedirect();
          return Promise.reject(errorResponse);
        }

        const { refreshToken } = useUserStore.getState();

        if (!refreshToken || !originalRequest) {
          console.error(`[401] ${ERROR_MESSAGES.UNAUTHORIZED}`, errorResponse);
          logoutAndRedirect();
          return Promise.reject(errorResponse);
        }

        try {
          const newAccessToken = await refreshAccessToken();
          originalRequest._retry = true;
          originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;
          return api(originalRequest);
        } catch {
          logoutAndRedirect();
          return Promise.reject(errorResponse);
        }
      }

      const toastMessage = TOAST_MESSAGES[status];
      if (toastMessage) {
        console.error(`[${status}] ${toastMessage}`, errorResponse);
        showErrorToast(toastMessage);
      }
      if (status === 429) {
        console.error(`[429] ${ERROR_MESSAGES.RATE_LIMIT}`, errorResponse);
      }

      return Promise.reject(errorResponse);
    }

    return Promise.reject(error);
  },
);

export default api;
