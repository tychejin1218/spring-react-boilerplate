import { useEffect } from 'react';
import { createPortal } from 'react-dom';
import { useToastStore } from '@/app/stores/useToastStore';
import type { ToastItem } from '@/app/stores/useToastStore';

// 타입별 스타일
const STYLE_MAP: Record<ToastItem['type'], string> = {
  success: 'bg-green-600',
  error: 'bg-red-600',
  info: 'bg-blue-600',
  warning: 'bg-amber-500',
};

/**
 * 개별 토스트 렌더러 — 자동 소멸 타이머 포함
 */
const ToastElement = ({ toast }: { toast: ToastItem }) => {
  const remove = useToastStore((s) => s.remove);

  // 자동 소멸
  useEffect(() => {
    if (!toast.duration) return;
    const timer = setTimeout(() => remove(toast.id), toast.duration);
    return () => clearTimeout(timer);
  }, [toast.id, toast.duration, remove]);

  return (
    <div role="status" className={`${STYLE_MAP[toast.type]} flex items-center gap-2 rounded px-4 py-3 text-sm text-white shadow-lg`}>
      <span className="flex-1">{toast.message}</span>
      <button className="ml-2 text-white/80 hover:text-white" onClick={() => remove(toast.id)}>
        ×
      </button>
    </div>
  );
};

/**
 * 토스트 렌더러 — App에 한 번만 배치
 * 우상단 고정, 스택 구조로 여러 토스트 표시
 */
const ToastRenderer = () => {
  const toasts = useToastStore((s) => s.toasts);

  if (toasts.length === 0) return null;

  return createPortal(
    // 상단 바(약 56px) 아래에 표시해 로그아웃 버튼 등 헤더 요소를 가리지 않게 한다
    <div className="fixed top-16 right-4 z-50 flex flex-col gap-2">
      {toasts.map((toast) => (
        <ToastElement key={toast.id} toast={toast} />
      ))}
    </div>,
    document.body,
  );
};

export default ToastRenderer;
