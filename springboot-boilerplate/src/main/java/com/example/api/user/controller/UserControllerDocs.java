package com.example.api.user.controller;

import com.example.api.common.response.BaseResponse;
import com.example.api.common.response.ErrorResponse;
import com.example.api.user.dto.UserDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;

/**
 * 회원 Controller API 문서
 */
@Tag(name = "회원", description = "회원 정보 조회 API")
public interface UserControllerDocs {

  /**
   * 내 정보 조회
   *
   * @param email JWT subject (이메일)
   * @return 내 정보 응답
   */
  @Operation(
      summary = "내 정보 조회",
      description = "액세스 토큰으로 인증된 회원 정보를 반환합니다. (GET /users/me)",
      security = @SecurityRequirement(name = "bearerAuth")
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "조회 성공",
          content = @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = UserDto.MeResponse.class),
              examples = @ExampleObject(value = """
                  {
                    "statusCode": "OK",
                    "message": "성공",
                    "data": {
                      "id": 1,
                      "name": "홍길동",
                      "email": "teacher01@example.com"
                    }
                  }
                  """)
          )
      ),
      @ApiResponse(
          responseCode = "401",
          description = "인증 실패 (토큰 없음 또는 만료)",
          content = @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = ErrorResponse.class)
          )
      )
  })
  BaseResponse<UserDto.MeResponse> getMe(String email);
}
