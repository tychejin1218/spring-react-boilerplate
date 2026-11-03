import { Suspense, type ReactNode } from 'react';
import { ErrorBoundary as ReactErrorBoundary } from 'react-error-boundary';
import type { FallbackProps } from 'react-error-boundary';
import { QueryErrorResetBoundary } from '@tanstack/react-query';
import LoadingSpinner from '@/shared/components/ui/LoadingSpinner';
import { getErrorMessage } from '@/shared/utils/apiError';

const DefaultErrorFallback = ({ error, resetErrorBoundary }: FallbackProps) => (
  <div className="flex min-h-50 flex-col items-center justify-center gap-4">
    <p className="text-gray-500">{getErrorMessage(error, '데이터를 불러오지 못했습니다.')}</p>
    <button className="rounded bg-blue-600 px-4 py-2 text-sm text-white" onClick={resetErrorBoundary}>
      다시 시도
    </button>
  </div>
);

interface SuspenseBoundaryProps {
  children: ReactNode;
  pendingFallback?: ReactNode;
  errorFallback?: React.ComponentType<FallbackProps>;
}

/**
 * lazy 컴포넌트와 useSuspenseQuery 의 로딩/에러를 처리하는 경계.
 * 이 경계가 에러 화면(재시도 버튼)을 직접 그리므로, 아래에서 쓰는 useSuspenseQuery 는
 * meta: { skipErrorNotify: true } 를 지정해 QueryCache 공통 토스트와 이중으로 안내되지 않게 한다.
 */
const SuspenseBoundary = ({ children, pendingFallback, errorFallback: ErrorFallbackComponent = DefaultErrorFallback }: SuspenseBoundaryProps) => (
  <QueryErrorResetBoundary>
    {({ reset }) => (
      <ReactErrorBoundary onReset={reset} FallbackComponent={ErrorFallbackComponent}>
        <Suspense fallback={pendingFallback ?? <LoadingSpinner />}>{children}</Suspense>
      </ReactErrorBoundary>
    )}
  </QueryErrorResetBoundary>
);

export default SuspenseBoundary;
