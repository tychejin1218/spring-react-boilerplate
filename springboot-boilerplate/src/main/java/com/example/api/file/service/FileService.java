package com.example.api.file.service;

import com.example.api.common.exception.ApiException;
import com.example.api.common.type.ApiStatus;
import com.example.api.file.dto.FileDto;
import java.net.URL;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

/**
 * 파일 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {

  private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
      "image/jpeg", "image/png", "image/gif", "image/webp"
  );

  private final S3Presigner s3Presigner;

  @Value("${aws.s3.bucket-name}")
  private String bucketName;

  @Value("${aws.s3.presigned-url-expiry-minutes:5}")
  private int expiryMinutes;

  /**
   * S3 Presigned URL 발급
   *
   * <p>반환된 {@code presignedUrl}로 PUT 요청하여 파일을 업로드한 후,
   * {@code objectUrl}을 저장 URL로 사용하세요.
   *
   * @param fileName    원본 파일명
   * @param contentType MIME 타입 (예: image/png) — 이미지 타입만 허용
   * @return Presigned URL 응답
   */
  public FileDto.PresignedUrlResponse getPresignedUrl(String fileName, String contentType) {
    if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
      throw new ApiException(ApiStatus.FILE_CONTENT_TYPE_INVALID);
    }
    String key = UUID.randomUUID() + "/" + fileName;
    PutObjectRequest putObjectRequest = PutObjectRequest.builder()
        .bucket(bucketName)
        .key(key)
        .contentType(contentType)
        .build();
    PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
        .signatureDuration(Duration.ofMinutes(expiryMinutes))
        .putObjectRequest(putObjectRequest)
        .build();
    PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(presignRequest);
    URL url = presigned.url();
    String objectUrl = url.getProtocol() + "://" + url.getAuthority() + url.getPath();
    return FileDto.PresignedUrlResponse.of(url.toString(), objectUrl);
  }
}
