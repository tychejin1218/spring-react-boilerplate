package com.example.api.common.response;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.example.api.common.type.ApiStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * ErrorResponse 단위 테스트
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("ErrorResponse 단위 테스트")
class ErrorResponseTest {

  private static final String METHOD = "GET";
  private static final String PATH = "/test/path";

  @Test
  @Order(1)
  @DisplayName("of(ApiStatus, method, path) - ApiStatus 코드·메시지 사용")
  void of_withApiStatus() {
    // given & when
    ErrorResponse response = ErrorResponse.of(ApiStatus.RESOURCE_NOT_FOUND, METHOD, PATH);

    // then
    assertAll(
        () -> assertThat(response.getStatusCode()).isEqualTo(ApiStatus.RESOURCE_NOT_FOUND.getCode()),
        () -> assertThat(response.getMessage()).isEqualTo(ApiStatus.RESOURCE_NOT_FOUND.getMessage()),
        () -> assertThat(response.getMethod()).isEqualTo(METHOD),
        () -> assertThat(response.getPath()).isEqualTo(PATH),
        () -> assertThat(response.getTimestamp()).isNotBlank()
    );
  }

  @Test
  @Order(2)
  @DisplayName("of(code, message, method, path) - 사용자 정의 코드·메시지 사용")
  void of_withCustomCodeAndMessage() {
    // given
    String customCode = "999";
    String customMessage = "사용자 정의 오류";

    // when
    ErrorResponse response = ErrorResponse.of(customCode, customMessage, METHOD, PATH);

    // then
    assertAll(
        () -> assertThat(response.getStatusCode()).isEqualTo(customCode),
        () -> assertThat(response.getMessage()).isEqualTo(customMessage),
        () -> assertThat(response.getMethod()).isEqualTo(METHOD),
        () -> assertThat(response.getPath()).isEqualTo(PATH),
        () -> assertThat(response.getTimestamp()).isNotBlank()
    );
  }

  @Test
  @Order(3)
  @DisplayName("timestamp - yyyyMMddHHmmss 형식 14자리 검증")
  void timestamp_format() {
    // given & when
    ErrorResponse response = ErrorResponse.of(ApiStatus.SYSTEM_INTERNAL_ERROR, METHOD, PATH);

    // then
    assertAll(
        () -> assertThat(response.getTimestamp()).hasSize(14),
        () -> assertThat(response.getTimestamp()).matches("\\d{14}")
    );
  }

  @Test
  @Order(4)
  @DisplayName("of(ApiStatus) 연속 호출 - timestamp가 서로 다를 수 있음 (연속 생성)")
  void of_multipleCallsHaveTimestamp() {
    // given & when
    ErrorResponse response1 = ErrorResponse.of(ApiStatus.RESOURCE_NOT_FOUND, METHOD, PATH);
    ErrorResponse response2 = ErrorResponse.of(ApiStatus.VALIDATION_INVALID_REQUEST, "POST", "/other");

    // then
    assertAll(
        () -> assertThat(response1.getTimestamp()).isNotBlank(),
        () -> assertThat(response2.getTimestamp()).isNotBlank()
    );
  }

  @Test
  @Order(5)
  @DisplayName("of(ApiStatus, method, path) - 각 ApiStatus 코드 매핑 검증")
  void of_apiStatusCodeMapping() {
    // given & when
    ErrorResponse internalError = ErrorResponse.of(ApiStatus.SYSTEM_INTERNAL_ERROR, METHOD, PATH);
    ErrorResponse notFound = ErrorResponse.of(ApiStatus.RESOURCE_NOT_FOUND, METHOD, PATH);
    ErrorResponse invalidRequest =
        ErrorResponse.of(ApiStatus.VALIDATION_INVALID_REQUEST, METHOD, PATH);

    // then
    assertAll(
        () -> assertThat(internalError.getStatusCode()).isEqualTo("SYSTEM_INTERNAL_ERROR"),
        () -> assertThat(notFound.getStatusCode()).isEqualTo("RESOURCE_NOT_FOUND"),
        () -> assertThat(invalidRequest.getStatusCode()).isEqualTo("VALIDATION_INVALID_REQUEST")
    );
  }

  @Test
  @Order(6)
  @DisplayName("of(ApiStatus, method, path) - HTTP 메서드와 경로 그대로 포함")
  void of_methodAndPath() {
    // given & when
    ErrorResponse response = ErrorResponse.of(ApiStatus.RESOURCE_NOT_FOUND, "POST", "/api/users/1");

    // then
    assertAll(
        () -> assertThat(response.getMethod()).isEqualTo("POST"),
        () -> assertThat(response.getPath()).isEqualTo("/api/users/1")
    );
  }
}
