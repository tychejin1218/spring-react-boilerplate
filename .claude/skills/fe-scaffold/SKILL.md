---
name: fe-scaffold
description: react-boilerplate 에 새 feature 디렉토리 구조(types, apis, hooks, mocks, pages)를 fe-feature-scaffolder 에이전트로 생성합니다.
argument-hint: "<feature명>"
---

# FE Feature 스캐폴딩

Agent 도구로 `fe-feature-scaffolder` 에이전트를 호출하고, 아래 feature 이름으로 새 feature 디렉토리 구조를 생성하세요.

feature 이름이 지정되지 않은 경우 생성할 feature 이름을 지정해달라고 사용자에게 요청하세요.

## 생성 범위

`react-boilerplate/src/features/{feature}/` 하위 전체 구조:
- `types/{feature}.ts` — 타입 정의
- `apis/{feature}Api.ts` — API 함수
- `hooks/use{Feature}s.ts` — TanStack Query 훅
- `mocks/{feature}Handlers.ts` — MSW 핸들러
- `pages/{Feature}Page.tsx` — 페이지 컴포넌트
- `components/{Feature}Item.tsx` — UI 컴포넌트 (필요 시)

기존 파일 수정:
- `src/shared/apis/queryKeys.ts` — 새 QueryKey 팩토리 추가
- `src/mocks/handlers.ts` — 새 MSW 핸들러 통합
- `src/app/router/router.tsx` — 새 페이지 라우트 등록

---

대상 feature: $ARGUMENTS
