package com.example.api.auth.service;

import com.example.api.auth.dto.AuthDto;
import com.example.api.common.component.RedisComponent;
import com.example.api.common.exception.ApiException;
import com.example.api.common.type.ApiStatus;
import com.example.api.common.type.RedisKeys;
import com.example.api.config.jwt.JwtTokenProvider;
import com.example.api.domain.entity.Member;
import com.example.api.domain.repository.MemberRepository;
import io.jsonwebtoken.Claims;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 인증 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

  private final JwtTokenProvider jwtTokenProvider;
  private final RedisComponent redisComponent;
  private final MemberRepository memberRepository;
  private final PasswordEncoder passwordEncoder;
  private final EmailService emailService;

  /**
   * 회원가입 - 이메일 중복 확인 후 저장 및 인증 메일 발송
   *
   * <p>가입 후 이메일 인증을 완료해야 로그인할 수 있습니다.
   *
   * @param request 회원가입 요청 (email, name, password)
   */
  @Transactional
  public void register(AuthDto.RegisterRequest request) {
    if (memberRepository.existsByEmail(request.getEmail())) {
      throw new ApiException(ApiStatus.MEMBER_EMAIL_DUPLICATED);
    }
    String encodedPassword = passwordEncoder.encode(request.getPassword());
    memberRepository.save(Member.of(request.getEmail(), request.getName(), encodedPassword));
    sendVerificationEmail(request.getEmail());
  }

  /**
   * 이메일 인증 - 토큰 검증 후 회원 인증 완료 및 JWT 발급
   *
   * @param token 인증 토큰
   * @return 토큰 응답
   */
  @Transactional
  public AuthDto.TokenResponse verifyEmail(String token) {
    String email = redisComponent.getStringValue(RedisKeys.EMAIL_VERIFY_TOKEN.key(token));

    if (StringUtils.isBlank(email)) {
      throw new ApiException(ApiStatus.AUTH_VERIFY_TOKEN_INVALID);
    }

    Member member = memberRepository.findByEmail(email)
        .orElseThrow(() -> new ApiException(ApiStatus.MEMBER_NOT_FOUND));

    member.verify();
    redisComponent.deleteKey(RedisKeys.EMAIL_VERIFY_TOKEN.key(token));

    return issueTokenPair(member);
  }

  /**
   * 인증 메일 재발송
   *
   * @param request 이메일 요청
   */
  @Transactional(readOnly = true)
  public void resendVerification(AuthDto.ResendVerificationRequest request) {
    Member member = memberRepository.findByEmail(request.getEmail())
        .orElseThrow(() -> new ApiException(ApiStatus.MEMBER_NOT_FOUND));

    if (member.isEmailVerified()) {
      throw new ApiException(ApiStatus.AUTH_EMAIL_ALREADY_VERIFIED);
    }

    sendVerificationEmail(request.getEmail());
  }

  /**
   * 로그인 - 이메일/비밀번호 검증 후 JWT 발급
   *
   * @param request 로그인 요청 (email, password)
   * @return 토큰 응답
   */
  @Transactional(readOnly = true)
  public AuthDto.TokenResponse login(AuthDto.LoginRequest request) {
    var member = memberRepository.findByEmail(request.getEmail())
        .orElseThrow(() -> new ApiException(ApiStatus.AUTH_LOGIN_FAILED));

    if (!passwordEncoder.matches(request.getPassword(), member.getPassword())) {
      throw new ApiException(ApiStatus.AUTH_LOGIN_FAILED);
    }

    if (!member.isEmailVerified()) {
      throw new ApiException(ApiStatus.AUTH_EMAIL_NOT_VERIFIED);
    }

    return issueTokenPair(member);
  }

  /**
   * 로그아웃
   *
   * <p>Redis 의 Refresh 토큰을 지워 재발급을 막고, 현재 Access 토큰은 남은 유효 시간 동안
   * 차단 목록에 올려 바로 쓸 수 없게 한다. 토큰이 이미 만료됐거나 잘못돼도 실패로 응답하지 않는다
   * (사용자 입장에서 로그아웃은 항상 성공해야 한다).
   *
   * @param request     리프레시 토큰 요청
   * @param accessToken 현재 Access 토큰 (Authorization 헤더, 없으면 null)
   */
  public void logout(AuthDto.RefreshRequest request, String accessToken) {
    Claims refreshClaims = jwtTokenProvider.getValidClaims(request.getRefreshToken());
    Long memberId = refreshClaims == null ? null : JwtTokenProvider.memberIdOf(refreshClaims);
    if (memberId != null) {
      redisComponent.deleteKey(RedisKeys.REFRESH_TOKEN.key(memberId));
    }
    denyAccessToken(accessToken);
  }

  /**
   * 토큰 재발급 (Refresh Token Rotation)
   *
   * <ol>
   *   <li>JWT 검증 — 서명과 exp(= 최초 로그인 + 절대 만료)를 확인한다.
   *   <li>Redis 저장값 확인 — 없으면 미활동 만료 또는 로그아웃, 다르면 이미 쓴 토큰의 재사용(탈취 의심)이다.
   *   <li>새 토큰 쌍 발급 — authTime 을 이어받아 절대 만료를 유지한다.
   *   <li>compare-and-set 으로 교체 — 동시에 두 요청이 같은 토큰으로 들어와도 하나만 성공한다.
   *       교체하면서 Redis TTL 을 다시 미활동 만료만큼 연장한다.
   * </ol>
   *
   * @param request 토큰 재발급 요청
   * @return 새 토큰 응답
   */
  @Transactional(readOnly = true)
  public AuthDto.TokenResponse refresh(AuthDto.RefreshRequest request) {
    String refreshToken = request.getRefreshToken();
    Claims claims = jwtTokenProvider.getValidClaims(refreshToken);
    if (claims == null) {
      throw new ApiException(ApiStatus.AUTH_REFRESH_TOKEN_INVALID);
    }

    Long memberId = JwtTokenProvider.memberIdOf(claims);
    Instant authTime = jwtTokenProvider.getAuthTime(refreshToken);
    if (memberId == null || authTime == null) {
      // 회원 번호, 최초 로그인 시각이 없는 이전 형식 토큰 — 만료로 보고 다시 로그인하게 한다
      throw new ApiException(ApiStatus.AUTH_REFRESH_TOKEN_INVALID);
    }

    String redisKey = RedisKeys.REFRESH_TOKEN.key(memberId);
    verifyStoredRefreshToken(redisKey, refreshToken);

    Member member = memberRepository.findById(memberId)
        .orElseThrow(() -> new ApiException(ApiStatus.AUTH_REFRESH_TOKEN_INVALID));

    String newAccessToken = jwtTokenProvider.createAccessToken(
        member.getEmail(), member.getId(), member.getRole().name());
    String newRefreshToken =
        jwtTokenProvider.createRefreshToken(member.getEmail(), member.getId(), authTime);

    boolean replaced = redisComponent.compareAndSet(redisKey, refreshToken, newRefreshToken,
        jwtTokenProvider.getSessionIdleTimeoutMs(), TimeUnit.MILLISECONDS);
    if (!replaced) {
      // 같은 토큰으로 동시에 들어온 다른 요청이 먼저 교체했거나, 그 사이 만료됨
      throw new ApiException(ApiStatus.AUTH_REFRESH_TOKEN_INVALID);
    }

    return AuthDto.TokenResponse.of(newAccessToken, newRefreshToken);
  }

  /**
   * 비밀번호 재설정 요청 - 이메일로 재설정 링크 발송
   *
   * <p>회원이 존재하지 않아도 보안상 동일 응답 반환
   *
   * @param request 비밀번호 재설정 요청 (email)
   */
  @Transactional(readOnly = true)
  public void requestPasswordReset(AuthDto.PasswordResetRequest request) {
    memberRepository.findByEmail(request.getEmail()).ifPresent(member -> {
      String token = UUID.randomUUID().toString();
      redisComponent.setStringValue(
          RedisKeys.PASSWORD_RESET_TOKEN.key(token),
          member.getEmail(),
          RedisKeys.PASSWORD_RESET_TOKEN.getTtl(),
          TimeUnit.SECONDS);
      emailService.sendPasswordResetEmail(member.getEmail(), token);
    });
  }

  /**
   * 비밀번호 재설정 - 토큰 검증 후 비밀번호 변경
   *
   * @param request 비밀번호 재설정 (token, newPassword)
   * @throws ApiException 유효하지 않은 토큰
   */
  @Transactional
  public void resetPassword(AuthDto.PasswordReset request) {
    String email = redisComponent.getStringValue(
        RedisKeys.PASSWORD_RESET_TOKEN.key(request.getToken()));

    if (StringUtils.isBlank(email)) {
      throw new ApiException(ApiStatus.AUTH_PASSWORD_RESET_TOKEN_INVALID);
    }

    Member member = memberRepository.findByEmail(email)
        .orElseThrow(() -> new ApiException(ApiStatus.MEMBER_NOT_FOUND));

    member.changePassword(passwordEncoder.encode(request.getNewPassword()));
    redisComponent.deleteKey(RedisKeys.PASSWORD_RESET_TOKEN.key(request.getToken()));
  }

  private void sendVerificationEmail(String email) {
    String token = UUID.randomUUID().toString();
    redisComponent.setStringValue(
        RedisKeys.EMAIL_VERIFY_TOKEN.key(token),
        email,
        RedisKeys.EMAIL_VERIFY_TOKEN.getTtl(),
        TimeUnit.SECONDS);
    emailService.sendVerificationEmail(email, token);
  }

  /**
   * Redis 에 저장된 Refresh 토큰과 요청 토큰 비교
   *
   * @param redisKey     Refresh 토큰 Redis 키
   * @param refreshToken 요청으로 받은 Refresh 토큰
   * @throws ApiException 저장값이 없으면(미활동 만료, 로그아웃) 또는 다르면(재사용) 401
   */
  private void verifyStoredRefreshToken(String redisKey, String refreshToken) {
    String storedToken = redisComponent.getStringValue(redisKey);
    if (StringUtils.isBlank(storedToken)) {
      // 미활동 만료(Redis TTL) 또는 로그아웃
      throw new ApiException(ApiStatus.AUTH_REFRESH_TOKEN_INVALID);
    }
    if (!storedToken.equals(refreshToken)) {
      // Refresh Token 재사용 감지 — 탈취 가능성으로 모든 세션 무효화
      log.warn("Refresh Token 재사용 감지: 모든 세션 무효화");
      redisComponent.deleteKey(redisKey);
      throw new ApiException(ApiStatus.AUTH_REFRESH_TOKEN_INVALID);
    }
  }

  private AuthDto.TokenResponse issueTokenPair(Member member) {
    String accessToken = jwtTokenProvider.createAccessToken(
        member.getEmail(), member.getId(), member.getRole().name());
    // 로그인 시점이 최초 로그인 시각(authTime)이 된다
    String refreshToken =
        jwtTokenProvider.createRefreshToken(member.getEmail(), member.getId(), Instant.now());

    redisComponent.setStringValue(
        RedisKeys.REFRESH_TOKEN.key(member.getId()),
        refreshToken,
        jwtTokenProvider.getSessionIdleTimeoutMs(),
        TimeUnit.MILLISECONDS);

    return AuthDto.TokenResponse.of(accessToken, refreshToken);
  }

  /**
   * Access 토큰을 남은 유효 시간 동안 차단 목록에 등록
   *
   * <p>키는 토큰의 jti 다. JwtAuthenticationFilter 가 매 요청 이 목록을 확인한다.
   *
   * @param accessToken 차단할 Access 토큰 (null 이거나 이미 만료됐으면 무시)
   */
  private void denyAccessToken(String accessToken) {
    if (StringUtils.isBlank(accessToken)) {
      return;
    }
    Claims claims = jwtTokenProvider.getValidClaims(accessToken);
    if (claims == null || claims.getId() == null) {
      return;
    }
    long remainingMs = claims.getExpiration().getTime() - System.currentTimeMillis();
    if (remainingMs > 0) {
      redisComponent.setStringValue(RedisKeys.ACCESS_TOKEN_DENY.key(claims.getId()), "1",
          remainingMs, TimeUnit.MILLISECONDS);
    }
  }

}
