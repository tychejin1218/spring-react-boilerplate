package com.example.api.config.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * JWT 토큰 생성 / 파싱 / 검증
 *
 * <p>세션 만료 정책 (자세한 흐름은 docs/ARCHITECTURE.md 4-3 참고):
 * <ul>
 *   <li>미활동 만료 ({@code jwt.session-idle-timeout-ms}) — Refresh 토큰의 Redis TTL.
 *       토큰을 재발급할 때마다 연장되므로, 그 시간 동안 아무 요청이 없으면 세션이 끝난다.
 *   <li>절대 만료 ({@code jwt.session-absolute-timeout-ms}) — 최초 로그인 시각(authTime) 기준 상한.
 *       Refresh 토큰의 exp 를 authTime + 절대 만료로 잡아, 계속 활동해도 이 시간이 지나면 다시 로그인해야 한다.
 *   <li>Access 토큰 유효 기간은 미활동 만료보다 짧아야 한다. 길면 Access 토큰만으로 계속 요청할 수 있어
 *       미활동 만료가 의미 없어진다.
 * </ul>
 */
@Slf4j
@Component
public class JwtTokenProvider {

  private static final String CLAIM_ROLE = "role";
  private static final String CLAIM_MEMBER_ID = "mid";
  private static final String CLAIM_AUTH_TIME = "authTime";

  private final SecretKey secretKey;
  private final long accessTokenValidityMs;

  /** 미활동 만료 (밀리초). Refresh 토큰 Redis TTL 로 쓴다 */
  @Getter
  private final long sessionIdleTimeoutMs;

  private final long sessionAbsoluteTimeoutMs;

  /**
   * JWT 토큰 제공자 생성자
   *
   * @param secret                   JWT 서명 키
   * @param accessTokenValidityMs    액세스 토큰 유효 기간 (밀리초)
   * @param sessionIdleTimeoutMs     미활동 만료 (밀리초)
   * @param sessionAbsoluteTimeoutMs 최초 로그인 기준 절대 만료 (밀리초)
   */
  public JwtTokenProvider(
      @Value("${jwt.secret}") String secret,
      @Value("${jwt.access-token-validity-ms}") long accessTokenValidityMs,
      @Value("${jwt.session-idle-timeout-ms}") long sessionIdleTimeoutMs,
      @Value("${jwt.session-absolute-timeout-ms}") long sessionAbsoluteTimeoutMs) {
    this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    this.accessTokenValidityMs = accessTokenValidityMs;
    this.sessionIdleTimeoutMs = sessionIdleTimeoutMs;
    this.sessionAbsoluteTimeoutMs = sessionAbsoluteTimeoutMs;
    if (accessTokenValidityMs >= sessionIdleTimeoutMs) {
      log.warn("[JWT] access-token-validity-ms({}) 가 session-idle-timeout-ms({}) 보다 길거나 같아"
          + " 미활동 만료가 동작하지 않습니다. Access 토큰 유효 기간을 줄이세요.",
          accessTokenValidityMs, sessionIdleTimeoutMs);
    }
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
    Instant now = Instant.now();
    var builder = baseBuilder(subject, now).expiration(Date.from(now.plusMillis(accessTokenValidityMs)));
    if (memberId != null) {
      builder.claim(CLAIM_MEMBER_ID, memberId);
    }
    if (role != null) {
      builder.claim(CLAIM_ROLE, role);
    }
    return builder.compact();
  }

  /**
   * 리프레시 토큰 생성
   *
   * <p>exp 는 발급 시각이 아니라 최초 로그인 시각(authTime) + 절대 만료다.
   * 재발급할 때 기존 토큰의 authTime 을 그대로 넘겨야 절대 만료가 유지된다.
   *
   * <p>회원 번호(mid)는 Redis 키({@code APP:AUTH:REFRESH_TOKEN:{회원 번호}})를 찾는 데 쓴다.
   *
   * @param subject  토큰 주체 (사용자 이메일)
   * @param memberId 회원 번호
   * @param authTime 최초 로그인 시각 (로그인 시 현재 시각, 재발급 시 기존 토큰 값)
   * @return 리프레시 토큰
   */
  public String createRefreshToken(String subject, Long memberId, Instant authTime) {
    return baseBuilder(subject, Instant.now())
        .claim(CLAIM_MEMBER_ID, memberId)
        .claim(CLAIM_AUTH_TIME, authTime.toEpochMilli())
        .expiration(Date.from(authTime.plusMillis(sessionAbsoluteTimeoutMs)))
        .compact();
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
   * 리프레시 토큰에서 최초 로그인 시각 추출
   *
   * @param token 리프레시 토큰
   * @return 최초 로그인 시각 (claim 이 없는 이전 토큰이면 null)
   */
  public Instant getAuthTime(String token) {
    Long epochMs = parseClaims(token).get(CLAIM_AUTH_TIME, Long.class);
    return epochMs == null ? null : Instant.ofEpochMilli(epochMs);
  }

  /**
   * 서명과 만료를 검증한 뒤 Claims 반환
   *
   * <p>한 요청에서 여러 claim 을 꺼낼 때 서명 검증을 한 번만 하도록 쓴다.
   *
   * @param token JWT 토큰
   * @return 유효하면 Claims, 아니면 null (사유는 WARN 로그)
   */
  // Claims 는 Map 을 상속하지만 컬렉션이 아니라 토큰 내용이다. "토큰이 무효"를 빈 Map 으로 표현하면
  // 호출부가 유효한 토큰으로 오인할 수 있어 null 을 돌려준다
  @SuppressWarnings("PMD.ReturnEmptyCollectionRatherThanNull")
  public Claims getValidClaims(String token) {
    try {
      return parseClaims(token);
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
    } catch (JwtException e) {
      log.warn("[JWT] 유효하지 않은 토큰: {}", e.getMessage());
    }
    return null;
  }

  /**
   * 토큰 유효성 검증
   *
   * @param token JWT 토큰
   * @return 유효 여부
   */
  public boolean validateToken(String token) {
    return getValidClaims(token) != null;
  }

  /**
   * Claims 에서 역할 추출
   *
   * @param claims 검증된 Claims
   * @return 역할 (없으면 null)
   */
  public static String roleOf(Claims claims) {
    return claims.get(CLAIM_ROLE, String.class);
  }

  /**
   * Claims 에서 회원 번호 추출
   *
   * @param claims 검증된 Claims
   * @return 회원 번호 (없으면 null)
   */
  public static Long memberIdOf(Claims claims) {
    return claims.get(CLAIM_MEMBER_ID, Long.class);
  }

  /**
   * jti(토큰 고유 ID)를 넣은 공통 빌더
   *
   * <p>jti 가 없으면 같은 사용자에게 같은 초에 발급한 토큰이 완전히 같아져
   * Refresh 토큰 재사용 감지와 Access 토큰 차단 목록이 구분하지 못한다.
   */
  private JwtBuilder baseBuilder(String subject, Instant now) {
    return Jwts.builder()
        .id(UUID.randomUUID().toString())
        .subject(subject)
        .issuedAt(Date.from(now))
        .signWith(secretKey);
  }

  private Claims parseClaims(String token) {
    return Jwts.parser()
        .verifyWith(secretKey)
        .build()
        .parseSignedClaims(token)
        .getPayload();
  }
}
