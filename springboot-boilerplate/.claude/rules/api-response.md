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
  "statusCode": "200",
  "message": "성공",
  "data": { "id": 1, "name": "admin", "email": "admin@example.com" }
}
```

## 에러 응답

`ExceptionAdvice`(@RestControllerAdvice)에서 자동 처리. `ErrorResponse` 형식 반환.

```json
{
  "statusCode": "804",
  "message": "존재하지 않는 정보입니다.",
  "method": "GET",
  "path": "/api/sample/member/4",
  "timestamp": "20250415145457"
}
```

## 예외 발생 방법

```java
// ApiStatus enum 사용 (권장)
throw new ApiException(HttpStatus.BAD_REQUEST, ApiStatus.NOT_FOUND);

// 커스텀 메시지
throw new ApiException(HttpStatus.BAD_REQUEST, ApiStatus.CUSTOM_EXCEPTION, "상세 오류 메시지");

// ApiStatus만 사용 (기본 HttpStatus.BAD_REQUEST)
throw new ApiException(ApiStatus.NOT_FOUND);
```

## 상태 코드 (ApiStatus enum)

### 비즈니스 에러 코드 (Custom Exception)

| 코드 | 이름 | 메시지 |
|------|------|--------|
| 200 | OK | 성공 |
| 800 | CUSTOM_EXCEPTION | 오류가 발생했습니다. 확인 후 다시 시도해주세요. |
| 801 | INVALID_REQUEST | 유효하지 않은 요청입니다. |
| 802 | FORBIDDEN_REQUEST | 허용되지 않은 요청입니다. |
| 803 | DUPLICATED_REQUEST | 중복된 요청입니다. |
| 804 | NOT_FOUND | 존재하지 않는 정보입니다. |
| 805 | UNAUTHORIZED | 유효하지 않은 권한입니다. |
| 806 | MEMBER_UPDATE_FAILED | 회원 정보 수정에 실패했습니다. |
| 807 | MEMBER_DELETE_FAILED | 회원 정보 삭제에 실패했습니다. |

### 시스템 에러 코드 (Exception Handler)

| 코드 | 이름 | 메시지 |
|------|------|--------|
| 900 | INTERNAL_SERVER_ERROR | 내부 오류가 발생했습니다. |
| 901 | METHOD_ARGUMENT_NOT_VALID | 파라미터가 유효하지 않습니다. |
| 902 | MISSING_SERVLET_REQUEST_PARAMETER | 필수 파라미터가 누락되었습니다. |
| 903 | CONSTRAINT_VIOLATION | 파라미터 유효성 검사에 실패했습니다. |
| 904 | METHOD_ARGUMENT_TYPE_MISMATCH | 파라미터 타입이 올바르지 않습니다. |
| 905 | NO_HANDLER_FOUND | 요청한 URL을 찾을 수 없습니다. |
| 906 | HTTP_REQUEST_METHOD_NOT_SUPPORTED | 지원하지 않는 메서드입니다. |
| 907 | HTTP_MEDIA_TYPE_NOT_SUPPORTED | 지원되지 않는 미디어 타입입니다. |
| 908 | HTTP_MESSAGE_NOT_READABLE_EXCEPTION | 읽을 수 있는 요청 정보가 없습니다. |

## 새 비즈니스 에러 코드 추가 시

1. `ApiStatus.java` enum에 새 코드 추가 (8xx 범위)
2. 필요시 `ExceptionAdvice.java`에 새 핸들러 추가

## 처리되는 예외 목록

`ExceptionAdvice`에서 처리하는 예외:

| 예외 | HTTP 상태 | ApiStatus 코드 |
|------|-----------|----------------|
| `Exception` | 500 | 900 |
| `ApiException` | 동적 (기본 400) | 동적 (ApiStatus에 따라) |
| `MethodArgumentNotValidException` | 400 | 901 |
| `MissingServletRequestParameterException` | 400 | 902 |
| `ConstraintViolationException` | 400 | 903 |
| `MethodArgumentTypeMismatchException` | 400 | 904 |
| `NoHandlerFoundException` | 404 | 905 |
| `HttpRequestMethodNotSupportedException` | 405 | 906 |
| `HttpMediaTypeNotSupportedException` | 415 | 907 |
| `HttpMessageNotReadableException` | 400 | 908 |
