---
name: fe-vitest-gen
description: react-boilerplate 소스 파일의 Vitest 단위 테스트 코드를 자동 생성하고 실행까지 검증합니다.
argument-hint: "[src/path/to/file.ts(x)]"
allowed-tools: Read, Write, Bash, Glob, Grep, Edit, Agent
---

# FE Vitest 테스트 코드 생성

모든 경로는 `react-boilerplate/` 기준이며, 명령은 `react-boilerplate/` 디렉토리에서 실행한다.

사용자가 `/fe-vitest-gen <파일경로>` 형태로 실행하면, 대상 파일을 분석하여 프로젝트 컨벤션에 맞는 테스트 코드를 자동 생성하고 실행까지 검증한다.

---

## 실행 전 확인

- 인자로 전달된 파일 경로가 비어 있으면 `"사용법: /fe-vitest-gen src/path/to/file.ts(x)"` 안내 후 종료한다.
- 파일이 존재하지 않으면 에러 메시지 후 종료한다.

---

## 단계 1: 분석

1. **대상 파일 읽기**: 인자로 받은 소스 파일을 Read로 읽는다.
2. **분류**: 파일 경로와 내용으로 아래 중 하나로 분류한다.

| 분류 | 판별 기준 |
|------|-----------|
| `component` | `src/**/components/` 또는 `src/**/pages/` 하위, JSX 반환 |
| `hook` | `src/shared/hooks/` 또는 `src/features/*/hooks/` 하위, `use` 접두사 함수 export |
| `store` | `src/app/stores/` 하위, zustand `create` 사용 |
| `api` | `src/shared/apis/` 또는 `src/features/*/apis/` 하위, axios/apiClient 호출 |
| `util` | `src/shared/utils/` 또는 `src/shared/lib/` 하위, 순수 함수 |
| `type` | `src/shared/types/` 또는 `src/features/*/types/` 하위, 타입/인터페이스만 선언 |

3. **type 분류인 경우**: `"타입 정의 파일은 테스트가 불필요합니다."` 안내 후 종료한다.
4. **export 목록 파악**: export된 함수, 컴포넌트, 훅, 상수 등을 수집한다.
5. **의존성 파악**: import문을 분석하여 의존하는 모듈을 파악한다 (타입, 스토어, API, 외부 라이브러리).

---

## 단계 2: 수집

분석에서 파악된 의존성에 따라 관련 파일을 읽는다.

1. **타입 파일**: import된 타입 정의 파일 (테스트 데이터 mock 작성에 필요)
2. **스토어**: 의존하는 Zustand 스토어 파일 (상태 mock에 필요)
3. **API 함수**: 의존하는 API 함수 파일 (MSW 핸들러 작성에 필요)
4. **기존 MSW 핸들러**: `src/features/{feature}/mocks/{feature}Handlers.ts` 또는 `src/mocks/fileHandlers.ts`에 관련 핸들러가 있는지 확인
5. **기존 테스트 파일**: 대상 파일명으로 `src/__tests__/` 아래에 이미 테스트가 있는지 확인
6. **기존 테스트 파일 예시**: `src/__tests__/` 아래 기존 테스트 파일 1~2개를 읽어서 프로젝트 컨벤션을 파악

---

## 단계 3: 생성

분류별 패턴에 따라 테스트 코드를 작성한다. **아래 공통 규칙과 분류별 패턴을 반드시 준수한다.**

### 공통 규칙

- **테스트 설명**: 한국어로 작성 (예: `it('초기 상태는 null이다', ...)`)
- **import 경로**: `@/` alias 사용 (예: `import { useUserStore } from '@/app/stores/useUserStore'`)
- **Vitest globals**: `describe`, `it`, `expect`, `beforeEach`, `afterEach`, `beforeAll`, `afterAll` 등을 import 없이 사용 (vitest.config에서 `globals: true` 설정됨)
- **jest-dom matchers**: `toBeInTheDocument()` 등 import 없이 사용 (`src/test/setup.ts`에서 전역 등록됨)
- **MSW server**: `src/test/setup.ts`에서 이미 `beforeAll(() => server.listen())`, `afterAll(() => server.close())` 처리됨. 테스트 파일에서 중복 작성하지 않는다. 핸들러 오버라이드가 필요한 경우에만 `server`를 import하여 `server.use()`를 사용한다.
- **Prettier 포맷**: printWidth 160, single quote, trailing comma `all`, semicolon 사용
- **type import**: 타입은 `import type { ... }` 구문으로 import

### 분류별 테스트 패턴

#### component (컴포넌트 / 페이지)

```tsx
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
// 라우터 의존 시:
import { MemoryRouter } from 'react-router-dom';
// React Query 의존 시:
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import MyComponent from '@/features/my/components/MyComponent';

// React Query 의존 시 래퍼 함수:
const createWrapper = () => {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
};

describe('MyComponent', () => {
  it('정상적으로 렌더링된다', () => {
    render(
      <MemoryRouter>
        <MyComponent />
      </MemoryRouter>,
    );
    expect(screen.getByText('예상 텍스트')).toBeInTheDocument();
  });

  it('버튼 클릭 시 동작한다', async () => {
    const user = userEvent.setup();
    render(
      <MemoryRouter>
        <MyComponent />
      </MemoryRouter>,
    );
    await user.click(screen.getByRole('button', { name: '버튼명' }));
    // assertion
  });
});
```

- `MemoryRouter`: 라우터 관련 import(`useNavigate`, `Link`, `Outlet` 등)가 있는 경우에만 감싼다.
- `QueryClientProvider`: React Query 훅(`useQuery`, `useMutation`, 또는 프로젝트 커스텀 훅)을 사용하는 경우에만 감싼다.
- `userEvent`: 사용자 인터랙션(클릭, 입력 등) 테스트 시 `@testing-library/user-event` 사용.
- 필요한 Provider만 최소한으로 감싼다.

#### store (Zustand 스토어)

```ts
import { useMyStore } from '@/app/stores/useMyStore';
import type { MyType } from '@/features/my/types/myType';

const mockData: MyType = { /* mock 데이터 */ };

describe('useMyStore', () => {
  beforeEach(() => {
    // 스토어 초기화 (clear/reset 액션 호출 또는 setState)
    useMyStore.getState().clearState();
  });

  it('초기 상태를 확인한다', () => {
    const state = useMyStore.getState();
    expect(state.someField).toBeNull();
  });

  it('액션으로 상태를 변경한다', () => {
    useMyStore.getState().setData(mockData);
    expect(useMyStore.getState().data).toEqual(mockData);
  });
});
```

- `.getState()`로 직접 접근하여 테스트 (renderHook 불필요)
- `beforeEach`에서 스토어 상태 초기화

#### hook (커스텀 훅)

```tsx
import { renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useMyHook } from '@/shared/hooks/useMyHook';

const createWrapper = () => {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
};

describe('useMyHook', () => {
  it('데이터를 성공적으로 가져온다', async () => {
    const { result } = renderHook(() => useMyHook(), {
      wrapper: createWrapper(),
    });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });

    expect(result.current.data).toBeDefined();
  });
});
```

- React Query 훅은 `QueryClientProvider` 래퍼 필수, `retry: false` 설정
- `renderHook` + `waitFor` 패턴
- React Query를 사용하지 않는 일반 훅은 래퍼 없이 `renderHook`만 사용

#### api (API 함수)

```ts
import { server } from '@/mocks/server';
import { http, HttpResponse } from 'msw';
import { ok, err } from '@/mocks/response';
import { env } from '@/app/config/env';
import { fetchItems } from '@/features/test/apis/testApi';
import type { MyItem } from '@/features/test/types/myType';

const mockItems: MyItem[] = [
  { id: 1, name: '항목 1' },
  { id: 2, name: '항목 2' },
];

describe('fetchItems', () => {
  it('아이템 목록을 가져온다', async () => {
    server.use(
      http.get(`${env.VITE_API_BASE_URL}/items`, () => {
        return HttpResponse.json(ok(mockItems));
      }),
    );

    const response = await fetchItems();
    expect(response.data).toEqual(mockItems);
  });

  it('에러 응답을 처리한다', async () => {
    server.use(
      http.get(`${env.VITE_API_BASE_URL}/items`, () => {
        return HttpResponse.json(err('500', 'Internal Server Error', 'GET', '/items'), { status: 500 });
      }),
    );

    await expect(fetchItems()).rejects.toThrow();
  });
});
```

- `server.use()`로 핸들러 오버라이드 (setup.ts에서 이미 서버 시작됨)
- `ok()`, `err()` 헬퍼로 `ApiResponse` / `ErrorResponse` 형태 응답 생성
- `env.VITE_API_BASE_URL` 사용 (`@/app/config/env`, `import.meta.env` 직접 접근 금지)

#### util (유틸리티 함수)

```ts
import { myUtil } from '@/shared/utils/myUtil';

describe('myUtil', () => {
  it('정상 입력을 처리한다', () => {
    expect(myUtil('input')).toBe('expected');
  });

  it('엣지 케이스를 처리한다', () => {
    expect(myUtil('')).toBe('');
    expect(myUtil(null)).toBeNull();
  });
});
```

- 순수 함수이므로 Provider 래핑 불필요
- 다양한 입력 케이스 (정상, 엣지, 에러) 테스트

---

## 단계 4: 작성

1. **테스트 파일 경로 결정**:
   - 컴포넌트/페이지: `src/__tests__/{ComponentName}.test.tsx`
   - 훅/스토어/API/유틸: `src/__tests__/{fileName}.test.ts`
   - 파일명에서 확장자를 제거하고 사용 (예: `useUserStore.ts` → `useUserStore.test.ts`)

2. **기존 파일 확인**: 동일 경로에 테스트 파일이 이미 있으면 사용자에게 덮어쓸지 확인한다.

3. **Write 도구로 파일 작성**: 테스트 코드를 Write 도구로 저장한다.

---

## 단계 5: 검증

1. **테스트 실행**: `react-boilerplate/` 디렉토리에서 `npx vitest run src/__tests__/{TestFileName}` 으로 해당 테스트만 실행한다 (루트 세션이면 `cd react-boilerplate &&` 를 앞에 붙인다).
2. **결과 확인**:
   - **성공**: 통과한 테스트 수를 안내하고 종료한다.
   - **실패**: 에러 메시지를 분석하여 테스트 코드를 수정한다.
3. **재시도**: 실패 시 최대 **3회**까지 수정 → 재실행을 반복한다.
4. **3회 실패**: 수정하지 못한 에러를 사용자에게 안내하고 종료한다.

---

## 출력 형식

작업 완료 후 아래 형식으로 요약한다:

```
테스트 생성 완료

- 대상: src/app/stores/useUserStore.ts (store)
- 테스트: src/__tests__/useUserStore.test.ts
- 결과: 4/4 통과
```
