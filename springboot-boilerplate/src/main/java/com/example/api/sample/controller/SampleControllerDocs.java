package com.example.api.sample.controller;

import com.example.api.common.response.BaseResponse;
import com.example.api.common.response.ErrorResponse;
import com.example.api.sample.dto.SampleDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.MediaType;

/**
 * 샘플 API Swagger 문서 인터페이스
 *
 * <p>Swagger 어노테이션을 이 인터페이스에 정의하고,
 * {@link SampleController}가 implements하여 비즈니스 로직과 분리한다
 */
@Tag(name = "Sample", description = "샘플 API")
public interface SampleControllerDocs {

  /**
   * QueryDSL을 이용한 회원 목록 조회
   *
   * @param name  이름 검색어
   * @param email 이메일 검색어
   * @return 회원 목록
   */
  @Operation(summary = "회원 목록 조회 (QueryDSL)", description = "이름·이메일로 회원 목록을 동적 검색합니다.")
  @ApiResponse(
      responseCode = "200",
      description = "조회 성공",
      content = @Content(
          mediaType = MediaType.APPLICATION_JSON_VALUE,
          schema = @Schema(implementation = SampleDto.MemberResponse.class),
          examples = @ExampleObject(value = """
              {
                "statusCode": "OK",
                "message": "성공",
                "data": [
                  {"id": 1, "name": "홍길동", "email": "hong@example.com"},
                  {"id": 2, "name": "김철수", "email": "kim@example.com"}
                ]
              }
              """)
      )
  )
  BaseResponse<List<SampleDto.MemberResponse>> getMemberList(String name, String email);

  /**
   * 회원 단건 조회
   *
   * @param id 회원 ID
   * @return 회원 정보
   */
  @Operation(summary = "회원 단건 조회", description = "ID로 회원을 조회합니다.")
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "조회 성공",
          content = @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = SampleDto.MemberResponse.class),
              examples = @ExampleObject(value = """
                  {
                    "statusCode": "OK",
                    "message": "성공",
                    "data": {"id": 1, "name": "홍길동", "email": "hong@example.com"}
                  }
                  """)
          )
      ),
      @ApiResponse(
          responseCode = "404",
          description = "회원 없음",
          content = @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = ErrorResponse.class),
              examples = @ExampleObject(value = """
                  {
                    "statusCode": "RESOURCE_NOT_FOUND",
                    "message": "존재하지 않는 정보입니다.",
                    "method": "GET",
                    "path": "/sample/member/1",
                    "timestamp": "20260219120000"
                  }
                  """)
          )
      )
  })
  BaseResponse<SampleDto.MemberResponse> getMember(long id);

  /**
   * JPA Repository를 이용한 회원 목록 조회
   *
   * @param name  이름 검색어
   * @param email 이메일 검색어
   * @return 회원 목록
   */
  @Operation(summary = "회원 목록 조회 (JPA Repository)",
      description = "JPA Repository를 이용한 회원 목록 조회입니다.")
  @ApiResponse(
      responseCode = "200",
      description = "조회 성공",
      content = @Content(
          mediaType = MediaType.APPLICATION_JSON_VALUE,
          schema = @Schema(implementation = SampleDto.MemberResponse.class),
          examples = @ExampleObject(value = """
              {
                "statusCode": "OK",
                "message": "성공",
                "data": [{"id": 1, "name": "홍길동", "email": "hong@example.com"}]
              }
              """)
      )
  )
  BaseResponse<List<SampleDto.MemberResponse>> getMembersWithRepository(
      String name, String email);

  /**
   * MyBatis Mapper를 이용한 회원 목록 조회
   *
   * @param name  이름 검색어
   * @param email 이메일 검색어
   * @return 회원 목록
   */
  @Operation(summary = "회원 목록 조회 (MyBatis Mapper)",
      description = "MyBatis Mapper를 이용한 회원 목록 조회입니다.")
  @ApiResponse(
      responseCode = "200",
      description = "조회 성공",
      content = @Content(
          mediaType = MediaType.APPLICATION_JSON_VALUE,
          schema = @Schema(implementation = SampleDto.MemberResponse.class),
          examples = @ExampleObject(value = """
              {
                "statusCode": "OK",
                "message": "성공",
                "data": [{"id": 1, "name": "홍길동", "email": "hong@example.com"}]
              }
              """)
      )
  )
  BaseResponse<List<SampleDto.MemberResponse>> getMembersWithMapper(String name, String email);
}
