package com.example.api.config.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class TraceIdFilterTest {

  private final TraceIdFilter filter = new TraceIdFilter();

  @Test
  @DisplayName("헤더가 없으면 새 traceId 를 만들어 MDC 와 응답 헤더에 넣고, 요청이 끝나면 MDC 에서 지운다")
  void generatesTraceId() throws Exception {
    AtomicReference<String> seen = new AtomicReference<>();
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(new MockHttpServletRequest(), response,
        new MockFilterChain() {
          @Override
          public void doFilter(jakarta.servlet.ServletRequest req,
              jakarta.servlet.ServletResponse res) {
            seen.set(MDC.get(TraceIdFilter.MDC_KEY));
          }
        });

    assertThat(seen.get()).hasSize(16).matches("[0-9a-f]+");
    assertThat(response.getHeader(TraceIdFilter.HEADER)).isEqualTo(seen.get());
    assertThat(MDC.get(TraceIdFilter.MDC_KEY)).isNull();
  }

  @Test
  @DisplayName("형식에 맞는 X-Trace-Id 헤더가 오면 그 값을 그대로 이어서 쓴다")
  void reusesValidIncomingTraceId() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(TraceIdFilter.HEADER, "gateway-abc-123");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, new MockFilterChain());

    assertThat(response.getHeader(TraceIdFilter.HEADER)).isEqualTo("gateway-abc-123");
  }

  @Test
  @DisplayName("줄바꿈 등 형식에 맞지 않는 헤더는 버리고 새 값을 만든다")
  void rejectsInvalidIncomingTraceId() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(TraceIdFilter.HEADER, "bad\nINFO fake log line");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, new MockFilterChain());

    assertThat(response.getHeader(TraceIdFilter.HEADER)).hasSize(16).matches("[0-9a-f]+");
  }
}
