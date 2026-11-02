package com.example.api.auth.service;

import com.example.api.auth.dto.AuthDto;
import com.example.api.common.component.RedisComponent;
import com.example.api.common.type.RedisKeys;
import com.example.api.config.jwt.JwtTokenProvider;
import com.example.api.domain.entity.MemberRole;
import com.example.api.domain.repository.MemberRepository;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * 임시 토큰 발급 서비스 (local 전용)
 *
 * <p>비밀번호 없이 이메일만으로 JWT를 즉시 발급한다. 로컬 Swagger 테스트 목적으로만 사용.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Profile("local")
public class TempAuthService {

  private final JwtTokenProvider jwtTokenProvider;
  private final RedisComponent redisComponent;
  private final MemberRepository memberRepository;

  /**
   * 이메일로 JWT 즉시 발급
   *
   * @param request 임시 토큰 발급 요청
   * @return 토큰 응답
   */
  public AuthDto.TokenResponse issueToken(AuthDto.TempTokenRequest request) {
    String subject = request.getEmail();
    String role = memberRepository.findByEmail(subject)
        .map(m -> m.getRole().name())
        .orElse(MemberRole.ROLE_USER.name());

    String accessToken = jwtTokenProvider.createAccessToken(subject, role);
    String refreshToken = jwtTokenProvider.createRefreshToken(subject);

    String redisKey = RedisKeys.REFRESH_TOKEN.getKey() + subject;
    redisComponent.setStringValue(
        redisKey, refreshToken, RedisKeys.REFRESH_TOKEN.getTtl(), TimeUnit.SECONDS);

    log.info("[TempAuth] 임시 토큰 발급: subject={}", subject);

    return AuthDto.TokenResponse.of(accessToken, refreshToken);
  }
}
