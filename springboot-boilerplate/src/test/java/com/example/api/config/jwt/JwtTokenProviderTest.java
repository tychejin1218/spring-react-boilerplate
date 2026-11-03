package com.example.api.config.jwt;

import static org.assertj.core.api.Assertions.assertThat;

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
  private static final long ACCESS_TOKEN_VALIDITY_MS = 1800000L;
  private static final long REFRESH_TOKEN_VALIDITY_MS = 604800000L;

  private JwtTokenProvider jwtTokenProvider;

  @BeforeEach
  void setUp() {
    jwtTokenProvider =
        new JwtTokenProvider(TEST_SECRET, ACCESS_TOKEN_VALIDITY_MS, REFRESH_TOKEN_VALIDITY_MS);
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
    String token = jwtTokenProvider.createRefreshToken("testUser");

    assertThat(token).isNotBlank();
    assertThat(token.split("\\.")).hasSize(3);
  }

  @Test
  @Order(3)
  @DisplayName("액세스 토큰과 리프레시 토큰은 서로 다른 값")
  void accessAndRefreshToken_areDifferent() {
    String accessToken = jwtTokenProvider.createAccessToken("testUser", 1L, "ROLE_USER");
    String refreshToken = jwtTokenProvider.createRefreshToken("testUser");

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
        new JwtTokenProvider(TEST_SECRET, 1L, 1L);
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
            REFRESH_TOKEN_VALIDITY_MS);
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
  @DisplayName("회원 번호 없이 만든 토큰은 getMemberId 가 null")
  void getMemberId_absent() {
    String accessToken = jwtTokenProvider.createAccessToken("testUser", null, "ROLE_USER");
    String refreshToken = jwtTokenProvider.createRefreshToken("testUser");

    assertThat(jwtTokenProvider.getMemberId(accessToken)).isNull();
    assertThat(jwtTokenProvider.getMemberId(refreshToken)).isNull();
  }
}
