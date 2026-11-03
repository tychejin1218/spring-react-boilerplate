---
paths:
  - "springboot-boilerplate/src/main/java/**/controller/**/*.java"
  - "src/main/java/**/controller/**/*.java"
  - "springboot-boilerplate/src/main/java/**/service/**/*.java"
  - "src/main/java/**/service/**/*.java"
---
# API 응답 & 예외 처리

## 서버 설정

- **포트**: 9091
- **Context Path**: `/api`
- **Base URL**: `http://localhost:9091/api`

## 성공 응답

`BaseResponse<T>` 래퍼를 사용하여 응답 반환.

```java
// Controller에서 사용
@GetMapping("/sample/member/{id}")
public BaseResponse<SampleDto.MemberResponse> getMember(@PathVariable long id) {
    SampleDto.MemberResponse response = sampleService.getMember(...);
    return BaseResponse.ok(response);          // 기본 사용
    // return BaseResponse.ok(response).message("커스텀 메시지");  // 메시지 커스텀
}
```

```json
{
  "statusCode": "OK",
  "message": "성공",
  "data": { "id": 1, "name": "admin", "email": "admin@example.com" }
}
```

## 에러 응답

`ExceptionAdvice`(@RestControllerAdvice)에서 자동 처리. `ErrorResponse` 형식 반환.

```json
{
  "statusCode": "RESOURCE_NOT_FOUND",
  "message": "존재하지 않는 정보입니다.",
  "method": "GET",
  "path": "/api/sample/member/4",
  "timestamp": "20250415145457"
}
```

## 응답 코드 규칙 (ApiStatus enum)

- `statusCode` 에는 `ApiStatus` enum 이름이 그대로 들어간다 (`getCode()` = `name()`). 숫자 코드는 쓰지 않는다.
- **enum 이름은 FE 와 맺은 API 규약이다.** 이름을 바꾸거나 지우면 FE 의 `react-boilerplate/src/shared/constants/apiStatus.ts` 와 MSW 목업(`react-boilerplate/src/**/mocks/*.ts`)도 함께 바꾼다.
- 코드 하나는 항상 같은 HTTP 상태로 나간다. HTTP 상태는 enum 에 정의하고, 예외를 던지는 곳에서는 지정하지 않는다.
- 명명:
  - 성공은 `OK` 하나
  - 도메인 접두: `AUTH_*`, `MEMBER_*`, `TODO_*`, `FILE_*` (새 도메인은 같은 방식으로 접두를 만든다)
  - 공통 접두: 도메인과 무관한 사유는 `VALIDATION_*`, `PERMISSION_*`, `RESOURCE_*`, `SYSTEM_*`
  - 접미: `*_NOT_FOUND`, `*_INVALID`, `*_DUPLICATED`, `*_FORBIDDEN`, `*_EXPIRED`, `*_LIMIT_EXCEEDED`, `*_FAILED`
- 가입 여부처럼 드러나면 안 되는 정보는 코드로 구분하지 않는다. 예: 로그인 실패는 이메일이 없든 비밀번호가 틀리든 `AUTH_LOGIN_FAILED` 하나.

## 예외 발생 방법

```java
// 기본 (HTTP 상태와 메시지는 ApiStatus 에 정의된 값)
throw new ApiException(ApiStatus.TODO_NOT_FOUND);

// 메시지만 바꿀 때
throw new ApiException(ApiStatus.SYSTEM_CUSTOM_EXCEPTION, "상세 오류 메시지");

// 원인 예외를 남길 때
throw new ApiException(ApiStatus.SYSTEM_INTERNAL_ERROR, e);
```

## 새 응답 코드 추가 시

1. `ApiStatus.java` 의 알맞은 구역에 `도메인_사유(HttpStatus.XXX, "메시지")` 를 추가한다.
2. 범용 코드(`RESOURCE_NOT_FOUND` 등)로 충분하면 새로 만들지 않는다. FE 가 구분해서 처리해야 하거나 사용자 메시지가 달라야 할 때만 만든다.
3. FE 가 이 코드로 분기한다면 `apiStatus.ts` 에 추가한다.

전체 목록과 메시지는 `src/main/java/com/example/api/common/type/ApiStatus.java` 를 본다.

## 처리되는 예외 목록

`ExceptionAdvice`에서 처리하는 예외:

| 예외 | HTTP 상태 | ApiStatus 코드 |
|------|-----------|----------------|
| `Exception` | 500 | `SYSTEM_INTERNAL_ERROR` |
| `ApiException` | ApiStatus 에 정의된 값 | 던진 ApiStatus |
| `MethodArgumentNotValidException` | 400 | `VALIDATION_METHOD_ARGUMENT_INVALID` |
| `MissingServletRequestParameterException` | 400 | `VALIDATION_PARAMETER_MISSING` |
| `ConstraintViolationException` | 400 | `VALIDATION_CONSTRAINT_VIOLATED` |
| `MethodArgumentTypeMismatchException` | 400 | `VALIDATION_PARAMETER_TYPE_MISMATCH` |
| `NoHandlerFoundException` | 404 | `SYSTEM_NO_HANDLER_FOUND` |
| `HttpRequestMethodNotSupportedException` | 405 | `SYSTEM_METHOD_NOT_SUPPORTED` |
| `HttpMediaTypeNotSupportedException` | 415 | `SYSTEM_MEDIA_TYPE_NOT_SUPPORTED` |
| `HttpMessageNotReadableException` | 400 | `VALIDATION_MESSAGE_NOT_READABLE` |
