---
name: fe-e2e-gen
description: react-boilerplate 페이지의 Playwright E2E 테스트 코드를 자동 생성하고 실행까지 검증합니다.
argument-hint: "<page-name>"
disable-model-invocation: true
allowed-tools: Read, Write, Bash, Glob, Grep, Edit, Agent, AskUserQuestion
---

# /fe-e2e-gen — Playwright E2E 테스트 코드 자동 생성 스킬

모든 경로는 `react-boilerplate/` 기준이며, 명령은 `react-boilerplate/` 디렉토리에서 실행한다.

사용자가 `/fe-e2e-gen <page-name>` 형태로 실행하면, 해당 페이지를 분석하여 프로젝트 컨벤션에 맞는 E2E 테스트 코드를 자동 생성하고 실행까지 검증한다.

---

## 실행 전 확인

- 인자로 전달된 페이지명이 비어 있으면 `"사용법: /fe-e2e-gen <page-name> (예: /fe-e2e-gen home, /fe-e2e-gen login)"` 안내 후 종료한다.

---

## 단계 1: 페이지 탐색 및 분석

1. **페이지 파일 탐색**: `src/**/pages/**/*Page.tsx`를 Glob으로 검색하여 인자와 매칭되는 페이지를 찾는다.
   - 탐색 대상: `src/app/pages/`, `src/features/*/pages/`
   - 매칭 규칙: 인자를 PascalCase로 변환 + `Page` 접미사 (예: `home` → `HomePage`, `login` → `LoginPage`, `test/api` → `ApiTestPage`)
   - 매칭되는 파일이 없으면 발견된 페이지 목록을 보여주고 종료한다.

2. **페이지 컴포넌트 읽기**: 매칭된 페이지 파일을 Read로 읽어 구조를 파악한다.
   - 사용된 컴포넌트, 이벤트 핸들러, 상태 관리, API 호출 등

3. **라우터 분석**: `src/app/router/router.tsx`를 Read로 읽어 다음을 파악한다.
   - 페이지의 라우트 경로 (예: `/home`, `/test/pdf`)
   - `AuthRoute` 하위 여부 (인증 필요 여부)
   - 중첩 라우트 구조

4. **기존 E2E 파일 확인**: `e2e/` 디렉토리에 이미 해당 페이지의 spec 파일이 있는지 확인한다.
   - 있으면 사용자에게 덮어쓸지 확인한다.

5. **기존 헬퍼 확인**: `e2e/helpers/` 디렉토리가 있으면 기존 헬퍼를 확인한다. 없으면 인증 로직은 기존 spec(`e2e/todo.spec.ts`)의 패턴을 따르고, 반복되면 `e2e/helpers/auth.ts`에 `login(page)`를 추출한다

---

## 단계 2: 시나리오 선택

분석 결과를 바탕으로 해당 페이지에 적합한 시나리오 목록을 구성한 뒤, AskUserQuestion으로 사용자에게 어떤 시나리오를 포함할지 선택하게 한다.

### 시나리오 후보

| 시나리오 | 조건 | 설명 |
|----------|------|------|
| 페이지 렌더링 | 항상 | 페이지 접근 후 주요 요소 표시 확인 |
| 인증 가드 | AuthRoute 하위일 때 | 미인증 상태 접근 시 리다이렉트 확인 |
| 버튼 인터랙션 | 버튼/onClick 있을 때 | 버튼 클릭 후 결과 확인 |
| API 데이터 표시 | API 호출이 있을 때 | page.route()로 응답 모킹 후 데이터 표시 확인 |
| API 에러 처리 | API 호출이 있을 때 | 500 응답 모킹 후 에러 상태 확인 |
| 폼 제출 | form/input 있을 때 | 입력 후 제출 → 결과 확인 |
| 페이지 이동 | 링크/navigate 있을 때 | 클릭 후 URL 변경 확인 |

AskUserQuestion 사용 시:
- `multiSelect: true`로 여러 시나리오를 선택할 수 있도록 한다.
- 페이지에 해당하지 않는 시나리오는 목록에서 제외한다.
- "페이지 렌더링"은 기본 포함으로 안내한다.

---

## 단계 3: 테스트 코드 생성

선택된 시나리오에 따라 테스트 코드를 작성한다. **아래 공통 규칙과 시나리오별 템플릿을 반드시 준수한다.**

### 공통 규칙

- **import**: `import { test, expect } from '@playwright/test';`
- **한국어 테스트 설명**: `test('페이지가 표시된다', ...)`
- **`test.describe()`로 그룹핑**: 시나리오 카테고리별 그룹
- **selector 우선순위**: `getByRole` > `getByText` > `getByPlaceholder` > `getByTestId`
- **Prettier 포맷**: printWidth 160, single quote, trailing comma `all`, semicolon 사용
- **인증 헬퍼**: 인증 필요 페이지는 `e2e/helpers/auth.ts`의 `login(page)`를 사용한다 (없으면 단계 4에서 생성)
- **API 모킹**: Playwright 네이티브 `page.route()` 사용 (MSW가 아님)
- **응답 형식**: BE `BaseResponse` 구조 `{ statusCode, message, data }`, 에러는 `{ statusCode, message, method, path, timestamp }`

### 시나리오별 템플릿

#### 페이지 렌더링

```ts
test.describe('페이지 렌더링', () => {
  test('주요 요소가 표시된다', async ({ page }) => {
    await page.goto('/path');

    await expect(page.getByRole('heading', { name: '제목' })).toBeVisible();
    await expect(page.getByRole('button', { name: '버튼명' })).toBeVisible();
  });
});
```

#### 인증 가드

인증 가드 테스트는 `login()` beforeEach 밖의 **별도 describe 블록**에 배치한다.

```ts
test.describe('인증 가드', () => {
  test('미인증 상태에서 접근 시 /로 리다이렉트된다', async ({ page }) => {
    await page.goto('/protected-path');

    await expect(page).toHaveURL('/');
  });
});
```

#### 버튼 인터랙션

```ts
test.describe('버튼 인터랙션', () => {
  test.beforeEach(async ({ page }) => {
    await login(page); // 인증 필요 시
  });

  test('버튼 클릭 시 동작한다', async ({ page }) => {
    await page.goto('/path');

    await page.getByRole('button', { name: '버튼명' }).click();
    // 결과 assertion
  });
});
```

#### API 데이터 표시

```ts
test.describe('API 데이터 표시', () => {
  test.beforeEach(async ({ page }) => {
    await login(page); // 인증 필요 시
  });

  test('API 응답 데이터가 화면에 표시된다', async ({ page }) => {
    await page.route('**/api/endpoint', (route) => {
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          statusCode: 'OK',
          message: '성공',
          data: [{ id: 1, name: '항목 1' }],
        }),
      });
    });

    await page.goto('/path');

    await expect(page.getByText('항목 1')).toBeVisible();
  });
});
```

#### API 에러 처리

```ts
test.describe('API 에러 처리', () => {
  test.beforeEach(async ({ page }) => {
    await login(page); // 인증 필요 시
  });

  test('API 에러 시 에러 상태가 표시된다', async ({ page }) => {
    await page.route('**/api/endpoint', (route) => {
      route.fulfill({
        status: 500,
        contentType: 'application/json',
        body: JSON.stringify({
          statusCode: 'SYSTEM_INTERNAL_ERROR',
          message: '내부 오류가 발생했습니다.',
          method: 'GET',
          path: '/api/endpoint',
          timestamp: '20260101000000',
        }),
      });
    });

    await page.goto('/path');

    // 에러 상태 확인 (에러 메시지, 재시도 버튼 등)
  });
});
```

#### 폼 제출

```ts
test.describe('폼 제출', () => {
  test.beforeEach(async ({ page }) => {
    await login(page); // 인증 필요 시
  });

  test('폼을 입력하고 제출한다', async ({ page }) => {
    await page.goto('/path');

    await page.getByPlaceholder('입력 힌트').fill('입력값');
    await page.getByRole('button', { name: '제출' }).click();

    // 결과 확인 (성공 메시지, URL 변경 등)
  });
});
```

#### 페이지 이동

```ts
test.describe('페이지 이동', () => {
  test.beforeEach(async ({ page }) => {
    await login(page); // 인증 필요 시
  });

  test('링크 클릭 시 해당 페이지로 이동한다', async ({ page }) => {
    await page.goto('/path');

    await page.getByRole('link', { name: '링크 텍스트' }).click();

    await expect(page).toHaveURL('/target-path');
  });
});
```

---

## 단계 4: 파일 작성

1. **파일명 결정**:
   - 기본: `e2e/{pageName}.spec.ts` (예: `home.spec.ts`, `login.spec.ts`)
   - nested 경로: hyphen으로 flatten (예: `test/api` → `test-api.spec.ts`, `test/pdf` → `test-pdf.spec.ts`)

2. **기존 파일 확인**: 동일 경로에 spec 파일이 이미 있으면 사용자에게 덮어쓸지 확인한다.

3. **Write 도구로 파일 작성**: 테스트 코드를 Write 도구로 저장한다.

4. **(선택) 헬퍼 생성**: 반복되는 로직이 있고 기존 헬퍼에 없는 경우 `e2e/helpers/`에 헬퍼 함수를 추출한다.
   - `Page` 인자를 받는 함수로 작성
   - 기존 spec의 인증 처리 패턴을 참고

---

## 단계 5: 검증

1. **테스트 실행**: `react-boilerplate/` 디렉토리에서 `npx playwright test e2e/{specFileName}` 으로 해당 테스트만 실행한다 (루트 세션이면 `cd react-boilerplate &&` 를 앞에 붙인다).
2. **결과 확인**:
   - **성공**: 통과한 테스트 수를 안내하고 종료한다.
   - **실패**: 에러 메시지를 분석하여 테스트 코드를 수정한다.
3. **재시도**: 실패 시 최대 **3회**까지 수정 → 재실행을 반복한다.
4. **3회 실패**: 수정하지 못한 에러를 사용자에게 안내하고 종료한다.

---

## 출력 형식

작업 완료 후 아래 형식으로 요약한다:

```
E2E 테스트 생성 완료

- 페이지: src/app/pages/HomePage.tsx
- 라우트: /home (인증 필요)
- 테스트: e2e/home.spec.ts
- 시나리오: 페이지 렌더링, 인증 가드, 버튼 인터랙션
- 결과: 3/3 통과
```
