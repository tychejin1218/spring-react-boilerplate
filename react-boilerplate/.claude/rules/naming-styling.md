---
paths:
  - "react-boilerplate/src/**"
  - "src/**"
---

# 이름과 스타일 규칙

## 파일 이름

| 종류 | 규칙 | 예시 |
|------|------|------|
| 컴포넌트, 페이지 | PascalCase `.tsx`, 페이지는 `Page` 로 끝냄 | `TodoItem.tsx`, `LoginPage.tsx` |
| 훅 | `use` + PascalCase `.ts` | `useTodos.ts`, `useLeaveConfirm.ts` |
| Zustand 스토어 | `use{Name}Store.ts` (`app/stores/`) | `useUserStore.ts` |
| API 함수 | `{feature}Api.ts` | `todoApi.ts`, `authApi.ts` |
| 타입, zod 스키마 | 도메인 이름 camelCase `.ts` | `todo.ts`, `authForm.ts` |
| MSW 핸들러 | `{feature}Handlers.ts` | `todoHandlers.ts` |
| 유틸 | 동작 이름 camelCase `.ts` | `apiError.ts`, `sessionExpired.ts` |

## 함수 이름과 export

- API 함수는 HTTP 메서드 + 리소스: `getTodos`, `postTodo`, `patchTodoComplete`, `deleteTodo` (BE 의 `getXXXList`/`insertXXX` 규칙과 다르다)
- Query 훅은 `use` + 동작: 조회 `useTodos`, 변경 `useCreateTodo`, `useDeleteTodo`
- 컴포넌트와 페이지는 `export default`. 훅, API 함수, 유틸, 스토어, 타입은 named export (`export const`)
- 상수 객체는 UPPER_SNAKE_CASE (`API_STATUS_CODES`, `CONFIRM_MESSAGES`), 화면 문구는 `shared/constants/messages.ts` 에 모은다

## 스타일 (Tailwind CSS 4)

- 스타일은 Tailwind 유틸리티 클래스를 `className` 에 직접 쓴다. CSS 파일은 전역 설정(`src/index.css`)만 둔다
- 변형이 있는 컴포넌트는 `Record<variant, string>` 맵으로 클래스를 고른다 (`Button.tsx` 의 `VARIANT_MAP`, `SIZE_MAP`)
- 공통 컴포넌트는 `className` 을 받아 뒤에 덧붙여 화면에서 여백 정도만 조정하게 한다
- 색은 gray(본문, 테두리), blue-600(주요 동작), red(오류, 삭제) 계열을 쓴다. 새 색을 쓰기 전에 공통 컴포넌트에 같은 용도가 있는지 본다
- 테마는 라이트로 고정되어 있다 (`index.css` 의 `color-scheme: light`). `dark:` 클래스를 일부에만 넣지 않는다
- 버튼·입력·빈 목록은 직접 스타일하지 말고 `shared/components/ui/` 컴포넌트를 쓴다

## 접근성

- 입력은 `FormField` 로 감싸 label 과 연결하고, 오류 문구는 `role="alert"` 로 표시된다
- `Button` 의 기본 `type` 은 `"button"` 이다. 폼 제출 버튼만 `type="submit"` 을 준다
- 아이콘만 있는 버튼에는 `aria-label` 을 준다
- 테스트 selector 가 `getByRole` 을 우선하므로, 클릭할 요소는 `div` 대신 `button`/`a` 를 쓴다
