import { MutationCache, QueryCache, QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ReactQueryDevtools } from '@tanstack/react-query-devtools';
import type { ReactNode } from 'react';
import { env } from '@/app/config/env';
import { notifyMutationError, notifyQueryError } from '@/shared/utils/notifyApiError';

const queryClient = new QueryClient({
  /**
   * 조회/변경 오류 공통 알림. 호출부마다 onError 를 배선하지 않아도 오류가 한 번씩 안내된다.
   * - 여러 쿼리가 동시에 실패해도 같은 문구는 한 번만 표시 (notifyApiError 중복 방지)
   * - 403/5xx 는 instance.ts 인터셉터가 이미 알렸으므로 다시 알리지 않음
   * - 세션 만료로 생긴 오류는 알리지 않음 (세션 만료 안내 하나만 표시)
   * - 화면이 직접 오류를 그리면 meta: { skipErrorNotify: true } 로 제외
   */
  queryCache: new QueryCache({ onError: notifyQueryError }),
  mutationCache: new MutationCache({ onError: notifyMutationError }),
  defaultOptions: {
    queries: {
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
});

const QueryProvider = ({ children }: { children: ReactNode }) => {
  return (
    <QueryClientProvider client={queryClient}>
      {children}
      {env.VITE_APP_ENV !== 'prd' && <ReactQueryDevtools initialIsOpen={false} />}
    </QueryClientProvider>
  );
};

export default QueryProvider;
