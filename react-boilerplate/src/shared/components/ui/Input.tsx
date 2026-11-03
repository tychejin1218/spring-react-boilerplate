import type { InputHTMLAttributes, Ref } from 'react';

interface Props extends InputHTMLAttributes<HTMLInputElement> {
  /** true 면 에러 스타일과 aria-invalid 를 적용한다 */
  invalid?: boolean;
  ref?: Ref<HTMLInputElement>;
}

/**
 * 공통 텍스트 입력
 * - React 19 의 ref-as-prop 으로 ref 를 전달하므로 react-hook-form 의 `{...register('name')}` 을 그대로 펼쳐 쓸 수 있다
 * - invalid 면 빨간 테두리 + aria-invalid 를 적용한다 (FormField 안에서는 FormField 가 자동 주입)
 */
const Input = ({ invalid = false, className = '', ref, ...rest }: Props) => (
  <input
    ref={ref}
    aria-invalid={invalid || undefined}
    className={`rounded border bg-white px-3 py-2 text-sm text-gray-900 placeholder:text-gray-400 focus:outline-none ${
      invalid ? 'border-red-500 focus:border-red-500' : 'border-gray-300 focus:border-blue-500'
    } ${className}`}
    {...rest}
  />
);

export default Input;
