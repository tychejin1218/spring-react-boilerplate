package com.example.api.todo.controller;

import com.example.api.common.response.BaseResponse;
import com.example.api.common.response.ErrorResponse;
import com.example.api.common.response.PageResponse;
import com.example.api.todo.dto.TodoDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;

/**
 * 할 일 Controller API 문서
 */
@Tag(name = "할 일", description = "할 일 목록 조회/추가/완료/삭제 API")
public interface TodoControllerDocs {

  /**
   * 내 할 일 목록 조회 (페이지네이션 + 상태 필터)
   *
   * @param authentication 인증 정보
   * @param request        조회 조건 (page, size, status)
   * @return 페이지 응답
   */
  @Operation(
      summary = "할 일 목록 조회",
      description = "인증된 회원의 할 일 목록을 최신순으로 페이지네이션하여 조회합니다."
  )
  @ApiResponse(
      responseCode = "200",
      description = "조회 성공",
      content = @Content(
          mediaType = MediaType.APPLICATION_JSON_VALUE,
          schema = @Schema(implementation = TodoDto.TodoResponse.class),
          examples = @ExampleObject(value = """
              {
                "statusCode": "OK",
                "message": "성공",
                "data": {
                  "content": [
                    {"id": 2, "title": "리액트 공부하기", "completed": false},
                    {"id": 1, "title": "스프링 부트 공부하기", "completed": true}
                  ],
                  "page": 0,
                  "size": 10,
                  "totalElements": 2,
                  "totalPages": 1,
                  "last": true
                }
              }
              """)
      )
  )
  BaseResponse<PageResponse<TodoDto.TodoResponse>> getTodoList(
      Authentication authentication, TodoDto.TodoListRequest request);

  /**
   * 할 일 추가
   *
   * @param authentication 인증 정보
   * @param request        할 일 생성 요청
   * @return 생성된 할 일
   */
  @Operation(summary = "할 일 추가", description = "새 할 일을 추가합니다.")
  @ApiResponse(
      responseCode = "200",
      description = "추가 성공",
      content = @Content(
          mediaType = MediaType.APPLICATION_JSON_VALUE,
          schema = @Schema(implementation = TodoDto.TodoResponse.class),
          examples = @ExampleObject(value = """
              {
                "statusCode": "OK",
                "message": "성공",
                "data": {"id": 3, "title": "JPA 공부하기", "completed": false}
              }
              """)
      )
  )
  BaseResponse<TodoDto.TodoResponse> insertTodo(Authentication authentication,
      TodoDto.CreateRequest request);

  /**
   * 할 일 완료 처리
   *
   * @param authentication 인증 정보
   * @param id             할 일 ID
   * @return 업데이트된 할 일
   */
  @Operation(summary = "할 일 완료 처리", description = "할 일을 완료 상태로 변경합니다.")
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "완료 처리 성공",
          content = @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = TodoDto.TodoResponse.class),
              examples = @ExampleObject(value = """
                  {
                    "statusCode": "OK",
                    "message": "성공",
                    "data": {"id": 1, "title": "스프링 부트 공부하기", "completed": true}
                  }
                  """)
          )
      ),
      @ApiResponse(
          responseCode = "404",
          description = "할 일 없음",
          content = @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = ErrorResponse.class)
          )
      )
  })
  BaseResponse<TodoDto.TodoResponse> updateTodoComplete(Authentication authentication, Long id);

  /**
   * 할 일 이미지 URL 업데이트
   *
   * @param authentication 인증 정보
   * @param id             할 일 ID
   * @param request        이미지 URL 업데이트 요청
   * @return 업데이트된 할 일
   */
  @Operation(
      summary = "할 일 이미지 업데이트",
      description = "Presigned URL로 파일 업로드 완료 후 이미지 URL을 저장합니다."
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "업데이트 성공",
          content = @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = TodoDto.TodoResponse.class),
              examples = @ExampleObject(value = """
                  {
                    "statusCode": "OK",
                    "message": "성공",
                    "data": {
                      "id": 1,
                      "title": "스프링 부트 공부하기",
                      "completed": false,
                      "imageUrl": "http://localhost:9000/boilerplate-bucket/uuid/image.png"
                    }
                  }
                  """)
          )
      ),
      @ApiResponse(
          responseCode = "404",
          description = "할 일 없음",
          content = @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = ErrorResponse.class)
          )
      )
  })
  BaseResponse<TodoDto.TodoResponse> updateTodoImage(
      Authentication authentication, Long id, TodoDto.UpdateImageRequest request);

  /**
   * 할 일 삭제
   *
   * @param authentication 인증 정보
   * @param id             할 일 ID
   * @return 성공 응답
   */
  @Operation(summary = "할 일 삭제", description = "할 일을 삭제합니다.")
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "삭제 성공",
          content = @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = BaseResponse.class),
              examples = @ExampleObject(value = """
                  {"statusCode": "OK", "message": "성공", "data": null}
                  """)
          )
      ),
      @ApiResponse(
          responseCode = "404",
          description = "할 일 없음",
          content = @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = ErrorResponse.class)
          )
      )
  })
  BaseResponse<Void> deleteTodo(Authentication authentication, Long id);
}
