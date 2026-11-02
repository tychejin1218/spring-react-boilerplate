import type { ReactNode } from 'react';

interface Props {
  message: string;
  /** 빈 상태에서 유도할 동작 (예: 추가 버튼) */
  action?: ReactNode;
}

/**
 * 빈 목록 안내
 * - 목록 데이터가 0건일 때 메시지와 선택적인 액션을 표시한다
 */
const EmptyState = ({ message, action }: Props) => (
  <div className="flex flex-col items-center gap-3 py-8 text-center">
    <p className="text-sm text-gray-400">{message}</p>
    {action}
  </div>
);

export default EmptyState;
