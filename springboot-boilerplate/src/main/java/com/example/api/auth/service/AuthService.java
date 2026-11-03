package com.example.api.auth.service;

import com.example.api.auth.dto.AuthDto;
import com.example.api.common.component.RedisComponent;
import com.example.api.common.exception.ApiException;
import com.example.api.common.type.ApiStatus;
import com.example.api.common.type.RedisKeys;
import com.example.api.config.jwt.JwtTokenProvider;
import com.example.api.domain.entity.Member;
import com.example.api.domain.repository.MemberRepository;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
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
      throw new ApiException(HttpStatus.CONFLICT, ApiStatus.DUPLICATED_REQUEST);
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
    String email = redisComponent.getStringValue(RedisKeys.EMAIL_VERIFY_TOKEN.getKey() + token);

    if (StringUtils.isBlank(email)) {
      throw new ApiException(HttpStatus.BAD_REQUEST, ApiStatus.INVALID_REQUEST);
    }

    Member member = memberRepository.findByEmail(email)
        .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, ApiStatus.UNAUTHORIZED));

    member.verify();
    redisComponent.deleteKey(RedisKeys.EMAIL_VERIFY_TOKEN.getKey() + token);

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
        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ApiStatus.NOT_FOUND));

    if (member.isEmailVerified()) {
      throw new ApiException(HttpStatus.BAD_REQUEST, ApiStatus.INVALID_REQUEST);
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
        .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, ApiStatus.UNAUTHORIZED));

    if (!passwordEncoder.matches(request.getPassword(), member.getPassword())) {
      throw new ApiException(HttpStatus.UNAUTHORIZED, ApiStatus.UNAUTHORIZED);
    }

    if (!member.isEmailVerified()) {
      throw new ApiException(HttpStatus.FORBIDDEN, ApiStatus.FORBIDDEN_REQUEST);
    }

    return issueTokenPair(member);
  }

  /**
   * 로그아웃 - Redis에서 Refresh 토큰 삭제
   *
   * @param request 리프레시 토큰 요청
   */
  @Transactional
  public void logout(AuthDto.RefreshRequest request) {
    String refreshToken = request.getRefreshToken();

    if (!jwtTokenProvider.validateToken(refreshToken)) {
      throw new ApiException(HttpStatus.UNAUTHORIZED, ApiStatus.UNAUTHORIZED);
    }

    String subject = jwtTokenProvider.getSubject(refreshToken);
    redisComponent.deleteKey(RedisKeys.REFRESH_TOKEN.getKey() + subject);
  }

  /**
   * 토큰 재발급 - Refresh 토큰 검증 후 Access/Refresh 재발급
   *
   * @param request 토큰 재발급 요청
   * @return 새 토큰 응답
   */
  @Transactional
  public AuthDto.TokenResponse refresh(AuthDto.RefreshRequest request) {
    String refreshToken = request.getRefreshToken();

    if (!jwtTokenProvider.validateToken(refreshToken)) {
      throw new ApiException(HttpStatus.UNAUTHORIZED, ApiStatus.UNAUTHORIZED);
    }

    String subject = jwtTokenProvider.getSubject(refreshToken);
    String storedToken = redisComponent.getStringValue(RedisKeys.REFRESH_TOKEN.getKey() + subject);

    if (StringUtils.isBlank(storedToken)) {
      throw new ApiException(HttpStatus.UNAUTHORIZED, ApiStatus.UNAUTHORIZED);
    }

    if (!storedToken.equals(refreshToken)) {
      // Refresh Token 재사용 감지 — 탈취 가능성으로 모든 세션 무효화
      log.warn("Refresh Token 재사용 감지 (email={}): 모든 세션 무효화", subject);
      redisComponent.deleteKey(RedisKeys.REFRESH_TOKEN.getKey() + subject);
      throw new ApiException(HttpStatus.UNAUTHORIZED, ApiStatus.UNAUTHORIZED);
    }

    Member member = memberRepository.findByEmail(subject)
        .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, ApiStatus.UNAUTHORIZED));

    return issueTokenPair(member);
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
          RedisKeys.PASSWORD_RESET_TOKEN.getKey() + token,
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
        RedisKeys.PASSWORD_RESET_TOKEN.getKey() + request.getToken());

    if (StringUtils.isBlank(email)) {
      throw new ApiException(HttpStatus.BAD_REQUEST, ApiStatus.INVALID_REQUEST);
    }

    Member member = memberRepository.findByEmail(email)
        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ApiStatus.NOT_FOUND));

    member.changePassword(passwordEncoder.encode(request.getNewPassword()));
    redisComponent.deleteKey(RedisKeys.PASSWORD_RESET_TOKEN.getKey() + request.getToken());
  }

  private void sendVerificationEmail(String email) {
    String token = UUID.randomUUID().toString();
    redisComponent.setStringValue(
        RedisKeys.EMAIL_VERIFY_TOKEN.getKey() + token,
        email,
        RedisKeys.EMAIL_VERIFY_TOKEN.getTtl(),
        TimeUnit.SECONDS);
    emailService.sendVerificationEmail(email, token);
  }

  private AuthDto.TokenResponse issueTokenPair(Member member) {
    String accessToken = jwtTokenProvider.createAccessToken(
        member.getEmail(), member.getId(), member.getRole().name());
    String refreshToken = jwtTokenProvider.createRefreshToken(member.getEmail());

    redisComponent.setStringValue(
        RedisKeys.REFRESH_TOKEN.getKey() + member.getEmail(),
        refreshToken,
        RedisKeys.REFRESH_TOKEN.getTtl(),
        TimeUnit.SECONDS);

    return AuthDto.TokenResponse.of(accessToken, refreshToken);
  }
}
