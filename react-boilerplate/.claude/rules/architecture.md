---
paths:
  - "react-boilerplate/src/**"
  - "src/**"
---

# Architecture Rules

## 레이어 의존 규칙

| 레이어      | 의존 가능 대상                | 금지                      |
| ----------- | ----------------------------- | ------------------------- |
| `app/`      | `shared/` + `features/`       |                           |
| `features/` | `shared/` + `app/stores/`     | 다른 features 도메인 참조 |
| `shared/`   | `app/stores/` + `app/config/` | features 참조             |

`app/stores/`와 `app/config/`는 전역 상태/설정으로 모든 레이어에서 참조 가능

## API 통신 패턴

```
features/[domain]/apis/  →  features/[domain]/hooks/  →  컴포넌트
(instance.ts 사용)            (TanStack Query 훅)
```

- API 함수: `@/shared/apis/instance`의 `api` 인스턴스 사용 (인터셉터가 토큰, 401 갱신, 공통 오류 알림을 처리)
- 반환 타입: `Promise<ApiResponse<T>>` 명시
- 쿼리 키: `@/shared/apis/queryKeys.ts`에 `createQueryKeys`로 등록
- 훅 파일: `use{Feature}s.ts` 형태로 피처 단위로 관리

### Query 훅 예시

- `useQuery`: 데이터 조회 — `queryKey`는 QueryKey 팩토리(`queryKeys.ts`) 사용
- `useMutation`: 데이터 변경 — `onSuccess`에서 관련 쿼리 `invalidateQueries`

```ts
// 조회 훅
export const useTodos = () =>
  useQuery({
    queryKey: todoKeys.all.queryKey,
    queryFn: getTodos,
    select: (data) => data.data,
  });

// 변경 훅
export const useCreateTodo = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateTodoRequest) => postTodo(data),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: todoKeys.all.queryKey }),
  });
};
```

### API 함수 예시

- `.then((res) => res.data)`로 axios 응답 래퍼 제거

```ts
// API 함수 패턴
export const getTodos = (): Promise<ApiResponse<Todo[]>> =>
  api.get<ApiResponse<Todo[]>>('/todos').then((res) => res.data);

export const postTodo = (data: CreateTodoRequest): Promise<ApiResponse<Todo>> =>
  api.post<ApiResponse<Todo>>('/todos', data).then((res) => res.data);
```

## 데이터 페칭 패턴

| 상황                         | 패턴                                         |
| ---------------------------- | -------------------------------------------- |
| 기본 (권장)                  | `useQuery` + `<SuspenseBoundary>`            |
| 조건부 페칭 (`enabled` 필요) | `useQuery` + `enabled` 옵션                  |
| 데이터 변경                  | `useMutation` + `onSuccess` invalidateQueries |

## 라우터 패턴

- 모든 페이지 컴포넌트는 `lazy()` + `withSuspense()` 헬퍼로 감싸기
- 인증 필요 라우트: `RouteGuard` 컴포넌트(`features/auth/components/RouteGuard.tsx`)로 감싸기. 역할 제한은 `<RouteGuard requiredRole="ROLE_ADMIN" />`
- 예제 기능(todo, admin, notification, 파일 업로드) 라우트는 `router.tsx` 상단의 `[예제: ...]` 블록에 모여 있다 (제거 방법은 README "예제 기능 제거 방법")
- 에러 처리: `errorElement: <RouterErrorFallback />`

## 새 도메인 추가 순서

1. `features/[domain]/types/` → 타입 정의 (Zod 스키마)
2. `features/[domain]/apis/` → API 함수
3. `shared/apis/queryKeys.ts` → 쿼리 키 등록
4. `features/[domain]/mocks/` → MSW 핸들러 → `mocks/handlers.ts`에 등록
5. `features/[domain]/hooks/` → TanStack Query 훅
6. `features/[domain]/components/` → UI 컴포넌트
7. `features/[domain]/pages/` → 페이지 컴포넌트
8. `app/router/router.tsx` → `lazy()` + `withSuspense()`로 라우트 등록

## 주요 유틸

- 다이얼로그: `useDialog` 훅 (`shared/hooks/useDialog`)
- 토스트: `useToast` 훅 (`shared/hooks/useToast`)
- 폼: `useForm` + `zodResolver` (Zod 스키마를 타입 + 검증 양쪽에서 재사용)
- 토큰 유틸: `@/shared/utils/token.ts`
- 환경변수: `env.ts`의 `env` 객체 사용 (`import.meta.env` 직접 접근 금지)
