# 백엔드 개발자를 위한 프론트엔드 읽는 법

Spring 과 Java 에 익숙한 사람이 `react-boilerplate/` 의 코드를 읽고, Claude 가 만든 FE 변경을 검토할 수 있게 하는 문서다.
React 입문서가 아니다. 이 저장소에서 실제로 쓰는 것만 다루고, 모든 항목은 실제 파일을 가리킨다.
전체 흐름(인증, 캐시, 알림)은 [ARCHITECTURE.md](ARCHITECTURE.md) 를, Claude 가 따르는 규칙은 `react-boilerplate/.claude/rules/` 를 본다.

경로는 `react-boilerplate/src/` 기준이다.

## 1. Spring 과의 대응

| Spring 에서 | 이 FE 에서 | 파일 | 다른 점 |
|-------------|-----------|------|---------|
| Controller | 페이지 컴포넌트 | `features/todo/pages/TodoPage.tsx` | 요청을 받는 게 아니라 화면을 그린다. 상태가 바뀌면 함수가 다시 실행되어 화면을 다시 그린다 |
| Service | TanStack Query 훅 | `features/todo/hooks/useTodos.ts` | 조회(`useQuery`)는 캐시가 있고, 변경(`useMutation`)은 성공 뒤 캐시를 무효화한다 |
| Repository, RestTemplate | API 함수 | `features/todo/apis/todoApi.ts` | `api.get('/todos')` 처럼 HTTP 호출만 한다. 비즈니스 판단은 넣지 않는다 |
| DTO + Bean Validation | zod 스키마 | `features/auth/types/authForm.ts` | 스키마 하나가 타입(`z.infer`)과 검증 규칙을 둘 다 만든다. `@NotBlank` 가 `z.string().min(1, '...')` 이다 |
| `@Transactional` 뒤 캐시 비우기 | mutation 의 `onSuccess` 에서 `invalidateQueries` | `useTodos.ts` 의 `useCreateTodo` | 무효화된 쿼리는 다음 렌더링에서 자동으로 다시 조회된다 |
| Filter, Interceptor | axios 인터셉터 | `shared/apis/instance.ts` | 요청에 Access Token 을 붙이고, 401 이면 Refresh 로 갱신 후 재시도한다 |
| `@ControllerAdvice` | `QueryProvider` 의 `onError` + `SuspenseBoundary` | `app/providers/QueryProvider.tsx`, `shared/components/common/SuspenseBoundary.tsx` | 조회/변경 실패는 한곳에서 토스트로 알린다. 화면마다 `try/catch` 를 쓰지 않는다 |
| 싱글턴 Bean 에 든 상태 | Zustand 스토어 | `app/stores/useUserStore.ts` | 로그인 사용자와 토큰. 새로고침해도 남게 `localStorage` 에 저장한다 (`user-store`) |
| `HandlerInterceptor` 의 권한 검사 | `RouteGuard` | `features/auth/components/RouteGuard.tsx` | 로그인 안 했으면 `/` 로, 역할이 다르면 `/home` 으로 보낸다. 서버 검사를 대신하지 않는다 (서버는 따로 403 을 준다) |
| `application.yml` | `env/.env.{local,dev,stg,prd}` + `app/config/env.ts` | `app/config/env.ts` | 빌드 시점에 값이 박힌다. 아래 2절 참고 |
| MockMvc, Mockito | MSW + Vitest | `mocks/`, `features/*/mocks/`, `*.test.tsx` | MSW 는 브라우저와 테스트에서 실제 BE 대신 응답하는 가짜 API 다 |
| Spring Security 의 필터 체인 순서 | `main.tsx` 의 Provider 중첩 순서 | `main.tsx`, `App.tsx` | 바깥 Provider 가 안쪽에 기능을 제공한다 (QueryProvider 안에 Router) |

## 2. 백엔드와 다르게 동작하는 것

알고 있지 않으면 코드가 틀린 것처럼 보이는 것들이다.

- **환경변수는 공개 값이다.** `VITE_` 로 시작하는 값은 `npm run build` 때 JS 파일 안에 문자열로 박혀 브라우저로 내려간다. 비밀키를 넣을 곳이 아니다. 그래서 `env/.env.dev` 같은 파일이 git 에 커밋되어 있어도 문제가 없다. 로컬에서만 쓰는 값은 `env/.env.local` 에 두고, 이 파일은 커밋하지 않는다.
- **URL 이 바뀌어도 서버 요청이 없다.** SPA 는 `index.html` 하나를 받은 뒤 JS 가 화면을 바꾼다. `/todos` 로 새로고침하면 서버에 `/todos` 를 요청하므로, 운영에서는 nginx 가 어떤 경로든 `index.html` 을 돌려줘야 한다 (`nginx.conf` 의 `try_files $uri $uri/ /index.html`).
- **상태는 새로고침하면 사라진다.** 컴포넌트의 `useState`, Query 캐시 모두 메모리에만 있다. 남겨야 하는 것(로그인 정보)만 `useUserStore` 가 `localStorage` 에 쓴다.
- **같은 조회는 한 번만 나간다.** 두 컴포넌트가 같은 `queryKey` 로 `useQuery` 를 부르면 요청은 하나만 가고 결과를 나눠 쓴다. 그래서 `queryKey` 가 중요하고, `shared/apis/queryKeys.ts` 에서 한곳에 만든다.
- **Refresh Token 이 `localStorage` 에 있다.** XSS 에 노출되는 구조라 BE 가 토큰 회전과 재사용 감지로 피해를 줄인다 ([ARCHITECTURE.md](ARCHITECTURE.md) 4-3). HttpOnly 쿠키로 바꾸려면 BE 와 FE 를 함께 고쳐야 한다.
- **에러는 던지는 게 아니라 값이다.** `useQuery` 는 실패해도 예외를 던지지 않고 `isError`, `error` 를 돌려준다. 예외처럼 위로 올리는 건 `useSuspenseQuery` 를 쓸 때뿐이고, 그때는 `SuspenseBoundary` 가 받는다.
- **CORS 는 BE 책임이다.** 브라우저가 `localhost:5173` 에서 `localhost:9091` 을 부르면 BE `cors.allowed-origins` 에 FE 주소가 있어야 한다. FE 코드로는 못 푼다.

## 3. 새 화면이 추가될 때 봐야 할 파일 순서

Claude 가 "상품 목록 화면" 같은 걸 만들면 아래 순서로 파일이 생긴다. 각 파일에서 확인할 것은 한 가지다.

| 순서 | 파일 | 확인할 것 |
|------|------|-----------|
| 1 | `features/{domain}/types/{domain}.ts` | BE 응답 DTO 와 필드 이름, 타입이 같은가. 폼 검증 규칙(zod)이 BE `@Valid` 와 같은가 |
| 2 | `features/{domain}/apis/{domain}Api.ts` | URL 과 HTTP 메서드가 BE Controller 와 같은가. 반환 타입이 `Promise<ApiResponse<T>>` 인가 |
| 3 | `shared/apis/queryKeys.ts` | 조회 조건(페이지, 필터)이 키에 들어가는가. 빠지면 조건이 바뀌어도 이전 결과가 보인다 |
| 4 | `features/{domain}/mocks/{domain}Handlers.ts` | 응답 모양이 BE `BaseResponse` 와 같고, 오류 `statusCode` 가 BE `ApiStatus` enum 이름인가 |
| 5 | `features/{domain}/hooks/use{Domain}s.ts` | 변경 훅의 `onSuccess` 에 `invalidateQueries` 가 있는가 |
| 6 | `features/{domain}/components/` | 공통 UI(`Button`, `Input`, `FormField`)를 쓰는가 |
| 7 | `features/{domain}/pages/{Domain}Page.tsx` | 훅을 조합만 하는가. 여기서 `api.get` 을 직접 부르면 안 된다 |
| 8 | `app/router/router.tsx` | `lazy()` + `withSuspense()` 로 등록됐는가. 인증이 필요하면 `RouteGuard` 안에 있는가 |

BE 와 맞춰야 하는 단계는 1, 2, 4 다. BE 응답을 바꿨으면 이 세 파일을 같이 본다.

## 4. 리뷰할 때 확인할 다섯 가지

Claude 가 만든 FE 코드에서 실제로 자주 어긋나는 것들이다. 규칙은 `react-boilerplate/.claude/rules/` 에 있지만, 사람이 볼 때는 이 다섯 개만 보면 대부분 잡힌다.

1. **화면에서 직접 토스트를 띄우는가.** 조회/변경 실패는 `QueryProvider` 가 알리고, 403/5xx 는 인터셉터가 알린다. 화면이 또 띄우면 두 번 뜬다. 화면이 직접 오류를 그릴 때만 `meta: { skipErrorNotify: true }` 로 공통 알림에서 뺀다.
2. **`err.message` 를 그대로 쓰는가.** axios 의 영문 메시지("Request failed with status code 404")가 사용자에게 보인다. `getErrorMessage(err, '화면 문구')` (`shared/utils/apiError.ts`) 를 써야 BE `message` 가 나온다.
3. **`import.meta.env` 를 직접 읽는가.** `app/config/env.ts` 의 `env` 객체만 쓴다. 직접 읽으면 앱 시작 시 검증(누락 시 fail-fast)을 건너뛴다.
4. **mutation 뒤 `invalidateQueries` 가 빠졌는가.** 빠지면 저장은 됐는데 목록이 안 바뀐다. 또는 반대로 `all` 키를 무효화해 관련 없는 조회까지 다시 나가는지 본다.
5. **MSW 응답이 BE 와 다른가.** `statusCode` 가 숫자이거나 임의 문자열이면 FE 는 MSW 에서만 동작하고 실제 BE 에서 깨진다. HTTP 상태도 `ApiStatus` 에 묶인 값과 같아야 한다.

덧붙여, 변경 훅에 `onError: console.error` 가 남아 있으면 지워도 된다. `QueryProvider` 가 이미 받는다.

## 5. 용어

| 용어 | 뜻 | 이 저장소에서 |
|------|-----|---------------|
| 컴포넌트 | 화면 조각을 만드는 함수. 입력(props)을 받아 HTML 비슷한 것(JSX)을 돌려준다 | `features/todo/components/TodoItem.tsx` |
| props | 컴포넌트가 받는 인자. 메서드 파라미터와 같다 | `interface Props { todo: Todo; onComplete: ... }` |
| 훅 (hook) | `use` 로 시작하는 함수. 컴포넌트 안에서만 부를 수 있고, 상태나 캐시 같은 React 기능을 쓴다 | `useTodos`, `useToast`, `useLeaveConfirm` |
| 렌더링 | 컴포넌트 함수가 실행되어 화면이 그려지는 것. 상태가 바뀌면 다시 실행된다 | 그래서 컴포넌트 함수 본문에 부수 효과(API 호출)를 직접 쓰지 않는다 |
| useState | 컴포넌트 안의 지역 상태. 바뀌면 다시 렌더링된다 | `TodoPage` 의 `filter`, `page` |
| queryKey | 조회 캐시의 키. 같은 키면 같은 캐시 | `todoKeys.list(params).queryKey` |
| mutation | 데이터를 바꾸는 요청. `mutate()` 로 실행하고 `isPending` 으로 진행 중을 안다 | `createTodo.mutate({ title })` |
| invalidate | 캐시를 "오래됨" 으로 표시해 다음 렌더링 때 다시 조회하게 하는 것 | `queryClient.invalidateQueries(...)` |
| lazy, Suspense | 페이지 코드를 처음 들어갈 때 받아오고(lazy), 받는 동안 스피너를 보여 주는(Suspense) 것 | `router.tsx` 의 `withSuspense` |
| resolver | react-hook-form 이 폼 값을 검증할 때 쓰는 함수. zod 스키마를 넣는다 | `zodResolver(loginFormSchema)` |
| 인터셉터 | axios 요청/응답을 가로채는 함수. Spring 의 Filter 와 같은 자리 | `shared/apis/instance.ts` |
| 스토어 | 컴포넌트 밖에서도 읽고 쓰는 전역 상태 | `useUserStore`, `useToastStore`, `useDialogStore` |
| Provider | 자식 컴포넌트 전체에 무언가를 공급하는 컴포넌트. Spring 의 ApplicationContext 가 Bean 을 주는 것과 비슷하다 | `QueryProvider` |

## 6. 직접 돌려 볼 때

```bash
cd react-boilerplate
npm run dev                                 # 5173, env/.env.local 의 VITE_API_BASE_URL 로 BE 호출
VITE_ENABLE_MSW=true npm run dev            # BE 없이 MSW 응답으로 화면만 확인
npm run test:run                            # Vitest (MSW 응답으로 훅과 화면 테스트)
npx tsc -p tsconfig.app.json --noEmit       # 타입 검사. 컴파일 오류와 같은 역할
```

브라우저 개발자 도구에서 볼 것:
- **Network 탭**: 요청 헤더의 `Authorization: Bearer ...`, 응답의 `X-Trace-Id` (BE 로그와 맞춰 볼 때)
- **Application 탭 > Local Storage**: `user-store` 에 로그인 정보와 토큰
- **React Query Devtools** (화면 구석의 아이콘, prd 제외): 어떤 쿼리가 캐시에 있고 언제 다시 조회됐는지
