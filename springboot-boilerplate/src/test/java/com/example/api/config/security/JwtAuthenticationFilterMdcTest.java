package com.example.api.config.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.api.config.jwt.JwtTokenProvider;
import com.example.api.config.web.TraceIdFilter;
import jakarta.servlet.FilterChain;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 인증 필터의 회원 번호 MDC 처리 테스트
 *
 * <p>실제 필터 순서(TraceIdFilter 바깥, JwtAuthenticationFilter 안쪽)를 그대로 엮어서 확인한다.
 */
class JwtAuthenticationFilterMdcTest {

  private static final String SECRET =
      "test-secret-key-for-jwt-must-be-at-least-256-bits-long-for-hs256";

  private final JwtTokenProvider jwtTokenProvider =
      new JwtTokenProvider(SECRET, 60_000L, 120_000L);
  private final JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(jwtTokenProvider);
  private final TraceIdFilter traceIdFilter = new TraceIdFilter();

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
    MDC.clear();
  }

  @Test
  @DisplayName("유효한 토큰이면 처리 중에 memberId 가 MDC 에 있고, 바깥 필터가 끝나면 지워진다")
  void putsMemberIdAndTraceIdFilterClearsIt() throws Exception {
    String token = jwtTokenProvider.createAccessToken("user1@example.com", 7L, "ROLE_USER");
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer " + token);
    AtomicReference<String> seenInside = new AtomicReference<>();
    AtomicReference<String> seenAfterJwtFilter = new AtomicReference<>();

    FilterChain endpoint = (req, res) -> seenInside.set(MDC.get(JwtAuthenticationFilter.MDC_KEY));
    FilterChain chainAfterTrace = (req, res) -> {
      jwtFilter.doFilter(req, res, endpoint);
      // LoggingFilter 처럼 인증 필터 바깥에서 요청 후 남기는 로그 시점
      seenAfterJwtFilter.set(MDC.get(JwtAuthenticationFilter.MDC_KEY));
    };

    traceIdFilter.doFilter(request, new MockHttpServletResponse(), chainAfterTrace);

    assertThat(seenInside.get()).isEqualTo("7");
    assertThat(seenAfterJwtFilter.get()).isEqualTo("7");
    assertThat(MDC.get(JwtAuthenticationFilter.MDC_KEY)).isNull();
  }

  @Test
  @DisplayName("토큰이 없으면 memberId 를 넣지 않는다")
  void noTokenNoMemberId() throws Exception {
    AtomicReference<String> seen = new AtomicReference<>("not-called");

    jwtFilter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(),
        (req, res) -> seen.set(MDC.get(JwtAuthenticationFilter.MDC_KEY)));

    assertThat(seen.get()).isNull();
  }
}
