package com.example.api.auth.service;

import com.example.api.auth.dto.AuthDto;
import com.example.api.common.component.RedisComponent;
import com.example.api.common.exception.ApiException;
import com.example.api.common.type.ApiStatus;
import com.example.api.common.type.RedisKeys;
import com.example.api.config.jwt.JwtTokenProvider;
import com.example.api.domain.entity.Member;
import com.example.api.domain.repository.MemberRepository;
import java.time.Instant;
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
    // Refresh 토큰 Redis 키가 회원 번호 기준이라 가입된 회원만 발급한다
    Member member = memberRepository.findByEmail(subject)
        .orElseThrow(() -> new ApiException(ApiStatus.MEMBER_NOT_FOUND));

    String accessToken =
        jwtTokenProvider.createAccessToken(subject, member.getId(), member.getRole().name());
    String refreshToken =
        jwtTokenProvider.createRefreshToken(subject, member.getId(), Instant.now());

    String redisKey = RedisKeys.REFRESH_TOKEN.key(member.getId());
    redisComponent.setStringValue(redisKey, refreshToken,
        jwtTokenProvider.getSessionIdleTimeoutMs(), TimeUnit.MILLISECONDS);

    log.info("[TempAuth] 임시 토큰 발급: subject={}", subject);

    return AuthDto.TokenResponse.of(accessToken, refreshToken);
  }
}
