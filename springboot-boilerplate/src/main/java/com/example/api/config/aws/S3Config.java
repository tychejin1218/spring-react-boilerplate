package com.example.api.config.aws;

import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * AWS S3 설정
 *
 * <p>로컬 환경에서는 {@code aws.s3.endpoint}가 설정된 경우 MinIO로 연결합니다.
 * 실제 환경에서는 {@link DefaultCredentialsProvider} 체인(환경변수, 시스템 프로퍼티,
 * {@code ~/.aws/credentials} 프로파일, ECS/EKS 컨테이너 자격증명, EC2 인스턴스 프로파일 순)으로
 * 자격증명을 찾으므로 IAM Role 을 붙인 ECS/EKS/EC2 에서는 별도 설정이 필요 없습니다.
 */
@Configuration
public class S3Config {

  @Value("${aws.s3.region}")
  private String region;

  @Value("${aws.s3.endpoint:}")
  private String endpoint;

  @Value("${aws.s3.access-key:}")
  private String accessKey;

  @Value("${aws.s3.secret-key:}")
  private String secretKey;

  /**
   * AWS 자격증명 프로바이더 빈 생성
   *
   * <p>endpoint가 설정된 경우(로컬 MinIO) 정적 자격증명을 사용하고,
   * 그 외에는 {@link DefaultCredentialsProvider} 체인(IAM Role 포함)을 사용합니다.
   *
   * @return AwsCredentialsProvider
   */
  @Bean
  public AwsCredentialsProvider awsCredentialsProvider() {
    if (hasEndpointOverride()) {
      return StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
    }
    return DefaultCredentialsProvider.builder().build();
  }

  /**
   * S3 클라이언트 빈 생성
   *
   * @return S3Client
   */
  @Bean
  public S3Client s3Client(AwsCredentialsProvider awsCredentialsProvider) {
    S3ClientBuilder builder = S3Client.builder()
        .region(Region.of(region))
        .credentialsProvider(awsCredentialsProvider);
    if (hasEndpointOverride()) {
      builder.endpointOverride(URI.create(endpoint))
          .serviceConfiguration(pathStyleConfig());
    }
    return builder.build();
  }

  /**
   * S3 Presigned URL 생성기 빈 생성
   *
   * @return S3Presigner
   */
  @Bean
  public S3Presigner s3Presigner(AwsCredentialsProvider awsCredentialsProvider) {
    S3Presigner.Builder builder = S3Presigner.builder()
        .region(Region.of(region))
        .credentialsProvider(awsCredentialsProvider);
    if (hasEndpointOverride()) {
      builder.endpointOverride(URI.create(endpoint))
          .serviceConfiguration(pathStyleConfig());
    }
    return builder.build();
  }

  private boolean hasEndpointOverride() {
    return endpoint != null && !endpoint.isEmpty();
  }

  private S3Configuration pathStyleConfig() {
    return S3Configuration.builder().pathStyleAccessEnabled(true).build();
  }
}
