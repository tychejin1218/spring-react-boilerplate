package com.example.api.config.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * JWT 토큰 생성 / 파싱 / 검증
 */
@Slf4j
@Component
public class JwtTokenProvider {

  private static final String CLAIM_ROLE = "role";
  private static final String CLAIM_MEMBER_ID = "mid";

  private final SecretKey secretKey;
  private final long accessTokenValidityMs;
  private final long refreshTokenValidityMs;

  /**
   * JWT 토큰 제공자 생성자
   *
   * @param secret                  JWT 서명 키
   * @param accessTokenValidityMs   액세스 토큰 유효 기간 (밀리초)
   * @param refreshTokenValidityMs  리프레시 토큰 유효 기간 (밀리초)
   */
  public JwtTokenProvider(
      @Value("${jwt.secret}") String secret,
      @Value("${jwt.access-token-validity-ms}") long accessTokenValidityMs,
      @Value("${jwt.refresh-token-validity-ms}") long refreshTokenValidityMs) {
    this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    this.accessTokenValidityMs = accessTokenValidityMs;
    this.refreshTokenValidityMs = refreshTokenValidityMs;
  }

  /**
   * 액세스 토큰 생성
   *
   * <p>회원 번호(mid claim)는 로그 MDC 에 남기기 위한 값이다. 이메일 같은 개인정보를 로그에 남기지 않으려고 쓴다.
   *
   * @param subject  토큰 주체 (사용자 이메일)
   * @param memberId 회원 번호 (DB 에 없는 회원이면 null)
   * @param role     회원 역할
   * @return 액세스 토큰
   */
  public String createAccessToken(String subject, Long memberId, String role) {
    return createToken(subject, memberId, role, accessTokenValidityMs);
  }

  /**
   * 리프레시 토큰 생성
   *
   * @param subject 토큰 주체 (사용자 이메일)
   * @return 리프레시 토큰
   */
  public String createRefreshToken(String subject) {
    return createToken(subject, null, null, refreshTokenValidityMs);
  }

  /**
   * 토큰에서 Subject(사용자 이메일) 추출
   *
   * @param token JWT 토큰
   * @return Subject
   */
  public String getSubject(String token) {
    return parseClaims(token).getSubject();
  }

  /**
   * 토큰에서 역할 추출
   *
   * @param token JWT 토큰
   * @return 역할 문자열 (없으면 null)
   */
  public String getRole(String token) {
    return parseClaims(token).get(CLAIM_ROLE, String.class);
  }

  /**
   * 토큰에서 회원 번호 추출
   *
   * @param token JWT 토큰
   * @return 회원 번호 (claim 이 없는 이전 토큰이면 null)
   */
  public Long getMemberId(String token) {
    return parseClaims(token).get(CLAIM_MEMBER_ID, Long.class);
  }

  /**
   * 토큰 유효성 검증
   *
   * @param token JWT 토큰
   * @return 유효 여부
   */
  public boolean validateToken(String token) {
    try {
      parseClaims(token);
      return true;
    } catch (ExpiredJwtException e) {
      log.warn("[JWT] 만료된 토큰: {}", e.getMessage());
    } catch (UnsupportedJwtException e) {
      log.warn("[JWT] 지원하지 않는 토큰: {}", e.getMessage());
    } catch (MalformedJwtException e) {
      log.warn("[JWT] 잘못된 형식의 토큰: {}", e.getMessage());
    } catch (SignatureException e) {
      log.warn("[JWT] 서명 검증 실패: {}", e.getMessage());
    } catch (IllegalArgumentException e) {
      log.warn("[JWT] 빈 토큰: {}", e.getMessage());
    }
    return false;
  }

  private String createToken(String subject, Long memberId, String role, long validityMs) {
    Instant now = Instant.now();
    var builder = Jwts.builder()
        .subject(subject)
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plusMillis(validityMs)))
        .signWith(secretKey);
    if (memberId != null) {
      builder.claim(CLAIM_MEMBER_ID, memberId);
    }
    if (role != null) {
      builder.claim(CLAIM_ROLE, role);
    }
    return builder.compact();
  }

  private Claims parseClaims(String token) {
    return Jwts.parser()
        .verifyWith(secretKey)
        .build()
        .parseSignedClaims(token)
        .getPayload();
  }
}
