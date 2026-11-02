import type { ButtonHTMLAttributes } from 'react';
import LoadingSpinner from '@/shared/components/ui/LoadingSpinner';

interface Props extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary' | 'danger' | 'ghost';
  size?: 'sm' | 'md';
  /** true 면 스피너를 표시하고 버튼을 비활성화한다 */
  loading?: boolean;
}

const VARIANT_MAP: Record<NonNullable<Props['variant']>, string> = {
  primary: 'bg-blue-600 text-white hover:bg-blue-700',
  secondary: 'border border-gray-300 bg-white text-gray-700 hover:bg-gray-50',
  danger: 'bg-red-600 text-white hover:bg-red-700',
  ghost: 'text-gray-500 hover:bg-gray-100 hover:text-gray-700',
};

const SIZE_MAP: Record<NonNullable<Props['size']>, string> = {
  sm: 'px-3 py-1 text-xs',
  md: 'px-4 py-2 text-sm',
};

/**
 * 공통 버튼
 * - variant: primary(기본) / secondary / danger / ghost, size: sm / md(기본)
 * - loading 이면 스피너를 보여주고 disabled 처리한다 (버튼 텍스트는 children 그대로 유지)
 * - type 기본값은 "button" (폼 안에서 의도치 않은 submit 방지). 제출 버튼은 type="submit" 을 명시한다
 * - 나머지 native button 속성은 그대로 전달된다
 */
const Button = ({ variant = 'primary', size = 'md', loading = false, disabled, type = 'button', className = '', children, ...rest }: Props) => (
  <button
    type={type}
    disabled={disabled || loading}
    aria-busy={loading || undefined}
    className={`inline-flex items-center justify-center gap-2 rounded font-medium transition-colors disabled:cursor-not-allowed disabled:opacity-50 ${VARIANT_MAP[variant]} ${SIZE_MAP[size]} ${className}`}
    {...rest}
  >
    {loading && <LoadingSpinner size="sm" fullScreen={false} />}
    {children}
  </button>
);

export default Button;
