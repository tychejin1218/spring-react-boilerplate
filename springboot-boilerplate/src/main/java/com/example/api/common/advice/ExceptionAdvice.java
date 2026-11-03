package com.example.api.common.advice;

import com.example.api.common.exception.ApiException;
import com.example.api.common.response.ErrorResponse;
import com.example.api.common.type.ApiStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

/**
 * 전역 예외 처리
 */
@Slf4j
@RestControllerAdvice
public class ExceptionAdvice {

  /**
   * 미처리 예외 처리
   *
   * @param ex      예외
   * @param request HTTP 요청
   * @return 오류 응답
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleException(
      Exception ex, HttpServletRequest request) {
    log.error("Exception: {}", ex.getMessage(), ex);
    return ResponseEntity
        .status(ApiStatus.SYSTEM_INTERNAL_ERROR.getHttpStatus())
        .body(ErrorResponse.of(
            ApiStatus.SYSTEM_INTERNAL_ERROR,
            request.getMethod(),
            request.getRequestURI()));
  }

  /**
   * API 비즈니스 예외 처리
   *
   * @param ex      API 예외
   * @param request HTTP 요청
   * @return 오류 응답
   */
  @ExceptionHandler(ApiException.class)
  public ResponseEntity<ErrorResponse> handleApiException(
      ApiException ex, HttpServletRequest request) {
    if (ex.getHttpStatus().is5xxServerError()) {
      log.error("handleApiException: {}", request.getRequestURI(), ex);
    } else {
      log.warn("handleApiException: {} - {}", request.getRequestURI(), ex.getMessage());
    }
    return ResponseEntity
        .status(ex.getHttpStatus())
        .body(ErrorResponse.of(
            ex.getApiStatus().getCode(),
            ex.getMessage(),
            request.getMethod(),
            request.getRequestURI()));
  }

  /**
   * 메서드 인자 유효성 검증 실패 처리
   *
   * @param ex      유효성 검증 예외
   * @param request HTTP 요청
   * @return 오류 응답
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
      MethodArgumentNotValidException ex, HttpServletRequest request) {
    log.error("MethodArgumentNotValidException: {}", ex.getMessage(), ex);
    return ResponseEntity
        .status(ApiStatus.VALIDATION_METHOD_ARGUMENT_INVALID.getHttpStatus())
        .body(ErrorResponse.of(
            ApiStatus.VALIDATION_METHOD_ARGUMENT_INVALID,
            request.getMethod(),
            request.getRequestURI()));
  }

  /**
   * 필수 요청 파라미터 누락 처리
   *
   * @param ex      파라미터 누락 예외
   * @param request HTTP 요청
   * @return 오류 응답
   */
  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<ErrorResponse> handleMissingServletRequestParameterException(
      MissingServletRequestParameterException ex, HttpServletRequest request) {
    log.error("MissingServletRequestParameterException: {}", ex.getMessage(), ex);
    return ResponseEntity
        .status(ApiStatus.VALIDATION_PARAMETER_MISSING.getHttpStatus())
        .body(ErrorResponse.of(
            ApiStatus.VALIDATION_PARAMETER_MISSING,
            request.getMethod(),
            request.getRequestURI()));
  }

  /**
   * 제약 조건 위반 처리
   *
   * @param ex      제약 조건 위반 예외
   * @param request HTTP 요청
   * @return 오류 응답
   */
  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ErrorResponse> handleConstraintViolationException(
      ConstraintViolationException ex, HttpServletRequest request) {
    log.error("ConstraintViolationException: {}", ex.getMessage(), ex);
    return ResponseEntity
        .status(ApiStatus.VALIDATION_CONSTRAINT_VIOLATED.getHttpStatus())
        .body(ErrorResponse.of(
            ApiStatus.VALIDATION_CONSTRAINT_VIOLATED,
            request.getMethod(),
            request.getRequestURI()));
  }

  /**
   * 메서드 인자 타입 불일치 처리
   *
   * @param ex      타입 불일치 예외
   * @param request HTTP 요청
   * @return 오류 응답
   */
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(
      MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
    log.error("MethodArgumentTypeMismatchException: {}", ex.getMessage(), ex);
    return ResponseEntity
        .status(ApiStatus.VALIDATION_PARAMETER_TYPE_MISMATCH.getHttpStatus())
        .body(ErrorResponse.of(
            ApiStatus.VALIDATION_PARAMETER_TYPE_MISMATCH,
            request.getMethod(),
            request.getRequestURI()));
  }

  /**
   * 요청 URL 미존재 처리
   *
   * @param ex      핸들러 미존재 예외
   * @param request HTTP 요청
   * @return 오류 응답
   */
  @ExceptionHandler(NoHandlerFoundException.class)
  public ResponseEntity<ErrorResponse> handleNoHandlerFoundException(
      NoHandlerFoundException ex, HttpServletRequest request) {
    log.warn("NoHandlerFoundException: {} {}", request.getMethod(), request.getRequestURI());
    return ResponseEntity
        .status(ApiStatus.SYSTEM_NO_HANDLER_FOUND.getHttpStatus())
        .body(ErrorResponse.of(
            ApiStatus.SYSTEM_NO_HANDLER_FOUND,
            request.getMethod(),
            request.getRequestURI()));
  }

  /**
   * 지원하지 않는 HTTP 메서드 처리
   *
   * @param ex      메서드 미지원 예외
   * @param request HTTP 요청
   * @return 오류 응답
   */
  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ErrorResponse> handleHttpRequestMethodNotSupportedException(
      HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
    log.error("HttpRequestMethodNotSupportedException: {}", ex.getMessage(), ex);
    return ResponseEntity
        .status(ApiStatus.SYSTEM_METHOD_NOT_SUPPORTED.getHttpStatus())
        .body(ErrorResponse.of(
            ApiStatus.SYSTEM_METHOD_NOT_SUPPORTED,
            request.getMethod(),
            request.getRequestURI()));
  }

  /**
   * 지원하지 않는 미디어 타입 처리
   *
   * @param ex      미디어 타입 미지원 예외
   * @param request HTTP 요청
   * @return 오류 응답
   */
  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  public ResponseEntity<ErrorResponse> handleHttpMediaTypeNotSupportedException(
      HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
    log.error("HttpMediaTypeNotSupportedException: {}", ex.getMessage(), ex);
    return ResponseEntity
        .status(ApiStatus.SYSTEM_MEDIA_TYPE_NOT_SUPPORTED.getHttpStatus())
        .body(ErrorResponse.of(
            ApiStatus.SYSTEM_MEDIA_TYPE_NOT_SUPPORTED,
            request.getMethod(),
            request.getRequestURI()));
  }

  /**
   * HTTP 메시지 읽기 불가 처리
   *
   * @param ex      메시지 읽기 불가 예외
   * @param request HTTP 요청
   * @return 오류 응답
   */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(
      HttpMessageNotReadableException ex, HttpServletRequest request) {
    log.error("HttpMessageNotReadableException: {}", ex.getMessage(), ex);
    return ResponseEntity
        .status(ApiStatus.VALIDATION_MESSAGE_NOT_READABLE.getHttpStatus())
        .body(ErrorResponse.of(
            ApiStatus.VALIDATION_MESSAGE_NOT_READABLE,
            request.getMethod(),
            request.getRequestURI()));
  }
}
