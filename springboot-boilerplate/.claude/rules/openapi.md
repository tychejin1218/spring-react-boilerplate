---
paths:
  - "springboot-boilerplate/src/main/java/**/controller/**/*.java"
  - "src/main/java/**/controller/**/*.java"
  - "springboot-boilerplate/src/main/java/**/dto/**/*.java"
  - "src/main/java/**/dto/**/*.java"
  - "springboot-boilerplate/src/main/java/**/config/OpenApiConfig.java"
  - "src/main/java/**/config/OpenApiConfig.java"
---
# OpenAPI (SpringDoc) 가이드

## 의존성

```gradle
implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:3.2.0'
```

Spring Boot 4.x → springdoc-openapi 3.x 사용 (2.x는 Spring Boot 3.x 전용)

---

## 프로파일별 활성화

`application.yml` 에서 기본으로 끄고, `environment-{profile}.yml` 에서 켤 프로파일만 켠다.
새 프로파일을 추가해도 명시적으로 켜기 전까지는 API 문서가 노출되지 않는다.

| 프로파일 | Swagger UI | API Docs |
|----------|-----------|---------|
| local | 활성화 | 활성화 |
| dev | 활성화 | 활성화 |
| stg | 활성화 | 활성화 |
| prd | **비활성화** | **비활성화** |

```yaml
# application.yml (기본값: 꺼짐)
springdoc:
  api-docs:
    enabled: false
  swagger-ui:
    enabled: false

# environment-local.yml / environment-dev.yml / environment-stg.yml
springdoc:
  api-docs:
    enabled: true
    path: /api-docs
    groups:
      enabled: true
  swagger-ui:
    path: /swagger-ui.html
    enabled: true
    groups-order: asc
    tags-sorter: alpha
    operations-sorter: alpha
    filter: true
    display-request-duration: true
    doc-expansion: none
  cache:
    disabled: true
  override-with-generic-response: false
  default-consumes-media-type: application/json;charset=UTF-8
  default-produces-media-type: application/json;charset=UTF-8

# environment-prd.yml
springdoc:
  swagger-ui:
    enabled: false
  api-docs:
    enabled: false
```

활성화 여부는 yml 프로퍼티로만 제어하며, `@Profile`은 사용하지 않는다.

---

## OpenApiConfig 구조

**위치**: `config/OpenApiConfig.java`

```java
@Configuration
public class OpenApiConfig {

  /** OpenAPI 기본 정보 설정 */
  @Bean
  public OpenAPI openApi() {
    return new OpenAPI()
        .info(new Info()
            .title("API")
            .version("v1.0.0")
            .description("백엔드 API 문서"));
  }

  /** 패키지별 GroupedOpenApi — 새 패키지 추가 시 빈 등록 */
  @Bean
  public GroupedOpenApi sampleApi() {
    return GroupedOpenApi.builder()
        .group("sample")
        .pathsToMatch("/sample/**")
        .build();
  }
}
```

새 패키지 추가 시 `GroupedOpenApi` 빈을 이 클래스에 추가한다. 그룹명은 `"한글명 API"` 형식으로 작성한다.

```java
@Bean
public GroupedOpenApi sampleApi() {
  return GroupedOpenApi.builder()
      .group("샘플 API")
      .pathsToMatch("/sample/**")
      .build();
}
```

---

## ControllerDocs 인터페이스 패턴

Swagger 어노테이션은 Controller에 직접 작성하지 않는다.
`XxxControllerDocs` 인터페이스에 모든 Swagger 어노테이션을 정의하고, Controller가 implements한다.

```
XxxControllerDocs (interface)  ← @Tag, @Operation, @ApiResponse 등 Swagger 어노테이션 + Javadoc
XxxController (class)          ← implements XxxControllerDocs, @Override, Spring MVC 어노테이션, 비즈니스 로직
```

**위치**: `{패키지}/controller/{패키지명}ControllerDocs.java`

### 규칙

- `@Tag`: 인터페이스 클래스에 1개
- `@Operation`: 모든 메서드에 `summary`(간결) + `description`(상세)
- 단일 응답 코드: `@ApiResponse` 직접 사용
- 복수 응답 코드: `@ApiResponses({...})` 사용
- Javadoc은 `ControllerDocs`에만 작성 — `Controller`에서는 제거
- Spring MVC 어노테이션(`@GetMapping`, `@RequestParam` 등)은 Controller에만 작성

### Controller 수정 사항

```java
// Controller에 추가
public class XxxController implements XxxControllerDocs {

  @Override   // PMD 필수
  @GetMapping("/xxx/{id}")
  public BaseResponse<XxxDto.XxxResponse> getXxx(@PathVariable long id) { ... }
}
```

---

## 응답 예시 작성 패턴

### DTO에 @Schema 추가

```java
@Schema(description = "회원 조회 응답")     // 클래스 레벨
public static class MemberResponse {

  @Schema(description = "회원 ID", example = "1")
  private Long id;

  @Schema(description = "이름", example = "홍길동")
  private String name;

  @Schema(description = "이메일", example = "hong@example.com")
  private String email;
}
```

### 단일 응답 코드

```java
@Operation(summary = "회원 목록 조회", description = "이름·이메일로 회원 목록을 동적 검색합니다.")
@ApiResponse(
    responseCode = "200",
    description = "조회 성공",
    content = @Content(
        mediaType = MediaType.APPLICATION_JSON_VALUE,
        schema = @Schema(implementation = XxxDto.XxxResponse.class),
        examples = @ExampleObject(value = """
            {
              "statusCode": "OK",
              "message": "성공",
              "data": [{"id": 1, "name": "홍길동", "email": "hong@example.com"}]
            }
            """)
    )
)
BaseResponse<List<XxxDto.XxxResponse>> getXxxList(String name);
```

### 복수 응답 코드

```java
@Operation(summary = "회원 단건 조회", description = "ID로 회원을 조회합니다.")
@ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "조회 성공",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = XxxDto.XxxResponse.class),
            examples = @ExampleObject(value = """
                {
                  "statusCode": "OK",
                  "message": "성공",
                  "data": {"id": 1, "name": "홍길동"}
                }
                """)
        )
    ),
    @ApiResponse(
        responseCode = "404",
        description = "항목 없음",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = ErrorResponse.class),
            examples = @ExampleObject(value = """
                {
                  "statusCode": "RESOURCE_NOT_FOUND",
                  "message": "존재하지 않는 정보입니다.",
                  "method": "GET",
                  "path": "/xxx/1",
                  "timestamp": "20260219120000"
                }
                """)
        )
    )
})
BaseResponse<XxxDto.XxxResponse> getXxx(long id);
```

### 주의

- `@ExampleObject` value는 실제 `BaseResponse` 래핑 구조 전체를 작성
- 에러 응답 스키마는 `ErrorResponse.class` 참조
- JSON 예시의 각 줄은 **120자 이내** (Checkstyle LineLength 규칙)
  → 멀티라인 텍스트 블록(`"""..."""`)으로 작성

---

## 접근 URL (local 기준)

| 항목 | URL |
|------|-----|
| Swagger UI | `http://localhost:9091/api/swagger-ui/index.html` |
| API Docs JSON | `http://localhost:9091/api/api-docs` |
