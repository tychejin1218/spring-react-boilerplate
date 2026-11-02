---
paths:
  - "springboot-boilerplate/src/main/java/**/*.java"
  - "src/main/java/**/*.java"
---
# 네이밍 컨벤션

## 파일명 Suffix

| Suffix | 용도 | 예시 |
|--------|------|------|
| `XXXController` | REST 요청/응답 처리 | `SampleController` |
| `XXXService` | 비즈니스 로직 | `SampleService` |
| `XXXDto` | 데이터 전송 객체 (inner static class 포함) | `SampleDto` |
| `XXXRepository` | JPA Repository 또는 QueryDSL 커스텀 Repository | `MemberRepository`, `SampleRepository` |
| `XXXMapper` | MyBatis 매퍼 인터페이스 | `SampleMapper` |

## Controller / Service 메서드 Prefix

| Prefix | 용도 | 예시 |
|--------|------|------|
| `getXXX` | 단건 조회 | `getMember()` |
| `getXXXList` | 목록 조회 | `getMemberList()` |
| `setXXX` | 객체 설정 | `setMemberStatus()` |
| `buildXXX` | 빌더 패턴으로 객체 생성 | `buildMemberResponse()` |
| `insertXXX` | 저장 | `insertMember()` |
| `updateXXX` | 수정 | `updateMember()` |
| `deleteXXX` | 삭제 | `deleteMember()` |

> **목록 조회 시 `getXXXs` 형태 사용 금지** — 반드시 `getXXXList` 사용

## Mapper / Repository 메서드 Prefix

| Prefix | 용도 | 예시 |
|--------|------|------|
| `selectXXX` | 단일행 조회 | `selectMember()` |
| `selectXXXList` | 다중행 조회 | `selectMemberList()` |
| `selectXXXCount` | 개수 조회 | `selectMemberCount()` |
| `insertXXX` | 단일행 저장 | `insertMember()` |
| `insertXXXs` | 다중행 저장 | `insertMembers()` |
| `updateXXX` | 단일행 수정 | `updateMember()` |
| `updateXXXs` | 다중행 수정 | `updateMembers()` |

> **목록 조회 시 `selectXXXs` 형태 사용 금지** — 반드시 `selectXXXList` 사용

## DTO 네이밍

DTO는 패키지 단위 클래스 내 **static inner class**로 정의:

```java
public class SampleDto {
    public static class MemberRequest { ... }        // 단건 조회 요청 (id 필드만)
    public static class MemberListRequest { ... }    // 목록 조회 요청 (검색/필터 조건)
    public static class MemberResponse { ... }       // 조회 응답
    public static class InsertMemberRequest { ... }  // 생성 요청
    public static class UpdateMemberRequest { ... }  // 수정 요청
    public static class DeleteMemberRequest { ... }  // 삭제 요청
    public static class DeleteMemberResponse { ... } // 삭제 응답
}
```

**단건 / 목록 Request 분리 원칙**:
- `XxxRequest` — id 필드만 포함 (단건 조회 전용)
- `XxxListRequest` — 검색·필터 조건 포함 (목록 조회 전용)

- MyBatis에서 사용 시 `@Alias` 어노테이션으로 타입 별칭 지정
- 요청 DTO에는 `@Valid` 관련 어노테이션 (`@NotBlank`, `@NotNull`, `@Min` 등) 사용

## DTO Lombok 사용 규칙

DTO의 getter/setter/constructor는 직접 작성하지 않고 Lombok 어노테이션을 사용한다.

`@Setter`는 **지양**하며, Jackson이 역직렬화해야 하는 경우에만 사용한다.
- **사용**: `@RequestBody` Request DTO, 외부 JSON 파싱 DTO (Redis 등)
- **미사용**: Response DTO (서비스에서 `from()` / builder로 직접 생성)

| 클래스 유형 | 어노테이션 |
|-----------|-----------|
| Request DTO (`@RequestBody`) | `@Getter` `@Setter` `@Builder` `@NoArgsConstructor` `@AllArgsConstructor` |
| 외부 JSON 파싱 DTO (Redis 등) | `@Getter` `@Setter` `@NoArgsConstructor` |
| Response DTO (코드에서 직접 생성) | `@Getter` `@NoArgsConstructor` |
| 불변 Request (정적 팩토리 전용) | `@Getter` (생성자는 `private`, setter 없음) |

```java
// Request DTO - Jackson @RequestBody 역직렬화
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public static class InsertRequest {
  private String name;
}

// 외부 JSON 파싱 DTO - Redis 등 외부 데이터 역직렬화
@Getter
@Setter
@NoArgsConstructor
public static class NodeResponse {
  private Long id;
  private String name;
}

// Response DTO - 서비스에서 from() / builder로 직접 생성
@Getter
@NoArgsConstructor
public static class MemberResponse {
  private Long id;
  private String name;

  public static MemberResponse from(Member member) {
    return new MemberResponse(member.getId(), member.getName());
  }
}

// 불변 Request (정적 팩토리 패턴)
@Getter
public static final class SearchRequest {
  private final Long id;
  private final String keyword;

  private SearchRequest(Long id, String keyword) { ... }

  public static SearchRequest of(Long id, String keyword) {
    return new SearchRequest(id, keyword);
  }
}
```

## 정적 팩토리 메서드 네이밍

DTO의 정적 팩토리 메서드는 Effective Java 관례를 따른다.

| 메서드명 | 용도 | 예시 |
|---------|------|------|
| `from(XxxEntity)` | 다른 타입(Entity 등)을 받아 변환 | `MemberResponse.from(member)` |
| `of(field1, field2, ...)` | 여러 파라미터로 직접 인스턴스 생성 | `MemberListRequest.of(name, email)` |

```java
// from - 타입 변환 (Entity → DTO)
public static MemberResponse from(Member member) {
  return MemberResponse.builder()
      .id(member.getId())
      .name(member.getName())
      .email(member.getEmail())
      .build();
}

// of - 값으로 직접 생성 (Request DTO)
public static MemberListRequest of(String name, String email) {
  return MemberListRequest.builder().name(name).email(email).build();
}

// of - 오버로딩으로 시그니처 분리 (Request DTO)
public static MemberRequest of(long id) {
  return MemberRequest.builder().id(id).build();
}

public static MemberRequest of(String name, String email) {
  return MemberRequest.builder().name(name).email(email).build();
}
```

### 빌더 직접 사용 금지 (Controller / Service)

> **Controller·Service 내부에서 DTO를 `.builder()...build()` 패턴으로 직접 생성하지 않는다.**
> 반드시 DTO 내부에 정적 팩토리 메서드를 정의하고 호출한다.

```java
// [금지] 컨트롤러에서 빌더 직접 사용
SampleDto.MemberListRequest request = SampleDto.MemberListRequest.builder()
    .name(name)
    .email(email)
    .build();

// [권장] 팩토리 메서드 호출 (인라인)
return BaseResponse.ok(sampleService.getMemberList(SampleDto.MemberListRequest.of(name, email)));
```

**적용 범위:**
- Request DTO 생성: `of(params...)` 사용
- Entity → Response DTO 변환: `from(entity)` 사용
- 테스트 코드는 가독성을 위해 `.builder()` 사용 허용

## API URL 설계 규칙

### 기본 원칙

- 리소스는 **명사** 사용 (동사 금지)
- 단수: 개별 리소스 / 복수: 컬렉션 또는 저장소
- 컨트롤러(동작)에만 예외적으로 동사 사용

### URL 규칙

| 규칙 | 올바른 예 | 잘못된 예 |
|------|-----------|-----------|
| **하이픈(`-`) 사용** | `/inventory-management/managed-entities` | `/inventory_management/managed_entities` |
| **소문자만 사용** | `/my-folder/my-doc` | `/My-Folder/my-doc` |
| **마지막 슬래시 금지** | `/managed-devices` | `/managed-devices/` |
| **파일 확장자 금지** | `/managed-devices` | `/managed-devices.xml` |

### HTTP Method 사용 원칙

| Method | 동작 | URL 예시 |
|--------|------|---------|
| `GET` | 조회 | `GET /device-management/managed-devices` |
| `POST` | 생성 | `POST /device-management/managed-devices` |
| `PUT` | 전체 수정 | `PUT /device-management/managed-devices/{id}` |
| `PATCH` | 부분 수정 | `PATCH /device-management/managed-devices/{id}` |
| `DELETE` | 삭제 | `DELETE /device-management/managed-devices/{id}` |

### Query Parameters vs Request Body

- `GET` → Query Parameters (camelCase)
  ```
  GET /device-management/managed-devices?version=1.00.01&deviceId=D001
  ```
- `POST`, `PUT`, `PATCH`, `DELETE` → Request Body (JSON, camelCase)

---

## Entity 네이밍

- `@Table(name = "snake_case")` 테이블 매핑
- `@Column(name = "snake_case")` 컬럼 매핑
- PK: `@Id @GeneratedValue(strategy = GenerationType.IDENTITY)`

## 코드 스타일

- **Google Java Style Guide** 준수 (Checkstyle 적용, 2-space 들여쓰기)
- 의존성 주입: `@RequiredArgsConstructor` + `private final` 필드 (생성자 주입)
- 로깅: `@Slf4j` (Lombok)
- 접근 제어: Entity의 `@NoArgsConstructor(access = AccessLevel.PROTECTED)`, `@AllArgsConstructor(access = AccessLevel.PRIVATE)`
- Jackson: `tools.jackson` 패키지 사용 (Jackson 3.x)

## Javadoc 스타일

클래스/메서드 Javadoc은 [주석 가이드](commenting.md) 참조.
