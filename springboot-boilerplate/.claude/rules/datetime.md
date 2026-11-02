---
paths:
  - "springboot-boilerplate/src/main/java/**/entity/**/*.java"
  - "src/main/java/**/entity/**/*.java"
  - "springboot-boilerplate/src/main/java/**/dto/**/*.java"
  - "src/main/java/**/dto/**/*.java"
  - "springboot-boilerplate/src/main/java/**/service/**/*.java"
  - "src/main/java/**/service/**/*.java"
---
# 날짜/시간 처리 가이드

> **현재 상태**: 아래 `KstDateTimeSerializer`와 `@JsonSerialize` 응답 패턴은 아직 코드베이스에 구현되어 있지 않다(2026-11 기준).
> 날짜 응답 직렬화가 필요한 기능을 추가할 때 이 가이드대로 `common/util/KstDateTimeSerializer.java`를 먼저 만든다.
> 현재 코드가 따르는 것은 "DB·서버 UTC 저장"과 `BaseAudit`의 `LocalDateTime.now(ZoneOffset.UTC)` 패턴이다.

## 기본 원칙

- **DB·서버**: UTC 저장
- **프론트 요청**: `ZonedDateTime` (타임존 포함) 으로 수신
- **프론트 응답**: KST(`+09:00`)로 반환

---

## 요청 패턴 (프론트 → DB)

사용자가 선택한 시각 필드는 `ZonedDateTime`으로 받아 서비스에서 UTC `LocalDateTime`으로 변환 후 저장.

```java
// DTO 필드
@Schema(description = "날짜+시각 (KST, ISO 8601 타임존 포함)", example = "2026-03-20T14:00:00+09:00")
private ZonedDateTime targetDate;
```

```java
// Service 헬퍼
private LocalDateTime toUtc(ZonedDateTime zonedDateTime) {
  if (zonedDateTime == null) {
    return null;
  }
  return zonedDateTime.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
}

// 사용
.targetDate(toUtc(request.getTargetDate()))
```

> 시스템 생성 시각(`createdAt` 등)은 `LocalDateTime.now(ZoneOffset.UTC)` 그대로 유지.

---

## 응답 패턴 (DB → 프론트)

응답 DTO의 날짜 필드에 `@JsonSerialize(using = KstDateTimeSerializer.class)` 적용.
Jackson이 직렬화할 때 UTC `LocalDateTime` → KST `OffsetDateTime` 문자열로 자동 변환.

```java
@JsonSerialize(using = KstDateTimeSerializer.class)
@Schema(description = "생성일시 (KST)", example = "2026-03-20T10:00:00+09:00")
private LocalDateTime createdAt;
```

> 내부 필드 타입은 `LocalDateTime` 유지 필수 — QueryDSL `Projections.fields` 호환 때문.

---

## KstDateTimeSerializer

위치: `common/util/KstDateTimeSerializer.java`

```java
public class KstDateTimeSerializer extends ValueSerializer<LocalDateTime> {

  private static final ZoneOffset KST = ZoneOffset.of("+09:00");

  @Override
  public void serialize(LocalDateTime value, JsonGenerator gen, SerializationContext ctxt) {
    if (value == null) {
      gen.writeNull();
      return;
    }
    gen.writeString(
        value.atOffset(ZoneOffset.UTC).withOffsetSameInstant(KST).toString()
    );
  }
}
```

---

## 필드 타입 적용 기준

| 필드 종류 | DB 컬럼 타입 | 요청 DTO | 응답 DTO (내부) | 직렬화 |
|----------|------------|---------|---------------|--------|
| 사용자 선택 날짜+시각 | `DATETIME` | `ZonedDateTime` | `LocalDateTime` | `@JsonSerialize` |
| 시스템 생성 시각 (`createdAt`, `updatedAt`) | `DATETIME` | — | `LocalDateTime` | `@JsonSerialize` |
| 날짜만 (생년월일 등) | `DATE` | `LocalDate` | `LocalDate` | 불필요 |

> **규칙**: 날짜만 저장하는 필드라면 DB 컬럼 타입을 `DATE`로 설계.
> `DATETIME`으로 만들면 UTC 변환 대상이 되고, `ZonedDateTime` → UTC `LocalDateTime` 흐름을 따라야 한다.

---

## Jackson 3.x API 주의사항 (Spring Boot 4.x)

Jackson 2.x에서 변경된 클래스명:

| Jackson 2.x | Jackson 3.x |
|-------------|-------------|
| `JsonSerializer<T>` | `tools.jackson.databind.ValueSerializer<T>` |
| `SerializerProvider` | `tools.jackson.databind.SerializationContext` |
| `throws IOException` | 제거 (`JacksonException`은 unchecked) |

---

## MySQL DATETIME 주의사항

`DATETIME` 타입 사용 시 MySQL이 UTC 변환을 해주지 않으므로 앱이 직접 UTC 값을 삽입해야 함.

```yaml
# datasource-{profile}.yml
writer-jdbc-url: jdbc:mysql:aws://...?serverTimezone=UTC
hibernate:
  jdbc.time_zone: UTC
```
