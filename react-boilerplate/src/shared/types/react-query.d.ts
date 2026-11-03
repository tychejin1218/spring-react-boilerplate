/**
 * TanStack Query 전역 타입 등록 (Register 인터페이스 모듈 보강)
 * useQuery / useMutation 의 meta 에 아래 키를 타입 안전하게 쓸 수 있다.
 */
export {};

/** 공통 오류 알림 제어용 meta */
type ErrorNotifyMeta = {
  /**
   * true 면 QueryCache/MutationCache 의 공통 오류 토스트를 띄우지 않는다.
   * 화면이 오류를 직접 그리는 경우(인라인 에러·재시도 UI, SuspenseBoundary, setError)에 쓴다.
   * 403/5xx 는 instance.ts 인터셉터가 별도로 알리므로 이 값과 관계없이 토스트가 뜬다.
   */
  skipErrorNotify?: boolean;
};

declare module '@tanstack/react-query' {
  interface Register {
    queryMeta: ErrorNotifyMeta;
    mutationMeta: ErrorNotifyMeta;
  }
}
