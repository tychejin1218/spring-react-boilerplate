---
paths:
  - "react-boilerplate/src/**/pages/**"
  - "react-boilerplate/src/**/components/**"
  - "react-boilerplate/src/shared/hooks/**"
  - "react-boilerplate/src/features/*/hooks/**"
  - "src/**/pages/**"
  - "src/**/components/**"
  - "src/shared/hooks/**"
  - "src/features/*/hooks/**"
---

# 컴포넌트와 폼 규칙

## 컴포넌트 구조

- 함수형 컴포넌트 + 화살표 함수 사용
- Props 타입은 `interface Props { ... }` 로 정의 (파일 상단, export 불필요)
- 기본 export: `export default ComponentName`
- 컴포넌트 파일은 `.tsx`, 순수 로직 파일은 `.ts`

```tsx
// 올바른 컴포넌트 구조
interface Props {
  todo: Todo;
  onComplete: (id: number) => void;
}

const TodoItem = ({ todo, onComplete }: Props) => {
  return <li>{todo.title}</li>;
};

export default TodoItem;
```

## 공통 UI, 폼, 오류 처리

- 버튼·입력은 `shared/components/ui/` 의 `Button`(variant/size/loading), `Input`, `FormField`(label + 에러 `role="alert"`)를 사용한다. 빈 목록은 `EmptyState`
- 인증 화면은 `features/auth/components/AuthCard.tsx` 로 감싼다
- 폼은 `useForm` + `zodResolver(schema)`. 스키마는 피처의 `types/` 에 두고 (`auth/types/authForm.ts`), 서버 에러는 `setError('root', ...)` 로 표시한다
- 전역 API 에러(403/5xx)는 `instance.ts` 인터셉터가, 그 밖의 조회/변경 오류는 `QueryProvider` 의 QueryCache/MutationCache 가 토스트로 알리므로 화면에서 중복 토스트를 띄우지 않는다. 화면이 오류를 직접 그리는 쿼리/mutation 은 `meta: { skipErrorNotify: true }` 로 공통 알림에서 뺀다. 429 는 해당 화면이 직접 처리한다
- 오류 문구는 `getErrorMessage(err, '화면별 실패 문구')` (`shared/utils/apiError.ts`) 로 만든다. `err.message` 를 그대로 쓰면 axios 영문 메시지가 노출된다
- 세션 만료(토큰 갱신 실패)는 `shared/utils/sessionExpired.ts` 가 한 번만 처리한다 (토스트 + 로그인 페이지 이동). 로그인 성공 시 `useLoginSuccess` 가 가드를 초기화한다
- 작성 중인 폼에서 벗어날 때 확인이 필요하면 `useLeaveConfirm(formState.isDirty)` 를 쓰고, 저장 성공 후 이동은 `leaveWithoutConfirm(() => navigate(...))` 로 감싼다 (예: `RegisterPage`)
