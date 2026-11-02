package com.example.api.auth.controller;

import com.example.api.auth.dto.AuthDto;
import com.example.api.auth.service.TempAuthService;
import com.example.api.common.response.BaseResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 임시 토큰 발급 컨트롤러 (local 전용)
 *
 * <p>비밀번호 없이 이메일만으로 JWT를 즉시 발급한다. 로컬 Swagger 테스트 목적으로만 사용한다.
 * 이메일만 알면 관리자 토큰까지 받을 수 있으므로 외부에서 접근 가능한 dev/stg 에서는 켜지 않는다.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Profile("local")
public class TempAuthController implements TempAuthControllerDocs {

  private final TempAuthService tempAuthService;

  @Override
  @PostMapping("/token/temp")
  public BaseResponse<AuthDto.TokenResponse> issueTempToken(
      @RequestBody @Valid AuthDto.TempTokenRequest request) {
    return BaseResponse.ok(tempAuthService.issueToken(request));
  }
}
