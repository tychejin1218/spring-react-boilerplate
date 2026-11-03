package com.example.api.admin.controller;

import com.example.api.admin.dto.AdminDto;
import com.example.api.common.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.MediaType;

/**
 * 관리자 Controller API 문서
 */
@Tag(name = "관리자", description = "관리자 전용 API (ROLE_ADMIN 필요)")
public interface AdminControllerDocs {

  /**
   * 전체 회원 목록 조회
   *
   * @return 회원 목록
   */
  @Operation(
      summary = "회원 목록 조회 (관리자)",
      description = "전체 회원 목록을 조회합니다. ROLE_ADMIN 권한이 필요합니다."
  )
  @ApiResponse(
      responseCode = "200",
      description = "조회 성공",
      content = @Content(
          mediaType = MediaType.APPLICATION_JSON_VALUE,
          schema = @Schema(implementation = AdminDto.MemberResponse.class),
          examples = @ExampleObject(value = """
              {
                "statusCode": "OK",
                "message": "성공",
                "data": [
                  {"id": 1, "email": "user1@example.com", "name": "홍길동", "role": "ROLE_USER"},
                  {"id": 2, "email": "admin@example.com", "name": "관리자", "role": "ROLE_ADMIN"}
                ]
              }
              """)
      )
  )
  BaseResponse<List<AdminDto.MemberResponse>> getMemberList();
}
