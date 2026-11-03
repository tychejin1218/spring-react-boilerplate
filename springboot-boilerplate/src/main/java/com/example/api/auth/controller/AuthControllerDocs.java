package com.example.api.auth.controller;

import com.example.api.auth.dto.AuthDto;
import com.example.api.common.response.BaseResponse;
import com.example.api.common.response.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;

/**
 * 인증 Controller API 문서
 */
@Tag(name = "인증", description = "회원가입, 로그인, 로그아웃, 토큰 재발급 API")
public interface AuthControllerDocs {

  /**
   * 회원가입
   *
   * @param request 회원가입 요청 (email, name, password)
   * @return 성공 응답 (인증 메일 발송)
   */
  @Operation(summary = "회원가입", description = "이메일/이름/비밀번호로 회원가입하고 인증 메일을 발송합니다. 이메일 인증 후 로그인할 수 있습니다.")
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "회원가입 성공 (인증 메일 발송됨)",
          content = @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = BaseResponse.class),
              examples = @ExampleObject(value = """
                  {
                    "statusCode": "OK",
                    "message": "성공",
                    "data": null
                  }
                  """)
          )
      ),
      @ApiResponse(
          responseCode = "409",
          description = "이미 사용 중인 이메일",
          content = @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = ErrorResponse.class)
          )
      )
  })
  BaseResponse<Void> register(AuthDto.RegisterRequest request);

  /**
   * 이메일 인증
   *
   * @param token 인증 토큰 (이메일 링크의 token 파라미터)
   * @return 토큰 응답 (자동 로그인)
   */
  @Operation(summary = "이메일 인증", description = "인증 메일의 링크를 통해 이메일을 인증하고 자동 로그인합니다.")
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "이메일 인증 성공",
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
      ),
      @ApiResponse(
          responseCode = "400",
          description = "유효하지 않거나 만료된 토큰",
          content = @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = ErrorResponse.class)
          )
      )
  })
  BaseResponse<AuthDto.TokenResponse> verifyEmail(String token);

  /**
   * 인증 메일 재발송
   *
   * @param request 이메일 요청
   * @return 성공 응답
   */
  @Operation(summary = "인증 메일 재발송", description = "이메일 인증 메일을 재발송합니다.")
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "재발송 성공",
          content = @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = BaseResponse.class),
              examples = @ExampleObject(value = """
                  {
                    "statusCode": "OK",
                    "message": "성공",
                    "data": null
                  }
                  """)
          )
      ),
      @ApiResponse(
          responseCode = "400",
          description = "이미 인증된 이메일",
          content = @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = ErrorResponse.class)
          )
      )
  })
  BaseResponse<Void> resendVerification(AuthDto.ResendVerificationRequest request);

  /**
   * 로그인
   *
   * @param request 로그인 요청 (email, password)
   * @return 토큰 응답
   */
  @Operation(summary = "로그인", description = "이메일/비밀번호로 accessToken/refreshToken을 발급합니다.")
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "로그인 성공",
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
      ),
      @ApiResponse(
          responseCode = "401",
          description = "인증 실패 (이메일/비밀번호 불일치)",
          content = @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = ErrorResponse.class)
          )
      )
  })
  BaseResponse<AuthDto.TokenResponse> login(AuthDto.LoginRequest request);

  /**
   * 로그아웃
   *
   * @param request       리프레시 토큰 요청
   * @param authorization Authorization 헤더 (Bearer Access 토큰, 없어도 됨)
   * @return 성공 응답
   */
  @Operation(summary = "로그아웃", description = "리프레시 토큰을 지우고 현재 Access 토큰을 차단 목록에 올립니다. "
      + "토큰이 이미 만료됐어도 항상 성공으로 응답합니다.")
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "로그아웃 성공",
          content = @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = BaseResponse.class),
              examples = @ExampleObject(value = """
                  {
                    "statusCode": "OK",
                    "message": "성공",
                    "data": null
                  }
                  """)
          )
      )
  })
  BaseResponse<Void> logout(AuthDto.RefreshRequest request, String authorization);

  /**
   * 토큰 재발급
   *
   * @param request 리프레시 토큰 요청
   * @return 새 토큰 응답
   */
  @Operation(summary = "토큰 재발급", description = "유효한 리프레시 토큰으로 새 accessToken/refreshToken을 재발급합니다.")
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "토큰 재발급 성공",
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
      ),
      @ApiResponse(
          responseCode = "401",
          description = "유효하지 않은 리프레시 토큰",
          content = @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = ErrorResponse.class)
          )
      )
  })
  BaseResponse<AuthDto.TokenResponse> refresh(AuthDto.RefreshRequest request);

  /**
   * 비밀번호 재설정 요청 (이메일 발송)
   *
   * @param request 이메일 요청
   * @return 성공 응답
   */
  @Operation(summary = "비밀번호 재설정 요청",
      description = "이메일로 비밀번호 재설정 링크를 발송합니다. 보안상 미등록 이메일에도 동일 응답을 반환합니다.")
  @ApiResponse(
      responseCode = "200",
      description = "요청 처리 완료",
      content = @Content(
          mediaType = MediaType.APPLICATION_JSON_VALUE,
          schema = @Schema(implementation = BaseResponse.class),
          examples = @ExampleObject(value = """
              {
                "statusCode": "OK",
                "message": "성공",
                "data": null
              }
              """)
      )
  )
  BaseResponse<Void> requestPasswordReset(AuthDto.PasswordResetRequest request);

  /**
   * 비밀번호 재설정
   *
   * @param request 토큰 및 새 비밀번호
   * @return 성공 응답
   */
  @Operation(summary = "비밀번호 재설정",
      description = "재설정 토큰을 검증하고 새 비밀번호를 설정합니다.")
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "비밀번호 재설정 성공",
          content = @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = BaseResponse.class),
              examples = @ExampleObject(value = """
                  {
                    "statusCode": "OK",
                    "message": "성공",
                    "data": null
                  }
                  """)
          )
      ),
      @ApiResponse(
          responseCode = "400",
          description = "유효하지 않거나 만료된 토큰",
          content = @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = ErrorResponse.class)
          )
      )
  })
  BaseResponse<Void> resetPassword(AuthDto.PasswordReset request);
}
