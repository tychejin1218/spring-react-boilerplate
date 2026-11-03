import { ErrorBoundary as ReactErrorBoundary } from 'react-error-boundary';
import type { FallbackProps } from 'react-error-boundary';
import type { ReactNode } from 'react';
import { getErrorMessage } from '@/shared/utils/apiError';

// 오류 원문(Error.message, axios 영문 메시지)은 콘솔에만 남기고 화면에는 안내 문구만 보여 준다
export const ErrorFallback = ({ error, resetErrorBoundary }: FallbackProps) => {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center gap-4">
      <h1 className="text-2xl font-bold">오류가 발생했습니다</h1>
      <p className="text-gray-500">{getErrorMessage(error)}</p>
      <button className="rounded bg-blue-600 px-4 py-2 text-white" onClick={resetErrorBoundary}>
        다시 시도
      </button>
    </div>
  );
};

export const ErrorBoundary = ({ children }: { children: ReactNode }) => {
  return (
    <ReactErrorBoundary
      FallbackComponent={ErrorFallback}
      onError={(error, info) => {
        console.error('[ErrorBoundary]', error, info);
      }}
    >
      {children}
    </ReactErrorBoundary>
  );
};

export default ErrorBoundary;
