---
paths:
  - "react-boilerplate/src/mocks/**"
  - "react-boilerplate/src/features/*/mocks/**"
  - "src/mocks/**"
  - "src/features/*/mocks/**"
---

# MSW 핸들러 규칙

MSW 는 개발 모드(`VITE_ENABLE_MSW=true`)와 Vitest 에서 실제 BE 대신 응답하는 가짜 API 다.
핸들러가 실제 BE 와 다르게 응답하면 화면은 MSW 에서만 동작하고 실제 연동에서 깨지므로, BE 응답 형식을 그대로 흉내 낸다.

## 작성 패턴

- 핸들러는 피처별로 분리: `features/{feature}/mocks/{feature}Handlers.ts`
- `src/mocks/handlers.ts`에서 통합하여 내보내기
- `ok()`, `err()` 헬퍼는 `src/mocks/response.ts` 사용

```ts
// MSW 핸들러 패턴
export const todoHandlers = [
  http.get(`${BASE}/todos`, () => HttpResponse.json(ok(todos))),
  http.post(`${BASE}/todos`, async ({ request }) => {
    const { title } = (await request.json()) as { title: string };
    const todo: Todo = { id: nextId++, title, completed: false };
    return HttpResponse.json(ok(todo));
  }),
];
```


## BE 와 맞출 것

- 응답 본문은 `ok(data)` / `err(statusCode, message, method, path)` 로만 만든다. BE `BaseResponse`, `ErrorResponse` 와 같은 모양이다
- `statusCode` 에는 BE `ApiStatus` enum 이름을 그대로 쓴다 (예: `TODO_NOT_FOUND`, `MEMBER_EMAIL_DUPLICATED`). 숫자 코드나 임의 문자열을 만들지 않는다
- HTTP 상태(`{ status: 404 }`)는 그 enum 에 묶인 값과 같게 준다. 전체 목록: `springboot-boilerplate/src/main/java/com/example/api/common/type/ApiStatus.java`
- 성공 응답은 HTTP 200 + `ok(...)` (BE 가 201/204 를 쓰지 않는다)
- 목록 API 는 BE 와 같은 `PageResponse` 모양(`content`, 페이지 정보)으로 돌려준다 (현재 타입은 `features/todo/types/todo.ts`)
- 기본 URL 은 `env.VITE_API_BASE_URL` 을 앞에 붙인다 (`const BASE = env.VITE_API_BASE_URL`)

## 상태와 등록

- 핸들러 파일 안의 배열·Set 은 가짜 DB 라 테스트 사이에 값이 남는다. 특정 테스트에서만 다른 응답이 필요하면 `server.use()` 로 덮어쓴다 (`src/test/setup.ts` 가 테스트마다 `resetHandlers()` 로 되돌린다)
- 예제 기능 핸들러는 `src/mocks/handlers.ts` 에서 `// [예제: ...]` 주석과 함께 등록한다 (예제 제거 시 함께 지운다)
