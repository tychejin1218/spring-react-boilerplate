---
paths:
  - "react-boilerplate/src/__tests__/**"
  - "react-boilerplate/e2e/**"
  - "src/__tests__/**"
  - "e2e/**"
---

# Testing Rules

## 단위 테스트 (Vitest)

- 위치: `src/__tests__/` 디렉토리 (파일명: `{name}.test.ts(x)`)
- 환경: jsdom, vitest/globals
- 설정: `vitest.config.ts`
- MSW 서버: `src/test/setup.ts`에서 전역 설정됨 (테스트 파일에서 중복 설정 불필요)

## E2E 테스트 (Playwright)

- 위치: `e2e/` 디렉토리
- 설정: `playwright.config.ts` (Chromium only, `tsconfig.e2e.json`으로 타입 분리)
- 헬퍼: 반복되는 로직은 `e2e/helpers/`에 추출 (예: `auth.ts`의 `login(page)`). 현재는 `e2e/todo.spec.ts` 한 개만 있음
- API 모킹: `page.route()` 사용 (MSW가 아닌 Playwright 네이티브)

## 공통 규칙

- 테스트 설명: 한국어로 작성 (예: `it('초기 상태는 null이다', ...)`)
- import 경로: `@/` alias 사용
- selector 우선순위: `getByRole` > `getByText` > `getByPlaceholder` > `getByTestId`
