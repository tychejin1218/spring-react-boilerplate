package com.example.api.common.advice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.api.common.exception.ApiException;
import com.example.api.common.response.ErrorResponse;
import com.example.api.common.type.ApiStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

/**
 * 전역 예외 처리 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("전역 예외 처리 단위 테스트")
class ExceptionAdviceTest {

  @InjectMocks
  private ExceptionAdvice exceptionAdvice;

  private HttpServletRequest request;

  @BeforeEach
  void setUp() {
    request = mock(HttpServletRequest.class);
    when(request.getMethod()).thenReturn("GET");
    when(request.getRequestURI()).thenReturn("/test");
  }

  @Test
  @Order(1)
  @DisplayName("미처리 예외 - 500 Internal Server Error 응답")
  void handleException_returns500() {
    // given
    Exception ex = new RuntimeException("unexpected error");

    // when
    ResponseEntity<ErrorResponse> response = exceptionAdvice.handleException(ex, request);

    // then
    assertAll(
        () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR),
        () -> assertThat(response.getBody()).isNotNull(),
        () -> assertThat(response.getBody().getStatusCode())
            .isEqualTo(ApiStatus.SYSTEM_INTERNAL_ERROR.getCode()),
        () -> assertThat(response.getBody().getMessage())
            .isEqualTo(ApiStatus.SYSTEM_INTERNAL_ERROR.getMessage()),
        () -> assertThat(response.getBody().getMethod()).isEqualTo("GET"),
        () -> assertThat(response.getBody().getPath()).isEqualTo("/test"),
        () -> assertThat(response.getBody().getTimestamp()).isNotBlank()
    );
  }

  @Test
  @Order(2)
  @DisplayName("ApiException 5xx - 500 응답 (log.error 분기)")
  void handleApiException_5xx_returns500() {
    // given
    ApiException ex = new ApiException(ApiStatus.SYSTEM_INTERNAL_ERROR);

    // when
    ResponseEntity<ErrorResponse> response = exceptionAdvice.handleApiException(ex, request);

    // then
    assertAll(
        () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR),
        () -> assertThat(response.getBody()).isNotNull(),
        () -> assertThat(response.getBody().getStatusCode())
            .isEqualTo(ApiStatus.SYSTEM_INTERNAL_ERROR.getCode()),
        () -> assertThat(response.getBody().getMessage())
            .isEqualTo(ApiStatus.SYSTEM_INTERNAL_ERROR.getMessage()),
        () -> assertThat(response.getBody().getTimestamp()).isNotBlank()
    );
  }

  @Test
  @Order(3)
  @DisplayName("ApiException 4xx - 404 응답 (log.warn 분기)")
  void handleApiException_4xx_returns404() {
    // given
    ApiException ex = new ApiException(ApiStatus.RESOURCE_NOT_FOUND);

    // when
    ResponseEntity<ErrorResponse> response = exceptionAdvice.handleApiException(ex, request);

    // then
    assertAll(
        () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND),
        () -> assertThat(response.getBody()).isNotNull(),
        () -> assertThat(response.getBody().getStatusCode())
            .isEqualTo(ApiStatus.RESOURCE_NOT_FOUND.getCode()),
        () -> assertThat(response.getBody().getMessage())
            .isEqualTo(ApiStatus.RESOURCE_NOT_FOUND.getMessage()),
        () -> assertThat(response.getBody().getTimestamp()).isNotBlank()
    );
  }

  @Test
  @Order(4)
  @DisplayName("ApiException 사용자 정의 메시지 - 응답에 커스텀 메시지 포함")
  void handleApiException_customMessage() {
    // given
    String customMessage = "사용자 정의 오류 메시지";
    ApiException ex = new ApiException(ApiStatus.VALIDATION_INVALID_REQUEST,
        customMessage);

    // when
    ResponseEntity<ErrorResponse> response = exceptionAdvice.handleApiException(ex, request);

    // then
    assertAll(
        () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST),
        () -> assertThat(response.getBody().getStatusCode())
            .isEqualTo(ApiStatus.VALIDATION_INVALID_REQUEST.getCode()),
        () -> assertThat(response.getBody().getMessage()).isEqualTo(customMessage)
    );
  }

  @Test
  @Order(5)
  @DisplayName("MethodArgumentNotValidException - 400 Bad Request (901)")
  void handleMethodArgumentNotValidException_returns400() {
    // given
    MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);

    // when
    ResponseEntity<ErrorResponse> response =
        exceptionAdvice.handleMethodArgumentNotValidException(ex, request);

    // then
    assertAll(
        () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST),
        () -> assertThat(response.getBody()).isNotNull(),
        () -> assertThat(response.getBody().getStatusCode())
            .isEqualTo(ApiStatus.VALIDATION_METHOD_ARGUMENT_INVALID.getCode()),
        () -> assertThat(response.getBody().getMessage())
            .isEqualTo(ApiStatus.VALIDATION_METHOD_ARGUMENT_INVALID.getMessage())
    );
  }

  @Test
  @Order(6)
  @DisplayName("MissingServletRequestParameterException - 400 Bad Request (902)")
  void handleMissingServletRequestParameterException_returns400() {
    // given
    MissingServletRequestParameterException ex =
        new MissingServletRequestParameterException("param", "String");

    // when
    ResponseEntity<ErrorResponse> response =
        exceptionAdvice.handleMissingServletRequestParameterException(ex, request);

    // then
    assertAll(
        () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST),
        () -> assertThat(response.getBody()).isNotNull(),
        () -> assertThat(response.getBody().getStatusCode())
            .isEqualTo(ApiStatus.VALIDATION_PARAMETER_MISSING.getCode()),
        () -> assertThat(response.getBody().getMessage())
            .isEqualTo(ApiStatus.VALIDATION_PARAMETER_MISSING.getMessage())
    );
  }

  @Test
  @Order(7)
  @DisplayName("ConstraintViolationException - 400 Bad Request (903)")
  void handleConstraintViolationException_returns400() {
    // given
    ConstraintViolationException ex =
        new ConstraintViolationException("constraint violation", Collections.emptySet());

    // when
    ResponseEntity<ErrorResponse> response =
        exceptionAdvice.handleConstraintViolationException(ex, request);

    // then
    assertAll(
        () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST),
        () -> assertThat(response.getBody()).isNotNull(),
        () -> assertThat(response.getBody().getStatusCode())
            .isEqualTo(ApiStatus.VALIDATION_CONSTRAINT_VIOLATED.getCode()),
        () -> assertThat(response.getBody().getMessage())
            .isEqualTo(ApiStatus.VALIDATION_CONSTRAINT_VIOLATED.getMessage())
    );
  }

  @Test
  @Order(8)
  @DisplayName("MethodArgumentTypeMismatchException - 400 Bad Request (904)")
  void handleMethodArgumentTypeMismatchException_returns400() {
    // given
    MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);

    // when
    ResponseEntity<ErrorResponse> response =
        exceptionAdvice.handleMethodArgumentTypeMismatchException(ex, request);

    // then
    assertAll(
        () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST),
        () -> assertThat(response.getBody()).isNotNull(),
        () -> assertThat(response.getBody().getStatusCode())
            .isEqualTo(ApiStatus.VALIDATION_PARAMETER_TYPE_MISMATCH.getCode()),
        () -> assertThat(response.getBody().getMessage())
            .isEqualTo(ApiStatus.VALIDATION_PARAMETER_TYPE_MISMATCH.getMessage())
    );
  }

  @Test
  @Order(9)
  @DisplayName("NoHandlerFoundException - 404 Not Found (905)")
  void handleNoHandlerFoundException_returns404() {
    // given
    NoHandlerFoundException ex =
        new NoHandlerFoundException("GET", "/unknown", new HttpHeaders());

    // when
    ResponseEntity<ErrorResponse> response =
        exceptionAdvice.handleNoHandlerFoundException(ex, request);

    // then
    assertAll(
        () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND),
        () -> assertThat(response.getBody()).isNotNull(),
        () -> assertThat(response.getBody().getStatusCode())
            .isEqualTo(ApiStatus.SYSTEM_NO_HANDLER_FOUND.getCode()),
        () -> assertThat(response.getBody().getMessage())
            .isEqualTo(ApiStatus.SYSTEM_NO_HANDLER_FOUND.getMessage())
    );
  }

  @Test
  @Order(10)
  @DisplayName("HttpRequestMethodNotSupportedException - 405 Method Not Allowed (906)")
  void handleHttpRequestMethodNotSupportedException_returns405() {
    // given
    HttpRequestMethodNotSupportedException ex =
        new HttpRequestMethodNotSupportedException("DELETE");

    // when
    ResponseEntity<ErrorResponse> response =
        exceptionAdvice.handleHttpRequestMethodNotSupportedException(ex, request);

    // then
    assertAll(
        () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED),
        () -> assertThat(response.getBody()).isNotNull(),
        () -> assertThat(response.getBody().getStatusCode())
            .isEqualTo(ApiStatus.SYSTEM_METHOD_NOT_SUPPORTED.getCode()),
        () -> assertThat(response.getBody().getMessage())
            .isEqualTo(ApiStatus.SYSTEM_METHOD_NOT_SUPPORTED.getMessage())
    );
  }

  @Test
  @Order(11)
  @DisplayName("HttpMediaTypeNotSupportedException - 415 Unsupported Media Type (907)")
  void handleHttpMediaTypeNotSupportedException_returns415() {
    // given
    HttpMediaTypeNotSupportedException ex = mock(HttpMediaTypeNotSupportedException.class);

    // when
    ResponseEntity<ErrorResponse> response =
        exceptionAdvice.handleHttpMediaTypeNotSupportedException(ex, request);

    // then
    assertAll(
        () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE),
        () -> assertThat(response.getBody()).isNotNull(),
        () -> assertThat(response.getBody().getStatusCode())
            .isEqualTo(ApiStatus.SYSTEM_MEDIA_TYPE_NOT_SUPPORTED.getCode()),
        () -> assertThat(response.getBody().getMessage())
            .isEqualTo(ApiStatus.SYSTEM_MEDIA_TYPE_NOT_SUPPORTED.getMessage())
    );
  }

  @Test
  @Order(12)
  @DisplayName("HttpMessageNotReadableException - 400 Bad Request (908)")
  void handleHttpMessageNotReadableException_returns400() {
    // given
    HttpMessageNotReadableException ex = mock(HttpMessageNotReadableException.class);

    // when
    ResponseEntity<ErrorResponse> response =
        exceptionAdvice.handleHttpMessageNotReadableException(ex, request);

    // then
    assertAll(
        () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST),
        () -> assertThat(response.getBody()).isNotNull(),
        () -> assertThat(response.getBody().getStatusCode())
            .isEqualTo(ApiStatus.VALIDATION_MESSAGE_NOT_READABLE.getCode()),
        () -> assertThat(response.getBody().getMessage())
            .isEqualTo(ApiStatus.VALIDATION_MESSAGE_NOT_READABLE.getMessage())
    );
  }
}
