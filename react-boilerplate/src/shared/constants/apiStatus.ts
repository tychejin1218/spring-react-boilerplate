/**
 * BE ApiStatus 응답 코드
 *
 * BE 는 응답 본문 statusCode 에 ApiStatus enum 이름을 그대로 보낸다 (예: 'OK', 'AUTH_LOGIN_FAILED').
 * 화면이 특정 오류를 구분해 처리해야 할 때만 여기에 추가한다. BE enum 이름이 바뀌면 함께 바꾼다.
 * 전체 목록: springboot-boilerplate/src/main/java/com/example/api/common/type/ApiStatus.java
 */
export const API_STATUS_CODES = {
  OK: 'OK',
  SYSTEM_RATE_LIMIT_EXCEEDED: 'SYSTEM_RATE_LIMIT_EXCEEDED',
} as const;
