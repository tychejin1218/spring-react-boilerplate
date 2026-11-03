package com.example.api.config.security;

import com.example.api.common.component.RedisComponent;
import com.example.api.common.type.RedisKeys;
import com.example.api.config.jwt.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * JWT 인증 필터
 *
 * <p>Authorization 헤더에서 Bearer 토큰을 추출하여 검증 후 SecurityContext에 저장.
 * 로그아웃으로 차단 목록(Redis)에 오른 토큰은 인증하지 않는다.
 *
 * <p>인증에 성공하면 회원 번호를 MDC({@value #MDC_KEY})에 넣어 이후 로그 줄에 남긴다.
 * 이메일은 개인정보라 로그에 남기지 않고 회원 번호만 쓴다.
 * 여기서 지우지 않는 이유: 바깥의 LoggingFilter 가 요청이 끝난 뒤 남기는 요약 로그에도 회원 번호가 찍혀야 한다.
 * 요청이 끝날 때 MDC 정리는 가장 바깥 필터인 {@link com.example.api.config.web.TraceIdFilter} 가 맡는다.
 */
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  /** MDC 키 (logback 패턴의 %X{memberId} 와 일치해야 한다) */
  public static final String MDC_KEY = "memberId";

  private static final String BEARER_PREFIX = "Bearer ";

  private final JwtTokenProvider jwtTokenProvider;
  private final RedisComponent redisComponent;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {

    String token = resolveToken(request);

    Claims claims = StringUtils.isNotBlank(token) ? jwtTokenProvider.getValidClaims(token) : null;
    if (claims != null && !isDenied(claims)) {
      String subject = claims.getSubject();
      String role = JwtTokenProvider.roleOf(claims);
      List<GrantedAuthority> authorities = role != null
          ? List.of(new SimpleGrantedAuthority(role))
          : Collections.emptyList();
      UsernamePasswordAuthenticationToken authentication =
          new UsernamePasswordAuthenticationToken(subject, null, authorities);
      SecurityContextHolder.getContext().setAuthentication(authentication);

      Long memberId = JwtTokenProvider.memberIdOf(claims);
      if (memberId != null) {
        MDC.put(MDC_KEY, String.valueOf(memberId));
      }
    }

    filterChain.doFilter(request, response);
  }

  /**
   * 로그아웃으로 차단 목록에 오른 토큰인지 확인
   *
   * <p>Redis 장애 시에는 조회 결과가 null 이라 차단하지 않는다 (가용성 우선). 차단 목록은
   * Access 토큰 유효 기간 안의 짧은 위험만 막는 장치라서 이 선택이 서비스 전체 장애보다 낫다.
   */
  private boolean isDenied(Claims claims) {
    String jti = claims.getId();
    return jti != null
        && redisComponent.getStringValue(RedisKeys.ACCESS_TOKEN_DENY.key(jti)) != null;
  }

  private String resolveToken(HttpServletRequest request) {
    String bearerToken = request.getHeader("Authorization");
    if (StringUtils.isNotBlank(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
      return bearerToken.substring(BEARER_PREFIX.length());
    }
    return null;
  }
}
