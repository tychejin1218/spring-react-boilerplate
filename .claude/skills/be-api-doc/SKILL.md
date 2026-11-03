---
name: be-api-doc
description: springboot-boilerplate 전용. Controller에 대한 ControllerDocs 인터페이스를 생성하고 Swagger 어노테이션을 추가합니다.
argument-hint: "<Controller 파일명>"
allowed-tools: Read, Glob, Grep, Write, Edit, Bash
disable-model-invocation: true
---

# BE API 문서 어노테이션 추가

지정된 Controller에 대한 `XxxControllerDocs` 인터페이스를 생성하고, Controller가 이를 implements하도록 수정해주세요.

## 사전 준비

대상 Controller와 연관 DTO 파일을 함께 읽어 요청/응답 구조를 파악하세요.

## 구조

Swagger 어노테이션은 Controller에 직접 작성하지 않는다.
`XxxControllerDocs` 인터페이스에 모든 Swagger 어노테이션을 정의하고,
Controller는 이를 implements하여 비즈니스 로직만 작성한다.

```
XxxControllerDocs (interface)  — @Tag, @Operation, @ApiResponse 등 Swagger 어노테이션
XxxController (class)          — implements XxxControllerDocs, Spring MVC 어노테이션 + 비즈니스 로직
```

## 1. DTO에 @Schema 추가

응답 DTO 클래스와 필드에 `@Schema`를 추가한다.

```java
@Schema(description = "회원 조회 응답")   // 클래스 레벨
public static class XxxResponse {

  @Schema(description = "회원 ID", example = "1")
  private Long id;

  @Schema(description = "이름", example = "홍길동")
  private String name;
}
```

## 2. XxxControllerDocs 인터페이스 생성

**위치**: `{패키지}/controller/{패키지명}ControllerDocs.java`

```java
@Tag(name = "{패키지명}", description = "기능 설명")
public interface XxxControllerDocs {

  // 단일 응답 코드
  @Operation(summary = "단건 조회", description = "ID로 항목을 조회합니다.")
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
  )
  BaseResponse<XxxDto.XxxResponse> getXxx(long id);

  // 복수 응답 코드
  @Operation(summary = "단건 조회", description = "ID로 항목을 조회합니다.")
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
  BaseResponse<XxxDto.XxxResponse> getXxxWithError(long id);
}
```

**작성 규칙**:
- `@Tag`: 인터페이스에 1개
- `@Operation`: 모든 메서드에 `summary`(간결) + `description`(상세)
- 단일 응답 코드: `@ApiResponse` 직접 사용
- 복수 응답 코드: `@ApiResponses({...})` 사용
- `examples`: `@ExampleObject(value = """...""")` 텍스트 블록으로 실제 응답 JSON 작성
- 에러 응답 스키마: `ErrorResponse.class` 참조
- Spring MVC 어노테이션(`@GetMapping`, `@RequestParam` 등)은 작성하지 않음
- Javadoc은 Controller의 것과 동일하게 작성, Controller에서는 Javadoc 제거

## 3. XxxController 수정

```java
public class XxxController implements XxxControllerDocs {

  @Override
  @GetMapping("/xxx/{id}")
  public BaseResponse<XxxDto.XxxResponse> getXxx(@PathVariable long id) {
    // 비즈니스 로직만
  }
}
```

- `implements XxxControllerDocs` 추가
- 기존 Swagger 어노테이션 제거
- 인터페이스 구현 메서드에 `@Override` 필수 (PMD 규칙)
- 메서드 Javadoc 제거 (ControllerDocs에서 상속)

## 4. OpenApiConfig에 GroupedOpenApi 등록

**위치**: `config/OpenApiConfig.java`

```java
@Bean
public GroupedOpenApi xxxApi() {
  return GroupedOpenApi.builder()
      .group("{패키지명}")
      .pathsToMatch("/{패키지URL}/**")
      .build();
}
```

## 5. 후처리

- JSON 예시 값의 각 줄이 120자를 넘지 않도록 멀티라인으로 작성
- 완료 후 `/be-check $ARGUMENTS` 스킬을 실행하세요 (포맷 정렬 + 정적 분석)

## 접근 URL (local/dev/stg)

- Swagger UI: `http://localhost:9091/api/swagger-ui/index.html`
- API Docs JSON: `http://localhost:9091/api/api-docs`

---

대상: $ARGUMENTS
