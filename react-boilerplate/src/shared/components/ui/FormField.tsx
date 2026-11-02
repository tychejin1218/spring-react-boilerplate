import { cloneElement, isValidElement, type ReactElement } from 'react';

interface ChildProps {
  id?: string;
  invalid?: boolean;
  'aria-describedby'?: string;
}

interface Props {
  /** 연결할 입력 요소의 id (label 의 htmlFor 와 자식 input 의 id 로 쓰인다) */
  id: string;
  label: string;
  /** 검증 에러 메시지 (react-hook-form 의 `errors.xxx?.message`) */
  error?: string;
  children: ReactElement<ChildProps>;
}

/**
 * 폼 필드 래퍼 (label + 입력 + 에러 메시지)
 * - label 의 htmlFor 와 자식 입력의 id 를 연결한다
 * - error 가 있으면 role="alert" 메시지를 표시하고, 자식에 invalid + aria-describedby 를 주입한다
 */
const FormField = ({ id, label, error, children }: Props) => {
  const errorId = `${id}-error`;
  const child = isValidElement(children) ? cloneElement(children, { id, invalid: !!error, 'aria-describedby': error ? errorId : undefined }) : children;

  return (
    <div className="flex flex-col gap-1">
      <label htmlFor={id} className="text-sm font-medium text-gray-700">
        {label}
      </label>
      {child}
      {error && (
        <p id={errorId} role="alert" className="text-sm text-red-500">
          {error}
        </p>
      )}
    </div>
  );
};

export default FormField;
