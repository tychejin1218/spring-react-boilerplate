package com.example.api.common.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.example.api.common.type.ApiStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.http.HttpStatus;

/**
 * ApiException 단위 테스트
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("ApiException 단위 테스트")
class ApiExceptionTest {

  @Test
  @Order(1)
  @DisplayName("ApiStatus만 전달 - httpStatus 기본값 400, 메시지 ApiStatus 메시지")
  void constructor_apiStatusOnly() {
    // given & when
    ApiException ex = new ApiException(ApiStatus.VALIDATION_INVALID_REQUEST);

    // then
    assertAll(
        () -> assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST),
        () -> assertThat(ex.getApiStatus()).isEqualTo(ApiStatus.VALIDATION_INVALID_REQUEST),
        () -> assertThat(ex.getMessage()).isEqualTo(ApiStatus.VALIDATION_INVALID_REQUEST.getMessage())
    );
  }

  @Test
  @Order(2)
  @DisplayName("ApiStatus 전달 - HTTP 상태는 ApiStatus 에 정의된 값을 따름")
  void constructor_apiStatusHttpStatusMapping() {
    // given & when
    ApiException ex = new ApiException(ApiStatus.RESOURCE_NOT_FOUND);

    // then
    assertAll(
        () -> assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND),
        () -> assertThat(ex.getApiStatus()).isEqualTo(ApiStatus.RESOURCE_NOT_FOUND),
        () -> assertThat(ex.getMessage()).isEqualTo(ApiStatus.RESOURCE_NOT_FOUND.getMessage())
    );
  }

  @Test
  @Order(3)
  @DisplayName("ApiStatus + 메시지 전달 - 사용자 정의 메시지 사용")
  void constructor_apiStatusAndMessage() {
    // given
    String customMessage = "사용자 정의 오류 메시지";

    // when
    ApiException ex = new ApiException(ApiStatus.VALIDATION_INVALID_REQUEST,
        customMessage);

    // then
    assertAll(
        () -> assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST),
        () -> assertThat(ex.getApiStatus()).isEqualTo(ApiStatus.VALIDATION_INVALID_REQUEST),
        () -> assertThat(ex.getMessage()).isEqualTo(customMessage)
    );
  }

  @Test
  @Order(4)
  @DisplayName("ApiStatus + Throwable 전달 - 원인 예외 포함, ApiStatus 메시지")
  void constructor_apiStatusAndCause() {
    // given
    RuntimeException cause = new RuntimeException("원인 예외");

    // when
    ApiException ex = new ApiException(ApiStatus.SYSTEM_INTERNAL_ERROR, cause);

    // then
    assertAll(
        () -> assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR),
        () -> assertThat(ex.getApiStatus()).isEqualTo(ApiStatus.SYSTEM_INTERNAL_ERROR),
        () -> assertThat(ex.getMessage()).isEqualTo(ApiStatus.SYSTEM_INTERNAL_ERROR.getMessage()),
        () -> assertThat(ex.getCause()).isEqualTo(cause)
    );
  }

  @Test
  @Order(5)
  @DisplayName("5xx 상태 - is5xxServerError 검증")
  void httpStatus_5xx_check() {
    // given & when
    ApiException ex = new ApiException(ApiStatus.SYSTEM_INTERNAL_ERROR);

    // then
    assertThat(ex.getHttpStatus().is5xxServerError()).isTrue();
  }

  @Test
  @Order(6)
  @DisplayName("4xx 상태 - is4xxClientError 검증")
  void httpStatus_4xx_check() {
    // given & when
    ApiException ex = new ApiException(ApiStatus.VALIDATION_INVALID_REQUEST);

    // then
    assertAll(
        () -> assertThat(ex.getHttpStatus().is4xxClientError()).isTrue(),
        () -> assertThat(ex.getHttpStatus().is5xxServerError()).isFalse()
    );
  }

  @Test
  @Order(7)
  @DisplayName("ApiException은 RuntimeException 상속")
  void extendsRuntimeException() {
    // given & when
    ApiException ex = new ApiException(ApiStatus.VALIDATION_INVALID_REQUEST);

    // then
    assertThat(ex).isInstanceOf(RuntimeException.class);
  }
}
