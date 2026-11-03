package com.example.api.file.controller;

import com.example.api.common.response.BaseResponse;
import com.example.api.file.dto.FileDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;

/**
 * 파일 Controller API 문서
 */
@Tag(name = "파일", description = "파일 업로드용 Presigned URL 발급 API")
public interface FileControllerDocs {

  /**
   * S3 Presigned URL 발급
   *
   * @param fileName    원본 파일명
   * @param contentType MIME 타입
   * @return Presigned URL 응답
   */
  @Operation(
      summary = "Presigned URL 발급",
      description = "S3에 파일을 업로드하기 위한 Presigned URL을 발급합니다. "
          + "반환된 presignedUrl로 PUT 요청하여 파일을 업로드한 후, "
          + "objectUrl을 Todo 이미지 URL로 저장하세요."
  )
  @ApiResponse(
      responseCode = "200",
      description = "발급 성공",
      content = @Content(
          mediaType = MediaType.APPLICATION_JSON_VALUE,
          schema = @Schema(implementation = FileDto.PresignedUrlResponse.class),
          examples = @ExampleObject(value = """
              {
                "statusCode": "OK",
                "message": "성공",
                "data": {
                  "presignedUrl": "http://localhost:9000/boilerplate-bucket/uuid/image.png?X-Amz-...",
                  "objectUrl": "http://localhost:9000/boilerplate-bucket/uuid/image.png"
                }
              }
              """)
      )
  )
  BaseResponse<FileDto.PresignedUrlResponse> getPresignedUrl(String fileName, String contentType);
}
