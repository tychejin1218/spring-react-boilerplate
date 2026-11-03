package com.example.api.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.longThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.api.auth.dto.AuthDto;
import com.example.api.common.component.RedisComponent;
import com.example.api.common.exception.ApiException;
import com.example.api.config.jwt.JwtTokenProvider;
import com.example.api.domain.entity.Member;
import com.example.api.domain.repository.MemberRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 인증 서비스 Mock 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("인증 서비스 Mock 테스트")
class AuthServiceMockTest {

  @InjectMocks
  private AuthService authService;

  @Mock
  private JwtTokenProvider jwtTokenProvider;

  @Mock
  private RedisComponent redisComponent;

  @Mock
  private MemberRepository memberRepository;

  @Mock
  private PasswordEncoder passwordEncoder;

  @Mock
  private EmailService emailService;

  @Test
  @Order(1)
  @DisplayName("로그인 - 성공")
  void login_success() {
    // given
    AuthDto.LoginRequest request = AuthDto.LoginRequest.builder()
        .email("test@example.com")
        .password("password123")
        .build();
    Member member = Member.builder()
        .id(1L)
        .email("test@example.com")
        .name("테스트")
        .password("encodedPassword")
        .emailVerified(true)
        .build();
    given(memberRepository.findByEmail("test@example.com")).willReturn(Optional.of(member));
    given(passwordEncoder.matches("password123", "encodedPassword")).willReturn(true);
    given(jwtTokenProvider.createAccessToken("test@example.com", 1L, "ROLE_USER")).willReturn(
        "accessToken");
    given(jwtTokenProvider.createRefreshToken(eq("test@example.com"), eq(1L), any(Instant.class)))
        .willReturn("refreshToken");

    // when
    AuthDto.TokenResponse response = authService.login(request);

    // then
    assertThat(response.getAccessToken()).isEqualTo("accessToken");
    assertThat(response.getRefreshToken()).isEqualTo("refreshToken");
  }

  @Test
  @Order(2)
  @DisplayName("로그인 - 존재하지 않는 이메일")
  void login_memberNotFound() {
    // given
    AuthDto.LoginRequest request = AuthDto.LoginRequest.builder()
        .email("notfound@example.com")
        .password("password123")
        .build();
    given(memberRepository.findByEmail("notfound@example.com")).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> authService.login(request))
        .isInstanceOf(ApiException.class);
  }

  @Test
  @Order(3)
  @DisplayName("로그인 - 비밀번호 불일치")
  void login_wrongPassword() {
    // given
    AuthDto.LoginRequest request = AuthDto.LoginRequest.builder()
        .email("test@example.com")
        .password("wrongPassword")
        .build();
    Member member = Member.builder()
        .id(1L)
        .email("test@example.com")
        .name("테스트")
        .password("encodedPassword")
        .build();
    given(memberRepository.findByEmail("test@example.com")).willReturn(Optional.of(member));
    given(passwordEncoder.matches("wrongPassword", "encodedPassword")).willReturn(false);

    // when & then
    assertThatThrownBy(() -> authService.login(request))
        .isInstanceOf(ApiException.class);
  }

  @Test
  @Order(4)
  @DisplayName("로그아웃 - Refresh 토큰 삭제, Access 토큰은 남은 시간만큼 차단 목록 등록")
  void logout_success() {
    // given
    AuthDto.RefreshRequest request = AuthDto.RefreshRequest.builder()
        .refreshToken("validRefresh")
        .build();
    given(jwtTokenProvider.getValidClaims("validRefresh"))
        .willReturn(claims("test@example.com", "refresh-jti", 60_000));
    given(jwtTokenProvider.getValidClaims("validAccess"))
        .willReturn(claims("test@example.com", "access-jti", 60_000));

    // when
    authService.logout(request, "validAccess");

    // then
    verify(redisComponent).deleteKey("APP:AUTH:REFRESH_TOKEN:1");
    verify(redisComponent).setStringValue(eq("APP:AUTH:ACCESS_TOKEN_DENY:access-jti"), eq("1"),
        longThat(ms -> ms > 0 && ms <= 60_000), eq(TimeUnit.MILLISECONDS));
  }

  @Test
  @Order(5)
  @DisplayName("로그아웃 - 토큰이 만료되거나 잘못돼도 예외 없이 끝남")
  void logout_invalidToken() {
    // given
    AuthDto.RefreshRequest request = AuthDto.RefreshRequest.builder()
        .refreshToken("invalidToken")
        .build();
    given(jwtTokenProvider.getValidClaims("invalidToken")).willReturn(null);

    // when
    authService.logout(request, null);

    // then
    verify(redisComponent, never()).deleteKey(any());
    verify(redisComponent, never()).setStringValue(any(), any(), anyLong(), any());
  }

  @Test
  @Order(6)
  @DisplayName("토큰 재발급 - 성공 (authTime 승계, compare-and-set 으로 교체하며 미활동 TTL 연장)")
  void refresh_success() {
    // given
    AuthDto.RefreshRequest request = AuthDto.RefreshRequest.builder()
        .refreshToken("validToken")
        .build();
    Member member = Member.builder()
        .id(1L)
        .email("test@example.com")
        .name("테스트")
        .password("encodedPassword")
        .build();
    Instant authTime = Instant.now().minusSeconds(600);
    given(jwtTokenProvider.getValidClaims("validToken"))
        .willReturn(claims("test@example.com", "jti", 60_000));
    given(redisComponent.getStringValue("APP:AUTH:REFRESH_TOKEN:1"))
        .willReturn("validToken");
    given(jwtTokenProvider.getAuthTime("validToken")).willReturn(authTime);
    given(memberRepository.findById(1L)).willReturn(Optional.of(member));
    given(jwtTokenProvider.createAccessToken("test@example.com", 1L, "ROLE_USER")).willReturn(
        "newAccessToken");
    given(jwtTokenProvider.createRefreshToken("test@example.com", 1L, authTime))
        .willReturn("newRefreshToken");
    given(jwtTokenProvider.getSessionIdleTimeoutMs()).willReturn(1_800_000L);
    given(redisComponent.compareAndSet("APP:AUTH:REFRESH_TOKEN:1", "validToken",
        "newRefreshToken", 1_800_000L, TimeUnit.MILLISECONDS)).willReturn(true);

    // when
    AuthDto.TokenResponse response = authService.refresh(request);

    // then
    assertThat(response.getAccessToken()).isEqualTo("newAccessToken");
    assertThat(response.getRefreshToken()).isEqualTo("newRefreshToken");
  }

  @Test
  @Order(7)
  @DisplayName("토큰 재발급 - 유효하지 않은 토큰 (서명 오류 또는 절대 만료 초과)")
  void refresh_invalidToken() {
    // given
    AuthDto.RefreshRequest request = AuthDto.RefreshRequest.builder()
        .refreshToken("invalidToken")
        .build();
    given(jwtTokenProvider.getValidClaims("invalidToken")).willReturn(null);

    // when & then
    assertThatThrownBy(() -> authService.refresh(request))
        .isInstanceOf(ApiException.class);
  }

  @Test
  @Order(8)
  @DisplayName("토큰 재발급 - Redis 저장 토큰 없음 (미활동 만료 또는 로그아웃)")
  void refresh_storedTokenBlank() {
    // given
    AuthDto.RefreshRequest request = AuthDto.RefreshRequest.builder()
        .refreshToken("validToken")
        .build();
    given(jwtTokenProvider.getValidClaims("validToken"))
        .willReturn(claims("test@example.com", "jti", 60_000));
    given(jwtTokenProvider.getAuthTime("validToken")).willReturn(Instant.now());
    given(redisComponent.getStringValue("APP:AUTH:REFRESH_TOKEN:1")).willReturn(null);

    // when & then
    assertThatThrownBy(() -> authService.refresh(request))
        .isInstanceOf(ApiException.class);
    verify(redisComponent, never()).deleteKey(any());
  }

  @Test
  @Order(9)
  @DisplayName("토큰 재발급 - Refresh Token 재사용 감지 시 모든 세션 무효화")
  void refresh_reuseDetected() {
    // given
    AuthDto.RefreshRequest request = AuthDto.RefreshRequest.builder()
        .refreshToken("oldToken")
        .build();
    given(jwtTokenProvider.getValidClaims("oldToken"))
        .willReturn(claims("test@example.com", "jti", 60_000));
    given(jwtTokenProvider.getAuthTime("oldToken")).willReturn(Instant.now());
    // Redis에는 이미 교체된 새 토큰이 저장되어 있음
    given(redisComponent.getStringValue("APP:AUTH:REFRESH_TOKEN:1"))
        .willReturn("newToken");

    // when & then
    assertThatThrownBy(() -> authService.refresh(request))
        .isInstanceOf(ApiException.class);
    // 재사용 감지 시 Redis 키 삭제로 모든 세션 무효화
    verify(redisComponent).deleteKey("APP:AUTH:REFRESH_TOKEN:1");
  }

  @Test
  @Order(20)
  @DisplayName("토큰 재발급 - 동시에 들어온 다른 요청이 먼저 교체하면 실패 (세션은 유지)")
  void refresh_concurrentReplaceFails() {
    // given
    AuthDto.RefreshRequest request = AuthDto.RefreshRequest.builder()
        .refreshToken("validToken")
        .build();
    Member member = Member.builder().id(1L).email("test@example.com").name("테스트")
        .password("encodedPassword").build();
    given(jwtTokenProvider.getValidClaims("validToken"))
        .willReturn(claims("test@example.com", "jti", 60_000));
    given(redisComponent.getStringValue("APP:AUTH:REFRESH_TOKEN:1"))
        .willReturn("validToken");
    given(jwtTokenProvider.getAuthTime("validToken")).willReturn(Instant.now());
    given(memberRepository.findById(1L)).willReturn(Optional.of(member));
    given(redisComponent.compareAndSet(any(), any(), any(), anyLong(), any())).willReturn(false);

    // when & then
    assertThatThrownBy(() -> authService.refresh(request))
        .isInstanceOf(ApiException.class);
    // 먼저 성공한 요청의 새 토큰을 지우면 안 된다
    verify(redisComponent, never()).deleteKey(any());
  }

  @Test
  @Order(21)
  @DisplayName("토큰 재발급 - 회원 번호, authTime 이 없는 이전 형식 토큰은 만료 처리")
  void refresh_legacyTokenWithoutAuthTime() {
    // given
    AuthDto.RefreshRequest request = AuthDto.RefreshRequest.builder()
        .refreshToken("legacyToken")
        .build();
    given(jwtTokenProvider.getValidClaims("legacyToken")).willReturn(Jwts.claims()
        .subject("test@example.com")
        .expiration(new Date(System.currentTimeMillis() + 60_000))
        .build());
    given(jwtTokenProvider.getAuthTime("legacyToken")).willReturn(null);

    // when & then
    assertThatThrownBy(() -> authService.refresh(request))
        .isInstanceOf(ApiException.class);
    verify(redisComponent, never()).compareAndSet(any(), any(), any(), anyLong(), any());
  }

  private static Claims claims(String subject, String jti, long validForMs) {
    return Jwts.claims()
        .subject(subject)
        .add("mid", 1L)
        .id(jti)
        .expiration(new Date(System.currentTimeMillis() + validForMs))
        .build();
  }

  @Test
  @Order(10)
  @DisplayName("회원가입 - 성공 (인증 메일 발송)")
  void register_success() {
    // given
    Member savedMember = Member.builder()
        .id(1L)
        .email("new@example.com")
        .name("신규회원")
        .password("encodedPassword")
        .build();
    given(memberRepository.existsByEmail("new@example.com")).willReturn(false);
    given(passwordEncoder.encode("password123")).willReturn("encodedPassword");
    given(memberRepository.save(any(Member.class))).willReturn(savedMember);
    AuthDto.RegisterRequest request = AuthDto.RegisterRequest.builder()
        .email("new@example.com")
        .name("신규회원")
        .password("password123")
        .build();

    // when
    authService.register(request);

    // then
    verify(memberRepository).save(any(Member.class));
    verify(emailService).sendVerificationEmail(eq("new@example.com"), any());
  }

  @Test
  @Order(11)
  @DisplayName("회원가입 - 이메일 중복")
  void register_duplicatedEmail() {
    // given
    AuthDto.RegisterRequest request = AuthDto.RegisterRequest.builder()
        .email("test@example.com")
        .name("테스트")
        .password("password123")
        .build();
    given(memberRepository.existsByEmail("test@example.com")).willReturn(true);

    // when & then
    assertThatThrownBy(() -> authService.register(request))
        .isInstanceOf(ApiException.class);
  }

  @Test
  @Order(12)
  @DisplayName("비밀번호 재설정 요청 - 성공 (회원 존재)")
  void requestPasswordReset_success() {
    // given
    AuthDto.PasswordResetRequest request = AuthDto.PasswordResetRequest.builder()
        .email("test@example.com")
        .build();
    Member member = Member.builder()
        .id(1L)
        .email("test@example.com")
        .name("테스트")
        .password("encodedPassword")
        .build();
    given(memberRepository.findByEmail("test@example.com")).willReturn(Optional.of(member));

    // when
    authService.requestPasswordReset(request);

    // then
    verify(redisComponent).setStringValue(any(), eq("test@example.com"), anyLong(),
        any(TimeUnit.class));
    verify(emailService).sendPasswordResetEmail(eq("test@example.com"), any());
  }

  @Test
  @Order(13)
  @DisplayName("비밀번호 재설정 요청 - 미등록 이메일 (정상 응답, 메일 미발송)")
  void requestPasswordReset_memberNotFound() {
    // given
    AuthDto.PasswordResetRequest request = AuthDto.PasswordResetRequest.builder()
        .email("notfound@example.com")
        .build();
    given(memberRepository.findByEmail("notfound@example.com")).willReturn(Optional.empty());

    // when
    authService.requestPasswordReset(request);

    // then
    verify(emailService, never()).sendPasswordResetEmail(any(), any());
  }

  @Test
  @Order(14)
  @DisplayName("비밀번호 재설정 - 성공")
  void resetPassword_success() {
    // given
    Member member = Member.builder()
        .id(1L)
        .email("test@example.com")
        .name("테스트")
        .password("oldEncodedPassword")
        .build();
    given(redisComponent.getStringValue("APP:AUTH:PASSWORD_RESET:validToken"))
        .willReturn("test@example.com");
    given(memberRepository.findByEmail("test@example.com")).willReturn(Optional.of(member));
    given(passwordEncoder.encode("newPassword123")).willReturn("newEncodedPassword");
    AuthDto.PasswordReset request = AuthDto.PasswordReset.builder()
        .token("validToken")
        .newPassword("newPassword123")
        .build();

    // when
    authService.resetPassword(request);

    // then
    verify(passwordEncoder).encode("newPassword123");
    verify(redisComponent).deleteKey("APP:AUTH:PASSWORD_RESET:validToken");
  }

  @Test
  @Order(15)
  @DisplayName("비밀번호 재설정 - 유효하지 않은 토큰")
  void resetPassword_invalidToken() {
    // given
    AuthDto.PasswordReset request = AuthDto.PasswordReset.builder()
        .token("invalidToken")
        .newPassword("newPassword123")
        .build();
    given(redisComponent.getStringValue("APP:AUTH:PASSWORD_RESET:invalidToken")).willReturn(null);

    // when & then
    assertThatThrownBy(() -> authService.resetPassword(request))
        .isInstanceOf(ApiException.class);
  }

  @Test
  @Order(16)
  @DisplayName("비밀번호 재설정 - 회원 미존재")
  void resetPassword_memberNotFound() {
    // given
    AuthDto.PasswordReset request = AuthDto.PasswordReset.builder()
        .token("validToken")
        .newPassword("newPassword123")
        .build();
    given(redisComponent.getStringValue("APP:AUTH:PASSWORD_RESET:validToken"))
        .willReturn("notfound@example.com");
    given(memberRepository.findByEmail("notfound@example.com")).willReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> authService.resetPassword(request))
        .isInstanceOf(ApiException.class);
  }
}
