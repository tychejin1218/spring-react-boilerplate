---
paths:
  - "react-boilerplate/src/**/*.test.ts"
  - "react-boilerplate/src/**/*.test.tsx"
  - "react-boilerplate/e2e/**"
  - "src/**/*.test.ts"
  - "src/**/*.test.tsx"
  - "e2e/**"
---

# Testing Rules

## 단위 테스트 (Vitest)

- 위치: 테스트 대상 파일과 같은 디렉토리에 co-location (파일명: `{name}.test.ts(x)`)
  - 예: `src/features/todo/hooks/useTodos.ts` -> `src/features/todo/hooks/useTodos.test.ts`
  - 예: `src/features/auth/pages/PasswordResetPage.tsx` -> `src/features/auth/pages/passwordReset.test.tsx` (여러 페이지를 묶어 테스트할 때는 공통 이름 사용)
  - 별도 `src/__tests__/` 디렉토리는 사용하지 않는다
- 환경: jsdom, vitest/globals
- 설정: `vite.config.ts` 의 `test` 항목 (별도 `vitest.config.ts` 없음)
- MSW 서버: `src/test/setup.ts`에서 전역 설정됨 (테스트 파일에서 중복 설정 불필요)

## E2E 테스트 (Playwright)

- 위치: `e2e/` 디렉토리
- 설정: `playwright.config.ts` (Chromium only, `tsconfig.e2e.json`으로 타입 분리)
- 헬퍼: 반복되는 로직은 `e2e/helpers/`에 추출 (예: `auth.ts`의 `login(page)`). 현재는 `e2e/todo.spec.ts` 한 개만 있어 헬퍼 디렉토리가 아직 없음
- 공통 Dialog 로 확인받는 동작(예: 삭제)은 `page.getByRole('dialog').getByRole('button', { name: ... })` 로 확인 버튼까지 눌러야 완료된다
- API 모킹: `page.route()` 사용 (MSW가 아닌 Playwright 네이티브)

## 공통 규칙

- 테스트 설명: 한국어로 작성 (예: `it('초기 상태는 null이다', ...)`)
- import 경로: `@/` alias 사용
- selector 우선순위: `getByRole` > `getByText` > `getByPlaceholder` > `getByTestId`
