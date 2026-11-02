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

- API 함수: `@/shared/apis/instance`의 `api` 인스턴스 사용
- 반환 타입: `Promise<ApiResponse<T>>` 명시
- 쿼리 키: `@/shared/apis/queryKeys.ts`에 `createQueryKeys`로 등록
- 훅 파일: `use{Feature}s.ts` 형태로 피처 단위로 관리

## 데이터 페칭 패턴

| 상황                         | 패턴                                         |
| ---------------------------- | -------------------------------------------- |
| 기본 (권장)                  | `useQuery` + `<SuspenseBoundary>`            |
| 조건부 페칭 (`enabled` 필요) | `useQuery` + `enabled` 옵션                  |
| 데이터 변경                  | `useMutation` + `onSuccess` invalidateQueries |

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
