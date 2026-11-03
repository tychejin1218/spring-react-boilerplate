import type { Mutation, Query } from '@tanstack/react-query';
// 예외: 전역 에러 토스트(cross-cutting concern)로 app 레이어 참조. 컴포넌트 밖이므로 getState() 로 호출
import { useToastStore } from '@/app/stores/useToastStore';
import { getErrorPresentation } from '@/shared/utils/apiError';
import { isSessionExpiryNoise } from '@/shared/utils/sessionExpired';

/** 같은 문구의 알림을 다시 띄우지 않는 간격 (여러 요청/쿼리가 동시에 실패할 때 중복 방지) */
const DEDUPE_MS = 2000;
const TOAST_DURATION_MS = 3000;

/**
 * 이미 알린 오류 객체. instance.ts(403/5xx)가 알린 오류가 QueryCache/MutationCache 로 다시 들어와도
 * 같은 객체이므로 두 번 알리지 않는다. 오류 객체가 사라지면 함께 사라지도록 WeakSet 을 쓴다.
 */
let notifiedErrors = new WeakSet<object>();
const lastShownAt = new Map<string, number>();

/**
 * API 오류 공통 알림 (에러 토스트). React 밖(axios 인터셉터, QueryCache)에서 호출한다.
 *
 * 다음은 알리지 않는다.
 * - 세션 만료 때문에 생긴 오류 (세션 만료 안내 하나만 남긴다)
 * - 이미 알린 오류 객체 (instance.ts 와 QueryCache 이중 알림 방지)
 * - DEDUPE_MS 안에 같은 문구를 이미 알린 경우
 *
 * @returns 실제로 토스트를 띄웠으면 true
 */
export const notifyApiError = (error: unknown): boolean => {
  if (isSessionExpiryNoise(error)) return false;

  if (typeof error === 'object' && error !== null) {
    if (notifiedErrors.has(error)) return false;
    notifiedErrors.add(error);
  }

  const { message } = getErrorPresentation(error);
  const now = Date.now();
  const last = lastShownAt.get(message);
  if (last !== undefined && now - last < DEDUPE_MS) return false;
  lastShownAt.set(message, now);

  useToastStore.getState().add({ type: 'error', message, duration: TOAST_DURATION_MS });
  return true;
};

/**
 * QueryCache.onError 콜백. retry 를 모두 소진한 최종 실패에 쿼리당 한 번 호출된다.
 * 화면이 직접 에러 UI 를 그리는 쿼리(SuspenseBoundary 아래 useSuspenseQuery, 인라인 재시도 UI 등)는
 * meta: { skipErrorNotify: true } 로 제외한다.
 */
export const notifyQueryError = (error: unknown, query: Query<unknown, unknown, unknown>) => {
  if (query.meta?.skipErrorNotify) return;
  notifyApiError(error);
};

/**
 * MutationCache.onError 콜백. 화면이 setError 등으로 직접 안내하는 mutation 은 meta: { skipErrorNotify: true } 로 제외한다.
 */
export const notifyMutationError = (error: unknown, _variables: unknown, _onMutateResult: unknown, mutation: Mutation<unknown, unknown, unknown, unknown>) => {
  if (mutation.meta?.skipErrorNotify) return;
  notifyApiError(error);
};

/** 테스트 전용: 중복 방지 상태 초기화 */
export const resetApiErrorNotifyState = () => {
  notifiedErrors = new WeakSet<object>();
  lastShownAt.clear();
};
