package com.example.api.config.aws;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.BucketAlreadyExistsException;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;
import software.amazon.awssdk.services.s3.model.CORSConfiguration;
import software.amazon.awssdk.services.s3.model.CORSRule;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.PutBucketCorsRequest;
import software.amazon.awssdk.services.s3.model.PutBucketPolicyRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * 로컬 환경 MinIO 초기화
 *
 * <p>애플리케이션 시작 시 MinIO에 버킷을 생성하고 CORS를 설정합니다.
 * 브라우저에서 Presigned URL로 직접 파일 업로드하기 위해 CORS 설정이 필요합니다.
 */
@Slf4j
@Profile("local")
@Component
@RequiredArgsConstructor
public class LocalS3Initializer implements ApplicationRunner {

  private static final int NOT_IMPLEMENTED = 501;

  private final S3Client s3Client;

  @Value("${aws.s3.bucket-name}")
  private String bucketName;

  @Override
  public void run(ApplicationArguments args) {
    try {
      createBucketIfNotExists();
      configureBucketCors();
      configureBucketPublicRead();
    } catch (SdkException e) {
      log.warn("MinIO 초기화 실패 (MinIO가 실행 중인지 확인하세요): {}", e.getMessage());
    }
  }

  private void createBucketIfNotExists() {
    try {
      s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
      log.info("MinIO 버킷 생성 완료: {}", bucketName);
    } catch (BucketAlreadyOwnedByYouException | BucketAlreadyExistsException e) {
      log.debug("MinIO 버킷이 이미 존재합니다: {}", bucketName);
    }
  }

  private void configureBucketPublicRead() {
    String policy = String.format("""
        {"Version":"2012-10-17","Statement":[{"Effect":"Allow","Principal":"*",\
        "Action":"s3:GetObject","Resource":"arn:aws:s3:::%s/*"}]}""", bucketName);
    s3Client.putBucketPolicy(PutBucketPolicyRequest.builder()
        .bucket(bucketName)
        .policy(policy)
        .build());
    log.info("MinIO 버킷 공개 읽기 정책 설정 완료: {}", bucketName);
  }

  /**
   * 버킷 CORS 설정
   *
   * <p>MinIO 는 버킷 단위 CORS API(PutBucketCors)를 지원하지 않아 501 을 반환한다.
   * MinIO 는 서버 전역 설정(MINIO_API_CORS_ALLOW_ORIGIN, 기본값 "*")으로 CORS 를 처리하므로
   * 501 은 무시하고 이후 정책 설정을 계속 진행한다.
   */
  private void configureBucketCors() {
    CORSRule corsRule = CORSRule.builder()
        .allowedHeaders(List.of("*"))
        .allowedMethods(List.of("GET", "PUT", "POST", "DELETE", "HEAD"))
        .allowedOrigins(List.of("*"))
        .exposeHeaders(List.of("ETag"))
        .maxAgeSeconds(3000)
        .build();
    try {
      s3Client.putBucketCors(PutBucketCorsRequest.builder()
          .bucket(bucketName)
          .corsConfiguration(CORSConfiguration.builder().corsRules(corsRule).build())
          .build());
      log.info("MinIO 버킷 CORS 설정 완료: {}", bucketName);
    } catch (S3Exception e) {
      if (e.statusCode() != NOT_IMPLEMENTED) {
        throw e;
      }
      log.info("MinIO 는 버킷 CORS API 를 지원하지 않아 서버 전역 CORS 설정을 사용합니다: {}", bucketName);
    }
  }
}
