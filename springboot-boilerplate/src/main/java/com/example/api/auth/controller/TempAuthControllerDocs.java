package com.example.api.auth.controller;

import com.example.api.auth.dto.AuthDto;
import com.example.api.common.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;

/**
 * 임시 토큰 발급 Controller API 문서 (local/dev/stg 전용)
 */
@Tag(name = "임시 인증", description = "SSO 없이 JWT를 즉시 발급하는 임시 API (local/dev/stg)")
public interface TempAuthControllerDocs {

  /**
   * 임시 JWT 즉시 발급
   *
   * @param request 이메일 요청
   * @return 토큰 응답
   */
  @Operation(
      summary = "임시 JWT 즉시 발급",
      description = "이메일만으로 accessToken/refreshToken을 즉시 발급합니다. "
          + "Swagger 인증 테스트 전용이며 local/dev/stg 환경에서만 활성화됩니다."
  )
  @ApiResponse(
      responseCode = "200",
      description = "토큰 발급 성공",
      content = @Content(
          mediaType = MediaType.APPLICATION_JSON_VALUE,
          schema = @Schema(implementation = AuthDto.TokenResponse.class),
          examples = @ExampleObject(value = """
              {
                "statusCode": "OK",
                "message": "성공",
                "data": {
                  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
                  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
                }
              }
              """)
      )
  )
  BaseResponse<AuthDto.TokenResponse> issueTempToken(AuthDto.TempTokenRequest request);
}
