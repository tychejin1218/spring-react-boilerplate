package com.example.api.config.security;

import com.example.api.common.response.ErrorResponse;
import com.example.api.common.type.ApiStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * 403 Forbidden 처리 핸들러
 */
@RequiredArgsConstructor
@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

  private final ObjectMapper objectMapper;

  @Override
  public void handle(
      HttpServletRequest request,
      HttpServletResponse response,
      AccessDeniedException accessDeniedException) throws IOException {

    response.setStatus(ApiStatus.PERMISSION_FORBIDDEN.getHttpStatus().value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");
    ErrorResponse errorResponse = ErrorResponse.of(
        ApiStatus.PERMISSION_FORBIDDEN,
        request.getMethod(),
        request.getRequestURI());
    response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
  }
}
