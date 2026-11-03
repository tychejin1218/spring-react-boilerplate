// 예외: 세션 만료 처리(cross-cutting concern)로 app 레이어 스토어 참조. 컴포넌트 밖에서도 호출되므로 getState() 사용
import { useToastStore } from '@/app/stores/useToastStore';
import { useUserStore } from '@/app/stores/useUserStore';
import { ERROR_MESSAGES } from '@/shared/constants/messages';
import { getErrorPresentation } from '@/shared/utils/apiError';

/** 세션 만료 시 이동할 로그인 페이지 경로 */
export const LOGIN_PATH = '/';

/**
 * 세션 만료 처리 직후 다른 오류 알림을 모두 숨기는 시간.
 * 만료 순간에는 진행 중이던 여러 요청이 함께 실패하는데, 그 알림이 세션 만료 안내 위에 쌓이면
 * 사용자가 해야 할 일(다시 로그인)이 가려진다.
 */
const QUIET_PERIOD_MS = 3000;
const TOAST_DURATION_MS = 4000;

/** 세션 만료를 처리한 시각. null 이면 아직 처리하지 않았다 (다음 로그인 성공 시 다시 null) */
let handledAt: number | null = null;

type Navigator = (to: string) => void;

/**
 * 기본 이동 방식. 라우터가 등록되지 않은 환경(테스트 등)용이며 전체 새로고침이라 토스트가 사라진다.
 * 앱에서는 router.tsx 가 registerSessionExpiredNavigator 로 router.navigate 를 등록해 토스트를 유지한다.
 */
const defaultNavigator: Navigator = (to) => window.location.assign(to);
let navigator: Navigator = defaultNavigator;

/**
 * 세션 만료 시 화면 이동 함수를 등록한다. instance.ts 는 컴포넌트가 아니라 useNavigate 를 쓸 수 없고,
 * shared 레이어는 app/router 를 참조할 수 없으므로 app 레이어가 주입한다.
 * @returns 등록 해제 함수
 */
export const registerSessionExpiredNavigator = (fn: Navigator) => {
  navigator = fn;
  return () => {
    if (navigator === fn) navigator = defaultNavigator;
  };
};

/** 세션 만료를 이미 처리했는지 (다음 로그인 성공 전까지 true) */
export const isSessionExpiredHandled = () => handledAt !== null;

/** 로그인 성공 시 호출해 다음 세션 만료를 다시 처리할 수 있게 한다 (useLoginSuccess) */
export const resetSessionExpiredGuard = () => {
  handledAt = null;
};

/**
 * 이 오류의 알림을 세션 만료 때문에 숨겨야 하는지.
 * - 만료 처리 직후(QUIET_PERIOD_MS 이내): 모든 오류 알림을 숨긴다 (동시에 실패한 요청들)
 * - 그 뒤 다시 로그인하기 전까지: 401 만 숨긴다 (만료된 세션의 잔여 요청). 다른 오류는 정상 안내한다
 */
export const isSessionExpiryNoise = (error: unknown): boolean => {
  if (handledAt === null) return false;
  if (Date.now() - handledAt < QUIET_PERIOD_MS) return true;
  return getErrorPresentation(error).httpStatus === 401;
};

/**
 * 세션 만료 처리. axios 인터셉터처럼 React 밖에서도 호출할 수 있다.
 * 여러 요청이 동시에 갱신에 실패해 여러 번 호출돼도 안내와 이동은 처음 한 번만 한다.
 * clearUser 는 멱등이므로 매번 호출한다 (처리 후 다시 저장된 토큰이 있어도 확실히 지운다).
 *
 * 안내 문구: BE 가 4xx 메시지를 보냈으면(예: AUTH_REFRESH_TOKEN_INVALID) 그 메시지,
 * 아니면(네트워크 오류, 5xx, Refresh 토큰 없음) ERROR_MESSAGES.SESSION_EXPIRED.
 *
 * @returns 이번 호출에서 실제로 처리했으면 true
 */
export const handleSessionExpired = (error?: unknown): boolean => {
  useUserStore.getState().clearUser();
  if (handledAt !== null) return false;
  handledAt = Date.now();

  const presentation = getErrorPresentation(error);
  const message = presentation.category === 'guide' && presentation.serverMessage ? presentation.serverMessage : ERROR_MESSAGES.SESSION_EXPIRED;
  useToastStore.getState().add({ type: 'warning', message, duration: TOAST_DURATION_MS });
  navigator(LOGIN_PATH);
  return true;
};
