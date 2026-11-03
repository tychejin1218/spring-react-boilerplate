package com.example.api.common.exception;

import com.example.api.common.type.ApiStatus;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * API 비즈니스 예외
 *
 * <p>HTTP 상태는 {@link ApiStatus} 에 묶여 있으므로 던지는 곳에서 지정하지 않는다.
 * 같은 코드가 곳곳에서 다른 HTTP 상태로 나가는 일을 막기 위해서다.
 */
@Getter
public class ApiException extends RuntimeException {

  private final ApiStatus apiStatus;

  /**
   * API 예외 생성
   *
   * @param apiStatus API 응답 상태 (HTTP 상태와 메시지 포함)
   */
  public ApiException(ApiStatus apiStatus) {
    super(apiStatus.getMessage());
    this.apiStatus = apiStatus;
  }

  /**
   * 응답 메시지를 바꿔 API 예외 생성
   *
   * @param apiStatus API 응답 상태
   * @param message   사용자에게 보낼 메시지 (기본 메시지 대신)
   */
  public ApiException(ApiStatus apiStatus, String message) {
    super(message);
    this.apiStatus = apiStatus;
  }

  /**
   * 원인 예외를 포함하는 API 예외 생성
   *
   * @param apiStatus API 응답 상태
   * @param cause     원인 예외
   */
  public ApiException(ApiStatus apiStatus, Throwable cause) {
    super(apiStatus.getMessage(), cause);
    this.apiStatus = apiStatus;
  }

  /**
   * 응답 HTTP 상태
   *
   * @return {@link ApiStatus} 에 정의된 HTTP 상태
   */
  public HttpStatus getHttpStatus() {
    return apiStatus.getHttpStatus();
  }
}
