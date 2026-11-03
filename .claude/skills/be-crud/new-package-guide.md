# 새 패키지 생성 가이드

새 기능 패키지 추가 시 `sample` 패키지를 참조하여 아래 순서대로 진행한다.

이 문서는 작업 절차다. `/be-crud` (`be-crud-generator` 에이전트)가 읽고, 직접 패키지를 만들 때도 이 순서를 따른다.
각 단계의 규칙 원본은 `springboot-boilerplate/.claude/rules/` 에 있다 (DTO: naming-conventions.md, 데이터 접근: data-access.md,
ControllerDocs: openapi.md, 예외: api-response.md). 둘이 다르면 rules 를 따르고 이 문서를 고친다.

## 1. Entity 생성

**위치**: `domain/entity/{EntityName}.java`

```
@Getter @Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString
@Table(name = "snake_case_table")
@Entity
```

- PK: `@Id @GeneratedValue(strategy = GenerationType.IDENTITY)`
- 컬럼: `@Column(name = "snake_case", nullable = false)`
- 연관관계: `@ManyToOne(fetch = LAZY)` + `@JoinColumn`, `@OneToMany(mappedBy = "...", fetch = LAZY)`
- 컬렉션 필드: `@Builder.Default` + `@ToString.Exclude`
- 참조: `domain/entity/Member.java`, `domain/entity/Todo.java`

## 2. DTO 생성

**위치**: `{패키지}/dto/{패키지명}Dto.java`

outer class 내 static inner class로 정의:

```java
public class {패키지명}Dto {

  @Getter @Builder @NoArgsConstructor @AllArgsConstructor
  @Alias("XxxRequest")   // MyBatis 사용 시 필수
  public static class XxxRequest {
    private Long id;     // 단건 조회 전용 — id 필드만
  }

  @Getter @Builder @NoArgsConstructor @AllArgsConstructor
  @Alias("XxxListRequest")
  public static class XxxListRequest {
    // 검색·필터 조건 필드 + @Valid 어노테이션
  }

  @Getter @Builder @NoArgsConstructor @AllArgsConstructor
  @Alias("XxxResponse")  // MyBatis 사용 시 필수
  public static class XxxResponse {
    // 응답 필드
    public static XxxResponse from({Entity} entity) { ... }
  }
}
```

**DTO 네이밍 패턴**:
- 단건 조회 요청: `XxxRequest` (id만)
- 목록 조회 요청: `XxxListRequest` (검색·필터 조건)
- 응답: `XxxResponse`
- 생성: `InsertXxxRequest`
- 수정: `UpdateXxxRequest`
- 삭제: `DeleteXxxRequest` / `DeleteXxxResponse`

## 3. 데이터 접근 계층 (필요한 것만 선택)

### 3-1. JPA Repository (단순 CRUD)

**위치**: `domain/repository/{EntityName}Repository.java`

```java
public interface {EntityName}Repository extends JpaRepository<{EntityName}, Long> {
  // Spring Data JPA Query Method
}
```

### 3-2. QueryDSL Repository (동적 쿼리, 복잡한 조건)

**위치**: `{패키지}/repository/{패키지명}Repository.java` (JPA Repository 와 이름이 같아지면 `{패키지명}QueryRepository`, 예: `TodoQueryRepository`)

```java
@Repository
@RequiredArgsConstructor
public class {패키지명}Repository {

  private final JPAQueryFactory jpaQueryFactory;

  // 단건 조회
  public {Dto}.XxxResponse selectXxx({Dto}.XxxRequest request) {
    Q{Entity} entity = Q{Entity}.{entity};
    return jpaQueryFactory
        .select(Projections.fields({Dto}.XxxResponse.class,
            entity.field1, entity.field2))
        .from(entity)
        .where(entity.id.eq(request.getId()))
        .setHint(Constants.HIBERNATE_SQL_COMMENT, "{Repository}.selectXxx")
        .fetchOne();
  }

  // 목록 조회 — BooleanBuilder로 동적 조건
  public List<{Dto}.XxxResponse> selectXxxList({Dto}.XxxListRequest request) {
    Q{Entity} entity = Q{Entity}.{entity};
    BooleanBuilder builder = new BooleanBuilder();
    // if (StringUtils.hasText(request.getField())) builder.and(...);
    return jpaQueryFactory
        .select(Projections.fields({Dto}.XxxResponse.class,
            entity.field1, entity.field2))
        .from(entity)
        .where(builder)
        .orderBy(entity.id.desc())
        .setHint(Constants.HIBERNATE_SQL_COMMENT, "{Repository}.selectXxxList")
        .fetch();
  }
}
```

- `Projections.fields()`로 DTO 직접 매핑
- `BooleanBuilder`로 동적 WHERE 조합
- `setHint(Constants.HIBERNATE_SQL_COMMENT, "클래스.메서드")`로 SQL 추적
- **주의**: QueryDSL insert는 PK 미반환 → PK 필요 시 JPA Repository 사용

### 3-3. MyBatis Mapper (네이티브 SQL, 프로시저)

**인터페이스 위치**: `{패키지}/mapper/{패키지명}Mapper.java`

```java
@Mapper
public interface {패키지명}Mapper {
  List<{Dto}.XxxResponse> selectXxxList({Dto}.XxxRequest request);
}
```

**XML 위치**: `resources/mapper/{패키지명}/{패키지명}Mapper.xml`

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">

<mapper namespace="com.example.api.{패키지}.mapper.{패키지명}Mapper">
    <select id="selectXxxList" parameterType="XxxRequest" resultType="XxxResponse">
        <!-- SQL -->
    </select>
</mapper>
```

- `parameterType` / `resultType`에는 `@Alias` 값 사용

## 4. Service 생성

**위치**: `{패키지}/service/{패키지명}Service.java`

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class {패키지명}Service {

  private final {EntityName}Repository {entityName}Repository;  // JPA (필요시)
  private final {패키지명}Repository {패키지명}Repository;            // QueryDSL (필요시)
  private final {패키지명}Mapper {패키지명}Mapper;                    // MyBatis (필요시)
```

**트랜잭션 규칙**:
- 조회: `@Transactional(readOnly = true)` → Reader DB 라우팅
- 변경: `@Transactional` → Writer DB 라우팅

**예외 처리**:
```java
throw new ApiException(ApiStatus.XXX_NOT_FOUND);                          // HTTP 상태는 ApiStatus 에 정의된 값
throw new ApiException(ApiStatus.SYSTEM_CUSTOM_EXCEPTION, "상세 메시지");  // 메시지만 바꿀 때
```

**메서드 네이밍**: `getXXX`(단건) / `getXXXList`(목록) / `insertXXX` / `updateXXX` / `deleteXXX`

## 5. ControllerDocs 인터페이스 생성

**위치**: `{패키지}/controller/{패키지명}ControllerDocs.java`

Swagger 어노테이션은 Controller에 직접 작성하지 않는다.
`{패키지명}ControllerDocs` 인터페이스에 정의하고 Controller가 implements한다.

```java
@Tag(name = "{패키지명}", description = "기능 설명")
public interface {패키지명}ControllerDocs {

  @Operation(summary = "단건 조회", description = "ID로 항목을 조회합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "조회 성공"),
      @ApiResponse(responseCode = "404", description = "항목 없음",
          content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
  })
  BaseResponse<{Dto}.XxxResponse> getXxx(long id);
}
```

**OpenApiConfig에 GroupedOpenApi 등록**:

```java
@Bean
public GroupedOpenApi {패키지명}Api() {
  return GroupedOpenApi.builder()
      .group("{패키지명}")
      .pathsToMatch("/{패키지URL}/**")
      .build();
}
```

## 6. Controller 생성

**위치**: `{패키지}/controller/{패키지명}Controller.java`

```java
@RestController
@RequestMapping("/{패키지 URL prefix}")
@RequiredArgsConstructor
public class {패키지명}Controller implements {패키지명}ControllerDocs {

  private final {패키지명}Service {패키지명}Service;

  @Override
  @GetMapping("/xxx/{id}")
  public BaseResponse<{Dto}.XxxResponse> getXxx(@PathVariable long id) {
    return BaseResponse.ok({패키지명}Service.getXxx(...));
  }

  @Override
  @PostMapping("/xxx")
  public BaseResponse<Void> insertXxx(@Valid @RequestBody {Dto}.InsertXxxRequest request) {
    {패키지명}Service.insertXxx(request);
    return BaseResponse.ok();
  }
}
```

- 응답: `BaseResponse.ok(data)` 또는 `BaseResponse.ok()`
- Validation: `@Valid` + DTO 필드에 `@NotBlank`, `@NotNull` 등
- 인터페이스 구현 메서드에 `@Override` 필수 (PMD 규칙)

## 7. 코드 포맷 & 정적 분석 검증

JetBrains MCP 가 있으면 Google Java Style 자동정렬 후 Checkstyle 검증 (없으면 정렬 단계를 건너뛰고 Checkstyle 결과로 직접 수정):

```bash
# JetBrains MCP로 재포맷 (파일별 실행)
mcp__jetbrains__reformat_file(projectPath, path)

# 정적 분석
./gradlew checkstyleMain pmdMain
```

- 코드 생성/수정 후 반드시 `reformat_file` 먼저 실행 — 수동 생성 코드와 IntelliJ 포맷이 다를 수 있음
- Google Checkstyle: 2칸 들여쓰기, Javadoc 필수 등
- PMD: 커스텀 규칙셋

## 체크리스트

| # | 항목 | 확인 |
|---|------|------|
| 1 | Entity → `domain/entity/`에 위치 |  |
| 2 | JPA Repository → `domain/repository/`에 위치 |  |
| 3 | QueryDSL Repository → `{패키지}/repository/`에 위치 |  |
| 4 | MyBatis Mapper → `{패키지}/mapper/` + `resources/mapper/{패키지}/` |  |
| 5 | DTO → `{패키지}/dto/` inner static class + `@Alias` |  |
| 6 | Service 조회 → `@Transactional(readOnly = true)` |  |
| 7 | Service 변경 → `@Transactional` |  |
| 8 | ControllerDocs 인터페이스 생성 (Swagger 어노테이션 분리) |  |
| 9 | Controller `implements ControllerDocs` + `@Override` |  |
| 10 | OpenApiConfig에 `GroupedOpenApi` 빈 등록 |  |
| 11 | Controller 응답 → `BaseResponse.ok()` |  |
| 12 | 예외 → `ApiException(ApiStatus.XXX)` |  |
| 13 | Javadoc 작성 (한글, 명사형 종결) |  |
| 14 | `reformat_file` 적용 (JetBrains MCP 가 있을 때) |  |
| 15 | Checkstyle + PMD 통과 |  |
