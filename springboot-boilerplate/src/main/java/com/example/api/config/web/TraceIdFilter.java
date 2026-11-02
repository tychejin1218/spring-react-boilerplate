package com.example.api.config.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 요청 추적 ID 필터
 *
 * <p>요청마다 traceId 를 MDC 에 넣어 같은 요청에서 남긴 모든 로그 줄에 같은 ID 가 찍히게 한다.
 * 응답 헤더 {@value #HEADER} 로도 돌려주므로, 사용자가 전달한 ID 로 서버 로그를 바로 찾을 수 있다.
 * 앞단(게이트웨이, 다른 서비스)이 보낸 {@value #HEADER} 가 형식에 맞으면 그 값을 이어서 쓴다.
 *
 * <p>보안 필터보다 먼저 실행되도록 가장 높은 우선순위로 등록한다.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

  /** 추적 ID 를 주고받는 HTTP 헤더 이름 */
  public static final String HEADER = "X-Trace-Id";

  /** MDC 키 (logback 패턴의 %X{traceId} 와 일치해야 한다) */
  public static final String MDC_KEY = "traceId";

  /** 외부에서 받은 ID 는 로그 주입을 막기 위해 영숫자와 하이픈, 64자 이하만 허용한다 */
  private static final Pattern VALID_TRACE_ID = Pattern.compile("^[A-Za-z0-9-]{1,64}$");

  private static final int GENERATED_LENGTH = 16;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {

    String traceId = resolveTraceId(request.getHeader(HEADER));
    MDC.put(MDC_KEY, traceId);
    response.setHeader(HEADER, traceId);
    try {
      filterChain.doFilter(request, response);
    } finally {
      MDC.remove(MDC_KEY);
    }
  }

  private String resolveTraceId(String incoming) {
    if (incoming != null && VALID_TRACE_ID.matcher(incoming).matches()) {
      return incoming;
    }
    return UUID.randomUUID().toString().replace("-", "").substring(0, GENERATED_LENGTH);
  }
}
