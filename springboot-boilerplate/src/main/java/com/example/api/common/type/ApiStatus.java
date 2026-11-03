package com.example.api.common.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * API 응답 상태 코드 및 메시지 정의
 *
 * <p>응답 본문의 {@code statusCode} 에는 enum 이름이 그대로 들어간다 ({@link #getCode()}).
 * <b>enum 이름은 FE 와 맺은 API 규약이다.</b> 이름을 바꾸면 그 코드를 비교하는 FE 코드가 깨지므로
 * 바꿀 때는 FE({@code react-boilerplate/src/shared/constants/apiStatus.ts}, MSW 목업)도 함께 바꾼다.
 *
 * <p>명명 규칙:
 * <ul>
 *   <li>성공은 {@code OK} 하나뿐이다.
 *   <li>도메인 접두 — {@code AUTH_*}, {@code MEMBER_*}, {@code TODO_*}, {@code FILE_*}.
 *       새 도메인을 추가하면 같은 방식으로 접두를 만든다.
 *   <li>공통 접두 — 도메인과 무관한 사유는 {@code VALIDATION_*}, {@code PERMISSION_*},
 *       {@code RESOURCE_*}, {@code SYSTEM_*}.
 *   <li>접미 — {@code *_NOT_FOUND}, {@code *_INVALID}, {@code *_DUPLICATED}, {@code *_FORBIDDEN},
 *       {@code *_EXPIRED}, {@code *_LIMIT_EXCEEDED}, {@code *_FAILED}.
 *   <li>코드 하나는 항상 같은 HTTP 상태로 나간다. 예외를 던지는 곳에서는 HTTP 상태를 지정하지 않고
 *       {@link com.example.api.common.exception.ApiException} 이 이 enum 의 {@link #getHttpStatus()} 를 쓴다.
 * </ul>
 */
@Getter
@RequiredArgsConstructor
public enum ApiStatus {

  // 성공
  OK(HttpStatus.OK, "성공"),

  // 공통 (도메인 무관)
  SYSTEM_CUSTOM_EXCEPTION(HttpStatus.BAD_REQUEST, "오류가 발생했습니다. 확인 후 다시 시도해주세요."),
  VALIDATION_INVALID_REQUEST(HttpStatus.BAD_REQUEST, "유효하지 않은 요청입니다."),
  PERMISSION_FORBIDDEN(HttpStatus.FORBIDDEN, "허용되지 않은 요청입니다."),
  RESOURCE_DUPLICATED(HttpStatus.CONFLICT, "중복된 요청입니다."),
  RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 정보입니다."),
  SYSTEM_RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS,
      "요청 횟수가 초과됐습니다. 잠시 후 다시 시도해주세요."),

  // 인증
  AUTH_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증 정보가 유효하지 않습니다. 다시 로그인해주세요."),
  // 이메일이 없을 때와 비밀번호가 틀렸을 때 같은 코드를 써서 가입 여부를 드러내지 않는다
  AUTH_LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),
  AUTH_EMAIL_NOT_VERIFIED(HttpStatus.FORBIDDEN, "이메일 인증 후 로그인할 수 있습니다."),
  AUTH_EMAIL_ALREADY_VERIFIED(HttpStatus.BAD_REQUEST, "이미 인증된 이메일입니다."),
  AUTH_VERIFY_TOKEN_INVALID(HttpStatus.BAD_REQUEST, "인증 링크가 만료되었거나 유효하지 않습니다."),
  AUTH_PASSWORD_RESET_TOKEN_INVALID(HttpStatus.BAD_REQUEST,
      "비밀번호 재설정 링크가 만료되었거나 유효하지 않습니다."),
  AUTH_REFRESH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "세션이 만료되었습니다. 다시 로그인해주세요."),

  // 회원
  MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 회원입니다."),
  MEMBER_EMAIL_DUPLICATED(HttpStatus.CONFLICT, "이미 가입된 이메일입니다."),

  // 할 일
  TODO_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 할 일입니다."),

  // 파일
  FILE_CONTENT_TYPE_INVALID(HttpStatus.BAD_REQUEST, "업로드할 수 없는 파일 형식입니다."),

  // 시스템 (ExceptionAdvice 가 프레임워크 예외를 자동 매핑)
  SYSTEM_INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "내부 오류가 발생했습니다."),
  VALIDATION_METHOD_ARGUMENT_INVALID(HttpStatus.BAD_REQUEST, "파라미터가 유효하지 않습니다."),
  VALIDATION_PARAMETER_MISSING(HttpStatus.BAD_REQUEST, "필수 파라미터가 누락되었습니다."),
  VALIDATION_CONSTRAINT_VIOLATED(HttpStatus.BAD_REQUEST, "파라미터 유효성 검사에 실패했습니다."),
  VALIDATION_PARAMETER_TYPE_MISMATCH(HttpStatus.BAD_REQUEST, "파라미터 타입이 올바르지 않습니다."),
  VALIDATION_MESSAGE_NOT_READABLE(HttpStatus.BAD_REQUEST, "읽을 수 있는 요청 정보가 없습니다."),
  SYSTEM_NO_HANDLER_FOUND(HttpStatus.NOT_FOUND, "요청한 URL을 찾을 수 없습니다."),
  SYSTEM_METHOD_NOT_SUPPORTED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 메서드입니다."),
  SYSTEM_MEDIA_TYPE_NOT_SUPPORTED(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "지원되지 않는 미디어 타입입니다.");

  private final HttpStatus httpStatus;
  private final String message;

  /**
   * 응답 코드 반환 (enum 이름)
   *
   * @return enum 이름
   */
  public String getCode() {
    return name();
  }
}
