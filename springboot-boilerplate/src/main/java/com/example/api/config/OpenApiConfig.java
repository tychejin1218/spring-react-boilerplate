package com.example.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SpringDoc OpenAPI 설정
 *
 * <p>프로파일별 활성화 여부는 {@code springdoc.api-docs.enabled}, {@code springdoc.swagger-ui.enabled}로
 * 제어한다. application.yml 기본값은 꺼짐이고, local/dev/stg 의 {@code environment-{profile}.yml} 에서만 켠다.
 * prd 는 꺼진 채로 둔다 (API 구조와 파라미터가 외부에 노출되지 않도록)
 *
 * <p>모듈별 {@link GroupedOpenApi} 빈을 등록하여 Swagger UI에서 그룹으로 분리한다
 * 새 모듈 추가 시 해당 모듈의 {@link GroupedOpenApi} 빈을 이 클래스에 추가한다
 */
@Configuration
public class OpenApiConfig {

  private static final String BEARER_AUTH = "bearerAuth";

  /**
   * OpenAPI 기본 정보 및 JWT Bearer 인증 스키마 설정
   *
   * <p>Swagger UI 우상단 "Authorize" 버튼에서 JWT 토큰을 입력하면
   * 이후 모든 API 요청에 Authorization: Bearer {token} 헤더가 자동으로 추가된다
   *
   * @return OpenAPI 인스턴스
   */
  @Bean
  public OpenAPI openApi() {
    return new OpenAPI()
        .info(new Info()
            .title("API")
            .version("v1.0.0")
            .description("백엔드 API 문서"))
        .components(new Components()
            .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")))
        .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
  }

  /**
   * Sample 모듈 API 그룹
   *
   * @return GroupedOpenApi 인스턴스
   */
  @Bean
  public GroupedOpenApi sampleApi() {
    return GroupedOpenApi.builder()
        .group("샘플 API")
        .pathsToMatch("/sample/**")
        .build();
  }

  /**
   * 인증 모듈 API 그룹
   *
   * @return GroupedOpenApi 인스턴스
   */
  @Bean
  public GroupedOpenApi authApi() {
    return GroupedOpenApi.builder()
        .group("인증 API")
        .pathsToMatch("/auth/**")
        .build();
  }

  /**
   * 회원 모듈 API 그룹
   *
   * @return GroupedOpenApi 인스턴스
   */
  @Bean
  public GroupedOpenApi userApi() {
    return GroupedOpenApi.builder()
        .group("회원 API")
        .pathsToMatch("/users/**")
        .build();
  }

  /**
   * 할 일 모듈 API 그룹
   *
   * @return GroupedOpenApi 인스턴스
   */
  @Bean
  public GroupedOpenApi todoApi() {
    return GroupedOpenApi.builder()
        .group("할 일 API")
        .pathsToMatch("/todos/**")
        .build();
  }
}
