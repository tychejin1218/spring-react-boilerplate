## 프로젝트 개요

React 프론트엔드 SPA 서비스

- **React 19** / **Vite 7** / **TypeScript 5**
- **상태관리**: TanStack Query 5 (서버 상태) + Zustand 5 (클라이언트 상태)
- **라우팅**: React Router DOM 7
- **HTTP 클라이언트**: Axios 1
- **스키마 검증**: Zod 4
- **폼**: React Hook Form 7 + `@hookform/resolvers` 7 (zodResolver)
- **스타일**: Tailwind CSS 4
- **Mock**: MSW 2 (개발/테스트 환경 API Mocking)
- **테스트**: Vitest 4 + Testing Library + Playwright (E2E)
- **코드 품질**: ESLint 9 + Prettier 3

## 개발 서버 실행 & 빌드

```bash
npm run dev                # 로컬 개발 서버 실행 (기본 포트: 5173)
npm run dev:dev            # dev 환경 모드
npm run dev:stg            # stg 환경 모드
npm run dev:prd            # prd 환경 모드

npm run build:dev          # dev 환경 빌드
npm run build:stg          # stg 환경 빌드
npm run build:prd          # prd 환경 빌드

npm run test               # Vitest 단위 테스트 (watch 모드)
npm run test:run           # Vitest 단위 테스트 (1회 실행)
npm run test:coverage      # 커버리지 리포트 생성

npm run e2e                # Playwright E2E 테스트
npm run e2e:ui             # Playwright UI 모드
npm run e2e:headed         # 브라우저 표시 모드

npm run lint               # ESLint 검사
npm run lint:fix           # ESLint 자동 수정
npm run format             # Prettier 포맷 적용
npm run format:check       # Prettier 포맷 체크
```

## 환경변수

`.env.local` (로컬), `.env.dev`, `.env.stg`, `.env.prd` 파일로 관리.
앱 시작 시 `src/app/config/env.ts`에서 Zod 스키마로 런타임 검증 (누락/오류 시 렌더 전 fail-fast).

| 변수 | 설명 | 예시 |
|------|------|------|
| `VITE_APP_ENV` | 현재 환경 (`local`\|`dev`\|`stg`\|`prd`) | `local` |
| `VITE_API_BASE_URL` | API 서버 기본 URL | `http://localhost:9091/api` |
| `VITE_ENABLE_MSW` | MSW 활성화 여부 (`true`\|`false`) | `true` |

## 디렉토리 구조

FSD(Feature-Sliced Design) 기반 3계층 구조:

```
src/
├── app/                   # 앱 레이어 — 전역 설정·프로바이더·라우터
│   ├── config/            # 환경변수 검증 (env.ts)
│   ├── layout/            # 인증 영역 공통 레이아웃 (Layout.tsx: 상단 바 + NAV_ITEMS)
│   ├── pages/             # 앱 레벨 페이지 (HomePage, NotFoundPage)
│   ├── providers/         # QueryProvider 등 전역 프로바이더
│   ├── router/            # React Router 설정 (router.tsx)
│   └── stores/            # 전역 Zustand 스토어 (useUserStore 등)
├── features/              # 피처 레이어 — 도메인별 기능 단위
│   ├── auth/              # 인증 (로그인, 회원가입, 이메일 인증, 비밀번호 재설정, RouteGuard)
│   ├── todo/              # [예제] 할 일 관리 (페이지네이션, 필터, 이미지 업로드)
│   ├── admin/             # [예제] 관리자 전용 페이지 (ROLE_ADMIN)
│   ├── notification/      # [예제] SSE 실시간 알림 (NotificationSubscriber)
│   └── {feature}/
│       ├── types/         # 타입 정의 (todo.ts)
│       ├── apis/          # API 함수 (todoApi.ts)
│       ├── hooks/         # TanStack Query 훅 (useTodos.ts) + 단위 테스트 (useTodos.test.ts)
│       ├── mocks/         # MSW 핸들러 (todoHandlers.ts)
│       ├── pages/         # 페이지 컴포넌트 (TodoPage.tsx) + 단위 테스트 (*.test.tsx)
│       └── components/    # UI 컴포넌트 (TodoItem.tsx)
├── shared/                # 공유 레이어 — 도메인 독립 공통 모듈
│   ├── apis/              # Axios 인스턴스(instance.ts), QueryKey 팩토리, fileApi.ts([예제] presigned URL 업로드)
│   ├── components/        # common/ (ErrorBoundary, SuspenseBoundary), ui/ (Button, Input, FormField, EmptyState, LoadingSpinner, Toast, Dialog)
│   ├── constants/         # 상수 (messages.ts)
│   ├── hooks/             # 공통 훅 (useDialog, useToast, useLeaveConfirm 등)
│   ├── types/             # 공통 타입 (api.ts, env.d.ts, react-query.d.ts)
│   └── utils/             # 유틸 함수 (token.ts, apiError.ts, notifyApiError.ts, sessionExpired.ts)
├── mocks/                 # MSW 진입점 (handlers.ts, browser.ts, server.ts, fileHandlers.ts)
└── test/                  # 테스트 설정 (setup.ts)

단위 테스트는 테스트 대상 파일 옆에 `{name}.test.ts(x)` 로 둔다 (별도 `__tests__/` 디렉토리 없음).
```

## 코드 작성 규칙

세부 규칙은 `.claude/rules/` 에 있고, 해당 경로의 파일을 다룰 때 자동으로 로드된다.

| 파일 | 내용 | 로드 시점 |
|------|------|-----------|
| [architecture-rules.md](.claude/rules/architecture-rules.md) | 레이어 의존 규칙, API 함수·Query 훅 패턴, 라우터, 새 도메인 추가 순서 | `src/` 파일 |
| [naming-styling.md](.claude/rules/naming-styling.md) | 파일·함수 이름, export 방식, Tailwind 스타일, 접근성 | `src/` 파일 |
| [components-forms.md](.claude/rules/components-forms.md) | 컴포넌트 구조, 공통 UI, 폼, 오류 알림, 세션 만료, 이탈 확인 | 페이지, 컴포넌트, 훅 |
| [mocks.md](.claude/rules/mocks.md) | MSW 핸들러 작성 규칙 | `src/mocks/`, `features/*/mocks/` |
| [testing-rules.md](.claude/rules/testing-rules.md) | Vitest, Playwright 규칙 | 테스트 파일, `e2e/` |

## Claude Code 자동화 도구

스킬과 에이전트는 모노레포 루트 `.claude/`에 있으며 FE 전용은 `fe-` 접두사가 붙는다.

### Skills (`/xxx`) — 명시적 호출

| 스킬 | 역할 | 사용법 |
|------|------|------|
| `/fe-scaffold <feature명>` | 새 feature 디렉토리 구조 일괄 생성 (`fe-feature-scaffolder` 에이전트 호출) | `/fe-scaffold product` |
| `/fe-vitest-gen <파일경로>` | Vitest 단위 테스트 자동 생성 | `/fe-vitest-gen src/features/auth/hooks/useLogin.ts` |
| `/fe-e2e-gen <페이지명>` | Playwright E2E 테스트 자동 생성 | `/fe-e2e-gen LoginPage` |
| `/fe-review <파일명>` | `fe-code-reviewer` 에이전트로 코드 리뷰 | `/fe-review src/features/todo/pages/TodoPage.tsx` |
| `/fe-explain <파일명>` | 코드 흐름·패턴 설명 | `/fe-explain src/shared/apis/instance.ts` |
| `/fe-check [파일명]` | ESLint + Prettier 포맷 체크 및 자동 수정 | `/fe-check` |

### Agents — 대화 맥락에 따라 자동 호출

| 에이전트 | 자동 호출 트리거 |
|---------|---------------|
| `fe-debugger` | FE 에러 메시지·스택 트레이스·콘솔 에러 공유 시 |
| `fe-code-reviewer` | FE 코드 변경 후 리뷰 요청 시 |
| `fe-feature-scaffolder` | 새 기능·페이지·feature 추가 요청 시 |

### Hooks — 파일 저장 시 자동 실행

루트 `.claude/hooks/fe-format.sh` hook이 `react-boilerplate/` 소스 파일 편집/생성 후 자동 실행된다
(루트에서 시작한 세션은 루트 `.claude/settings.json`, 이 디렉토리에서 시작한 세션은 `react-boilerplate/.claude/settings.json`에 등록되어 있다):
1. **Prettier** — 코드 포매팅 자동 적용
2. **ESLint --fix** — 자동 수정 가능한 린트 에러 해결

`react-boilerplate/node_modules`가 설치되어 있어야 동작한다 (`npm install`). 워크트리 세션은 루트 settings 의 `worktree.symlinkDirectories` 로 메인 체크아웃의 node_modules 를 공유한다.
