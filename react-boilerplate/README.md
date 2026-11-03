# React 보일러플레이트

React + Vite + TypeScript 기반의 프론트엔드 보일러플레이트입니다. 인증(로그인/회원가입/이메일 인증/비밀번호 재설정)을 기본으로 제공하고,
할 일 관리·관리자 페이지·파일 업로드·SSE 알림은 참고용 **예제 기능**으로 포함되어 있어 필요 없으면 제거할 수 있습니다 (아래 "예제 기능 제거 방법" 참고).

---

## 기술 스택

| 분류 | 기술 |
|------|------|
| UI | React 19, Tailwind CSS 4 |
| 빌드 | Vite 7, TypeScript 5 |
| 서버 상태 | React Query v5 (`@tanstack/react-query`) |
| 클라이언트 상태 | Zustand 5 |
| HTTP 클라이언트 | Axios |
| 유효성 검사 | Zod |
| 폼 | React Hook Form + `@hookform/resolvers` (zodResolver) |
| API Mocking | MSW 2 |
| 단위 테스트 | Vitest, Testing Library |
| E2E 테스트 | Playwright |
| 코드 품질 | ESLint, Prettier |

---

## 프로젝트 구조

```
src/
├── app/                        # 앱 전역 설정
│   ├── config/env.ts           # 환경변수 런타임 검증 (Zod)
│   ├── layout/Layout.tsx       # 인증 영역 공통 레이아웃 (상단 바: 앱 이름, 메뉴, 이메일, 로그아웃)
│   ├── pages/                  # 앱 레벨 페이지 (Home, NotFound)
│   ├── providers/              # React Query 등 전역 Provider
│   ├── router/router.tsx       # 라우터 정의 (React Router v7)
│   └── stores/                 # Zustand 전역 상태 (user, toast, dialog)
│
├── features/                   # 기능별 모듈 (Feature-Sliced 구조)
│   ├── auth/                   # 인증 기능 (기본 제공)
│   │   ├── apis/authApi.ts     # 로그인/회원가입/로그아웃/이메일 인증/비밀번호 재설정 API
│   │   ├── components/         # RouteGuard (인증·역할 가드), AuthCard (인증 화면 공통 카드)
│   │   ├── hooks/              # useLoginSuccess
│   │   ├── mocks/              # MSW 핸들러
│   │   ├── pages/              # Login, Register, EmailVerify, PasswordReset*, AuthError 페이지 (+ *.test.tsx)
│   │   └── types/              # user.ts (User / MemberRole), authForm.ts (폼 검증 Zod 스키마)
│   ├── todo/                   # [예제] 할 일 관리 (페이지네이션, 필터, 정렬, 이미지 업로드)
│   │   ├── apis/todoApi.ts     # 할 일 CRUD API
│   │   ├── components/         # TodoItem 컴포넌트 (삭제 시 useDialog.confirm 사용)
│   │   ├── hooks/useTodos.ts   # React Query 훅 (+ useTodos.test.ts)
│   │   ├── mocks/              # MSW 핸들러
│   │   ├── pages/TodoPage.tsx  # 할 일 목록 페이지
│   │   └── types/todo.ts       # Todo 타입 정의
│   ├── admin/                  # [예제] 관리자 전용 페이지 (ROLE_ADMIN)
│   └── notification/           # [예제] SSE 실시간 알림 (useNotifications, NotificationSubscriber)
│
├── shared/                     # 공통 모듈
│   ├── apis/                   # Axios 인스턴스(instance.ts), Query Keys, fileApi.ts([예제] presigned URL 업로드)
│   ├── components/             # common/ (ErrorBoundary, SuspenseBoundary), ui/ (Button, Input, FormField, EmptyState, LoadingSpinner, Toast, Dialog)
│   ├── constants/              # 공통 상수
│   ├── hooks/                  # 공통 훅 (useDialog, useToast, useRetryCountdown, useLeaveConfirm)
│   ├── types/                  # 공통 타입 (api.ts, env.d.ts, react-query.d.ts: Query meta 타입)
│   └── utils/                  # token.ts(JWT), apiError.ts(오류 안내 문구), notifyApiError.ts(공통 오류 토스트), sessionExpired.ts(세션 만료 1회 처리)
│
├── mocks/                      # MSW 브라우저/서버 설정, handlers.ts(통합 등록), fileHandlers.ts([예제])
├── test/setup.ts               # Vitest 전역 설정
├── App.tsx
└── main.tsx
```

---

## 로컬 개발 환경 설정

Node.js 26 이상이 필요합니다 (`.nvmrc` 참고, `nvm use`로 전환).

### 1. 의존성 설치

```bash
npm install
```

### 2. 환경변수 파일 설정

`env/` 디렉터리에 환경별 `.env` 파일이 위치합니다. `env/.env.local`은 gitignore 대상이라 저장소에 없으므로,
처음 클론한 뒤에는 예시 파일을 복사해서 만듭니다. 이 파일이 없으면 `env.ts` 검증이 실패해 앱이 뜨지 않습니다.

```bash
cp env/.env.example env/.env.local
```

```bash
# env/.env.local
VITE_APP_ENV=local
VITE_API_BASE_URL=http://localhost:9091/api
VITE_ENABLE_MSW=true   # MSW 사용 시 true, 실제 백엔드 연동 시 false
```

### 3. 개발 서버 실행

```bash
# .env.local 기준 (기본)
npm run dev

# 환경별 실행
npm run dev:dev   # .env.dev 사용
npm run dev:stg   # .env.stg 사용
npm run dev:prd   # .env.prd 사용
```

---

## 환경변수 설명

| 변수명 | 필수 | 설명 |
|--------|------|------|
| `VITE_APP_ENV` | 필수 | 앱 실행 환경 (`local` \| `dev` \| `stg` \| `prd`) |
| `VITE_API_BASE_URL` | 필수 | API 서버 Base URL (예: `http://localhost:9091/api`) |
| `VITE_ENABLE_MSW` | 선택 | MSW 활성화 여부 (`true` \| `false`, 기본값: `false`) |

> 환경변수는 앱 시작 시 Zod 스키마로 런타임 검증됩니다. 누락되거나 잘못된 값이 있으면 렌더 전에 오류가 발생합니다.

---

## MSW (Mock Service Worker)

MSW를 사용하면 실제 백엔드 없이 브라우저에서 API 요청을 가로채 Mock 응답을 반환합니다.

### Mock 개발 모드 (백엔드 없이 실행)

```bash
# env/.env.local에서 MSW 활성화
VITE_ENABLE_MSW=true

npm run dev
```

Mock 사용자 계정:

| 계정 | 비밀번호 | 역할 |
|------|---------|------|
| `test@example.com` | `password1` | `ROLE_USER` |
| `admin@example.com` | `admin1234` | `ROLE_ADMIN` (관리자 페이지 예제 접근 가능) |

### 실제 백엔드 연동 모드

```bash
# env/.env.local에서 MSW 비활성화
VITE_ENABLE_MSW=false
VITE_API_BASE_URL=http://localhost:9091/api  # 실제 백엔드 주소

npm run dev
```

MSW 핸들러는 `src/features/*/mocks/` 디렉터리에 기능별로 관리됩니다.

---

## 주요 기능

### 인증 (기본 제공)
- **로그인** (`/`): 이메일 + 비밀번호로 로그인, Access/Refresh 토큰 발급
- **회원가입** (`/register`): 이름 + 이메일 + 비밀번호로 계정 생성 후 인증 메일 발송
- **이메일 인증** (`/verify-email?token=`): 메일 링크의 토큰으로 계정 활성화 후 자동 로그인
- **비밀번호 재설정** (`/password/reset-request`, `/password/reset?token=`): 재설정 메일 발송 및 새 비밀번호 설정
- **인증 가드**: `RouteGuard` 컴포넌트로 비인증 접근 차단, `requiredRole` 로 역할 제한
- **토큰 갱신**: 401 응답 시 Refresh 토큰으로 자동 갱신 후 원 요청 재시도 (아래 "Axios 인터셉터 동작")
- **세션 만료**: 토큰 갱신 실패 시 동시에 실패한 요청이 여러 개여도 안내 토스트 1회 + 로그인 페이지 이동 1회 (아래 "오류 처리")
- **공통 UI**: `useToast`(토스트), `useDialog`(alert/confirm/custom 다이얼로그), `SuspenseBoundary`, `ErrorBoundary`
- **이탈 확인**: `useLeaveConfirm(isDirty)` 로 작성 중인 폼을 벗어날 때 확인 (예: 회원가입 화면)

### 할 일 관리 (예제)
- **목록 조회** (`/todos`): 페이지네이션, 상태 필터(전체/미완료/완료), 정렬(최신순/제목순)
- **할 일 추가 / 완료 처리 / 삭제**: 삭제는 `useDialog.confirm` 으로 한 번 더 확인
- **이미지 업로드**: presigned URL 발급 -> 스토리지 직접 업로드 -> 할 일에 이미지 URL 저장 (`shared/apis/fileApi.ts`)

### 관리자 페이지 (예제)
- `/admin`: `ROLE_ADMIN` 사용자만 접근 가능. 다른 역할은 `/home` 으로 리다이렉트

### 실시간 알림 (예제)
- `Layout` 에 배치된 `NotificationSubscriber` 가 로그인 상태에서 `GET /notifications/subscribe` SSE 를 구독하고, 수신한 알림을 토스트로 표시
- MSW 모드에서는 접속 3초 후 테스트 알림 한 건이 도착

---

## 테스트 실행

### 단위 테스트 (Vitest)

테스트 파일은 대상 파일 옆에 `{name}.test.ts(x)` 로 둡니다 (예: `src/features/todo/hooks/useTodos.test.ts`). 설정은 `vite.config.ts` 의 `test` 항목에 있습니다.

```bash
# 감시 모드 (개발 중)
npm run test

# 단일 실행
npm run test:run

# 커버리지 리포트
npm run test:coverage
```

### E2E 테스트 (Playwright)

```bash
# Headless 실행
npm run e2e

# UI 모드
npm run e2e:ui

# 브라우저 표시 모드
npm run e2e:headed

# 리포트 확인
npm run e2e:report
```

---

## 코드 품질

### ESLint

```bash
# 검사
npm run lint

# 자동 수정
npm run lint:fix
```

### Prettier

```bash
# 포매팅 적용
npm run format

# 포매팅 검사만
npm run format:check
```

---

## 빌드

```bash
npm run build:dev   # Development 빌드
npm run build:stg   # Staging 빌드
npm run build:prd   # Production 빌드 (console, debugger 자동 제거)
```

---

## 새 Feature 추가 가이드

`features/todo`와 동일한 구조로 새 기능을 추가합니다.

### Claude Code 스킬 사용 (권장)

모노레포 루트에서 Claude Code를 실행한 뒤:

```
/fe-scaffold <feature명>

예) /fe-scaffold board
```

자동으로 아래 파일 구조를 생성하고 `queryKeys.ts`, `handlers.ts`, `router.tsx`를 업데이트합니다.

### 수동 추가

```
src/features/{feature}/
├── types/{feature}.ts        # 타입 정의
├── apis/{feature}Api.ts      # API 함수
├── hooks/use{Feature}s.ts    # React Query 훅
├── mocks/{feature}Handlers.ts # MSW 핸들러
├── pages/{Feature}Page.tsx   # 페이지 컴포넌트
└── components/               # UI 컴포넌트
```

수동 추가 후 아래 파일도 함께 수정해야 합니다:

| 파일 | 수정 내용 |
|------|---------|
| `src/shared/apis/queryKeys.ts` | `createQueryKeys` 추가 |
| `src/mocks/handlers.ts` | 핸들러 import 및 추가 |
| `src/app/router/router.tsx` | lazy import 및 라우트 추가 |

---

## 예제 기능 제거 방법

todo, admin, notification, 파일 업로드는 참고용 예제입니다. 등록 지점이 기능별로 한 곳씩 모여 있어(코드의 `[예제: ...]` 주석) 아래 디렉토리와 등록 줄만 지우면 제거됩니다.
제거 후 `npm run lint` 와 `npx tsc -p tsconfig.app.json --noEmit` 으로 남은 참조가 없는지 확인하세요. BE 쪽은 [BE README](../springboot-boilerplate/README.md#예제-기능-제거-방법) 를 봅니다.

| 예제 기능 | 삭제할 디렉토리/파일 | 지울 등록 줄 |
|----------|-------------------|-------------|
| todo (할 일 관리) | `src/features/todo/`, `e2e/todo.spec.ts` | `router.tsx`: `[예제: todo]` 블록(lazy import + `todoRoutes`)과 `...todoRoutes` 한 줄, `mocks/handlers.ts`: `todoHandlers` import + 배열 항목, `shared/apis/queryKeys.ts`: `todoKeys` 와 `TodoListParams` import, `app/pages/HomePage.tsx`: `/todos` 링크, `app/layout/Layout.tsx`: `NAV_ITEMS` 의 `[예제: todo]` 항목 |
| 파일 업로드 (todo 이미지) | `src/shared/apis/fileApi.ts`, `src/mocks/fileHandlers.ts` | `mocks/handlers.ts`: `fileHandlers` import + 배열 항목. todo 를 남기고 업로드만 뺄 때는 `useTodos.ts` 의 `useUploadTodoImage`, `todoApi.ts` 의 `patchTodoImage`, `TodoItem.tsx` 의 사진 버튼도 함께 제거 |
| admin (관리자 페이지) | `src/features/admin/` | `router.tsx`: `[예제: admin]` 블록(lazy import + `adminRoutes`)과 `...adminRoutes` 한 줄, `app/pages/HomePage.tsx`: `/admin` 링크, `app/layout/Layout.tsx`: `NAV_ITEMS` 의 `[예제: admin]` 항목. `RouteGuard` 의 `requiredRole` 과 `MemberRole` 은 공통 기능이므로 유지 |
| notification (SSE 알림) | `src/features/notification/` | `app/layout/Layout.tsx`: `NotificationSubscriber` import 와 `<NotificationSubscriber />` 한 줄, `mocks/handlers.ts`: `notificationHandlers` import + 배열 항목 |

todo 를 지우면 `queryKeys.ts` 가 비게 되므로, 새 feature 의 쿼리 키를 등록하기 전까지는 파일 내용을 `export {};` 로 두거나 파일을 삭제하면 됩니다.

---

## Axios 인터셉터 동작

`src/shared/apis/instance.ts`에 전역 인터셉터가 설정되어 있습니다.

| 시점 | 동작 |
|------|------|
| 요청 전 | Zustand `accessToken`을 `Authorization: Bearer {token}` 헤더에 자동 주입 |
| 401 응답 | Refresh 토큰으로 `POST /auth/token/refresh` 호출 후 새 토큰으로 원 요청을 1회 재시도. 동시에 여러 요청이 401 을 받아도 갱신은 한 번만 수행(single-flight, 백엔드의 Refresh Token Rotation 재사용 감지 대응). 갱신 실패·갱신 요청 자체의 401·재시도 후 재401·Refresh 토큰 없음이면 `handleSessionExpired()` (세션 만료 1회 처리). 로그인 요청과 Authorization 헤더 없는 요청의 401 은 화면으로 그대로 넘김 |
| 403 응답 | 에러 토스트 + 콘솔 에러 (BE 메시지, 없으면 `ERROR_MESSAGES.FORBIDDEN`) |
| 429 응답 | 콘솔 에러만. 요청 횟수 제한은 로그인, 회원가입에만 걸리고 두 화면이 `useRetryCountdown` 으로 재시도 대기를 직접 보여 주므로 토스트를 띄우지 않음 |
| 5xx 응답 | 에러 토스트 + 콘솔 에러 (`ERROR_MESSAGES.SERVER_ERROR`, 관리자 문의 안내) |

reject 값은 BE `ErrorResponse` 본문에 `httpStatus` 를 붙인 객체입니다. 본문이 JSON 이 아니거나 응답이 없으면(네트워크 오류, 타임아웃) axios 오류를 그대로 넘깁니다.

### 오류 처리

| 구성 | 역할 |
|------|------|
| `shared/utils/apiError.ts` | `getErrorPresentation(error)` 순수 함수. 오류를 `guide`(4xx, BE 메시지) / `adminContact`(5xx, 관리자 문의) / `network`(응답 없음) / `unknown` 으로 분류하고 사용자 문구를 정한다. axios 영문 메시지(`Network Error` 등)는 노출하지 않는다. 화면에서는 `getErrorMessage(err, '화면별 실패 문구')` |
| `shared/utils/notifyApiError.ts` | 공통 에러 토스트. 같은 문구는 2초 안에 한 번, 이미 알린 오류 객체는 다시 알리지 않음, 세션 만료 중에는 알리지 않음 |
| `QueryProvider` | `QueryCache`/`MutationCache` 의 `onError` 로 모든 조회/변경 오류를 한 번씩 알림. 화면이 오류를 직접 그리면 `meta: { skipErrorNotify: true }` 로 제외 (`SuspenseBoundary` 아래 `useSuspenseQuery`, 인라인 재시도 UI 등) |
| `shared/utils/sessionExpired.ts` | `handleSessionExpired()`: 사용자 정보 삭제 + 세션 만료 토스트 1회(BE 메시지, 없으면 `ERROR_MESSAGES.SESSION_EXPIRED`) + 로그인 페이지 이동 1회. 처리 직후 3초간은 다른 오류 알림을, 그 뒤 재로그인 전까지는 401 알림을 숨긴다. 로그인 성공(`useLoginSuccess`) 시 가드를 초기화 |

알림 책임은 한 곳씩 나눈다. 403/5xx 는 `useQuery` 를 거치지 않는 직접 호출(로그인·회원가입 화면, 파일 업로드)까지 덮기 위해 인터셉터가 알리고, 나머지(4xx, 네트워크 오류)는 QueryCache/MutationCache 가 알린다. 인터셉터가 알린 오류가 QueryCache 로 다시 들어와도 같은 오류 객체라 한 번만 표시된다. `useQuery` 없이 직접 호출하는 화면은 `setError` 로 직접 안내한다.

세션 만료 이동은 `router.tsx` 가 `registerSessionExpiredNavigator` 로 등록한 `router.navigate` 를 쓴다 (새로고침 없이 이동해 토스트가 유지됨).

모든 API 함수는 `api` 인스턴스를 사용하므로 별도 토큰 처리 없이 자동 적용됩니다. 토큰 갱신 요청만 인터셉터 재진입을 피하기 위해 별도 `axios.post` 로 호출합니다.
