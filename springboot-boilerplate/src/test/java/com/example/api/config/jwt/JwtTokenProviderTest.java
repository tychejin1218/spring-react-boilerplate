package com.example.api.config.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * JWT 토큰 제공자 단위 테스트
 *
 * <p>Spring Context 없이 JwtTokenProvider 로직만 검증
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("JWT 토큰 제공자 테스트")
class JwtTokenProviderTest {

  private static final String TEST_SECRET =
      "test-jwt-secret-key-must-be-at-least-256-bits-long-for-hs256-algorithm";
  private static final long ACCESS_TOKEN_VALIDITY_MS = 900_000L;
  private static final long IDLE_TIMEOUT_MS = 1_800_000L;
  private static final long ABSOLUTE_TIMEOUT_MS = 43_200_000L;

  private JwtTokenProvider jwtTokenProvider;

  @BeforeEach
  void setUp() {
    jwtTokenProvider =
        new JwtTokenProvider(TEST_SECRET, ACCESS_TOKEN_VALIDITY_MS, IDLE_TIMEOUT_MS, ABSOLUTE_TIMEOUT_MS);
  }

  @Test
  @Order(1)
  @DisplayName("액세스 토큰 생성 - JWT 형식(header.payload.signature)")
  void createAccessToken_success() {
    String token = jwtTokenProvider.createAccessToken("testUser", 1L, "ROLE_USER");

    assertThat(token).isNotBlank();
    assertThat(token.split("\\.")).hasSize(3);
  }

  @Test
  @Order(2)
  @DisplayName("리프레시 토큰 생성 - JWT 형식(header.payload.signature)")
  void createRefreshToken_success() {
    String token = jwtTokenProvider.createRefreshToken("testUser", 1L, Instant.now());

    assertThat(token).isNotBlank();
    assertThat(token.split("\\.")).hasSize(3);
  }

  @Test
  @Order(3)
  @DisplayName("액세스 토큰과 리프레시 토큰은 서로 다른 값")
  void accessAndRefreshToken_areDifferent() {
    String accessToken = jwtTokenProvider.createAccessToken("testUser", 1L, "ROLE_USER");
    String refreshToken = jwtTokenProvider.createRefreshToken("testUser", 1L, Instant.now());

    assertThat(accessToken).isNotEqualTo(refreshToken);
  }

  @Test
  @Order(4)
  @DisplayName("토큰에서 Subject 추출 - 정상")
  void getSubject_success() {
    String token = jwtTokenProvider.createAccessToken("testUser", 1L, "ROLE_USER");

    assertThat(jwtTokenProvider.getSubject(token)).isEqualTo("testUser");
  }

  @Test
  @Order(5)
  @DisplayName("유효한 토큰 검증 - true 반환")
  void validateToken_validToken() {
    String token = jwtTokenProvider.createAccessToken("testUser", 1L, "ROLE_USER");

    assertThat(jwtTokenProvider.validateToken(token)).isTrue();
  }

  @Test
  @Order(6)
  @DisplayName("만료된 토큰 검증 - false 반환")
  void validateToken_expiredToken() throws InterruptedException {
    JwtTokenProvider shortLivedProvider =
        new JwtTokenProvider(TEST_SECRET, 1L, 1L, 1L);
    String token = shortLivedProvider.createAccessToken("testUser", 1L, "ROLE_USER");

    Thread.sleep(10);

    assertThat(shortLivedProvider.validateToken(token)).isFalse();
  }

  @Test
  @Order(7)
  @DisplayName("잘못된 형식의 토큰 검증 - false 반환")
  void validateToken_malformedToken() {
    assertThat(jwtTokenProvider.validateToken("malformed.token.value")).isFalse();
  }

  @Test
  @Order(8)
  @DisplayName("서명이 조작된 토큰 검증 - false 반환")
  void validateToken_tamperedSignature() {
    String token = jwtTokenProvider.createAccessToken("testUser", 1L, "ROLE_USER");
    String tampered = token.substring(0, token.lastIndexOf('.') + 1) + "invalidsignature";

    assertThat(jwtTokenProvider.validateToken(tampered)).isFalse();
  }

  @Test
  @Order(9)
  @DisplayName("빈 문자열 토큰 검증 - false 반환")
  void validateToken_blankToken() {
    assertThat(jwtTokenProvider.validateToken("")).isFalse();
  }

  @Test
  @Order(10)
  @DisplayName("다른 키로 서명된 토큰 검증 - false 반환")
  void validateToken_differentKey() {
    JwtTokenProvider otherProvider =
        new JwtTokenProvider(
            "other-secret-key-must-be-at-least-256-bits-long-for-hs256-algorithm",
            ACCESS_TOKEN_VALIDITY_MS,
            IDLE_TIMEOUT_MS,
            ABSOLUTE_TIMEOUT_MS);
    String token = otherProvider.createAccessToken("testUser", 1L, "ROLE_USER");

    assertThat(jwtTokenProvider.validateToken(token)).isFalse();
  }

  @Test
  @Order(11)
  @DisplayName("액세스 토큰에서 회원 번호(mid) 추출")
  void getMemberId_success() {
    String token = jwtTokenProvider.createAccessToken("testUser", 42L, "ROLE_USER");

    assertThat(jwtTokenProvider.getMemberId(token)).isEqualTo(42L);
  }

  @Test
  @Order(12)
  @DisplayName("회원 번호 없이 만든 액세스 토큰은 getMemberId 가 null")
  void getMemberId_absent() {
    String accessToken = jwtTokenProvider.createAccessToken("testUser", null, "ROLE_USER");
    String refreshToken = jwtTokenProvider.createRefreshToken("testUser", 1L, Instant.now());

    assertThat(jwtTokenProvider.getMemberId(accessToken)).isNull();
    // 리프레시 토큰은 Redis 키를 찾으려고 회원 번호를 항상 담는다
    assertThat(jwtTokenProvider.getMemberId(refreshToken)).isEqualTo(1L);
  }

  @Test
  @Order(13)
  @DisplayName("리프레시 토큰 - authTime 이 들어가고 만료 시각은 authTime + 절대 만료")
  void refreshToken_absoluteExpiry() {
    Instant authTime = Instant.now().minusSeconds(60);
    String token = jwtTokenProvider.createRefreshToken("testUser", 1L, authTime);

    assertThat(jwtTokenProvider.getAuthTime(token))
        .isEqualTo(Instant.ofEpochMilli(authTime.toEpochMilli()));
    assertThat(jwtTokenProvider.getValidClaims(token).getExpiration().toInstant())
        .isBetween(authTime.plusMillis(ABSOLUTE_TIMEOUT_MS).minusSeconds(1),
            authTime.plusMillis(ABSOLUTE_TIMEOUT_MS).plusSeconds(1));
  }

  @Test
  @Order(14)
  @DisplayName("리프레시 토큰 - 최초 로그인 후 절대 만료가 지났으면 검증 실패")
  void refreshToken_expiredAfterAbsoluteTimeout() {
    Instant longAgo = Instant.now().minusMillis(ABSOLUTE_TIMEOUT_MS + 1_000);
    String token = jwtTokenProvider.createRefreshToken("testUser", 1L, longAgo);

    assertThat(jwtTokenProvider.validateToken(token)).isFalse();
  }

  @Test
  @Order(15)
  @DisplayName("같은 순간에 발급해도 jti 가 달라 토큰이 서로 다르다")
  void tokens_haveUniqueJti() {
    Instant authTime = Instant.now();
    String first = jwtTokenProvider.createRefreshToken("testUser", 1L, authTime);
    String second = jwtTokenProvider.createRefreshToken("testUser", 1L, authTime);

    assertThat(first).isNotEqualTo(second);
    assertThat(jwtTokenProvider.getValidClaims(first).getId()).isNotBlank();
  }
}
