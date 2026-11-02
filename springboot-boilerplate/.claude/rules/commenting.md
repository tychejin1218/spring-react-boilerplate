---
paths:
  - "springboot-boilerplate/src/main/java/**/*.java"
  - "src/main/java/**/*.java"
---
# 주석 가이드

## Javadoc 기본 규칙

- **명사형** 종결: `~설정`, `~조회`, `~생성`으로 끝맺음 (마침표 없음)
- 모든 `public`/`protected` 메서드에 Javadoc 필수
- `private` 메서드는 로직이 복잡한 경우에만 작성
- **한글**로 작성

## Javadoc 구조

```java
/**
 * 첫 줄: 메서드 역할을 명사형으로 요약
 *
 * <p>부가 설명이 필요한 경우 빈 줄 + {@code <p>} 태그로 분리
 *
 * @param paramName 파라미터 설명
 * @param <T>       제네릭 타입 설명
 * @return 반환값 설명
 * @throws ExceptionType 예외 발생 조건
 */
```

### 필수 태그

| 태그 | 작성 기준 |
|------|----------|
| 첫 줄 요약 | **항상** 작성 |
| `@param` | **항상** 작성 (파라미터가 있는 경우) |
| `@return` | **항상** 작성 (`void` 제외) |
| `@throws` | checked 예외 또는 명시적으로 던지는 예외가 있는 경우 |

### HTML 태그 사용 규칙

코드 포매터(자동 정렬)가 줄바꿈을 합칠 수 있으므로 HTML 태그로 문단을 구분:

| 태그 | 사용 시점 |
|------|----------|
| `<p>` | 부가 설명 문단 시작 (빈 줄 + `<p>`로 첫 줄 요약과 분리) |
| `<ul><li>` | 항목 나열 (3개 이상일 때) |
| `{@code ...}` | 코드 참조 (`null`, 어노테이션 등) |
| `{@link ...}` | 클래스/메서드 참조 |

### 부가 설명 (선택)

아래 경우에 첫 줄 아래에 추가 설명 작성:

- 사이드이펙트가 있는 경우 (캐시 저장, 이벤트 발행 등)
- 특정 전략/패턴을 사용하는 경우
- 주의사항이 있는 경우

## 예시

### 일반 메서드

```java
/**
 * 회원 목록 조회
 *
 * @param request 조회 조건
 * @return 회원 목록
 */
@Transactional(readOnly = true)
public List<MemberResponse> getMembers(MemberRequest request) {
```

### 부가 설명이 필요한 메서드 (p 태그)

```java
/**
 * Redis 캐시 조회 또는 공급자를 통한 조회 후 캐시 저장
 *
 * <p>공급자 반환값이 {@code null}이면 캐시에 저장하지 않음
 *
 * @param key           Redis 키
 * @param typeReference 반환할 데이터 타입 정보
 * @param dataSupplier  캐시 미스 시 실행할 데이터 공급자
 * @param ttl           캐시 만료 기간
 * @param timeUnit      만료 기간 단위
 * @param <T>           데이터 타입
 * @return 캐시된 데이터 또는 공급자 조회 데이터
 */
public <T> T getCacheOrDefault(...) {
```

### 항목 나열이 필요한 메서드 (ul 태그)

```java
/**
 * 회원 상태 변경
 *
 * <p>상태 변경 시 아래 사이드이펙트 발생:
 * <ul>
 *   <li>변경 이력 저장
 *   <li>관련 캐시 무효화
 *   <li>상태 변경 이벤트 발행
 * </ul>
 *
 * @param memberId 회원 ID
 * @param status   변경할 상태
 * @throws ApiException 회원 미존재 시 (NOT_FOUND)
 */
public void updateMemberStatus(Long memberId, MemberStatus status) {
```

### Bean 설정 메서드

```java
/**
 * Read/Write 라우팅 데이터 소스 설정
 *
 * <p>{@code @Transactional(readOnly = true)} 여부에 따라 자동 라우팅
 *
 * @param writerDataSource Writer 데이터 소스
 * @param readDataSource   Reader 데이터 소스
 * @return 라우팅 {@link DataSource}
 */
@Bean
public DataSource dataSource(...) {
```

### 예외를 던지는 메서드

```java
/**
 * 회원 상세 정보 조회
 *
 * @param memberId 회원 ID
 * @return 회원 상세 정보
 * @throws ApiException 회원 미존재 시 (NOT_FOUND)
 */
public MemberResponse getMember(Long memberId) {
```

### 클래스 Javadoc

```java
/**
 * Redis 캐시 공통 컴포넌트
 *
 * <p>문자열, 객체, 정수 타입별 저장/조회와 캐시 패턴(Cache-Aside) 지원
 * 모든 Redis 연산은 내부적으로 예외를 처리하여 장애 전파 방지
 *
 * @see RedisTemplateConfig
 */
@Component
public class RedisComponent {
```

## 인라인 주석

- 복잡한 비즈니스 로직에 **왜(why)** 이렇게 했는지 작성
- **무엇(what)** 을 하는지는 코드로 충분히 표현되면 생략
- 한글로 작성

```java
// 탈퇴 후 30일 이내 회원은 재가입 불가
if (member.getDeletedAt().isAfter(LocalDateTime.now().minusDays(30))) {
    throw new ApiException(ApiStatus.MEMBER_REJOIN_RESTRICTED);
}
```

## TODO / FIXME 규칙

- `TODO`: 추후 구현 또는 개선 필요 시 → `// TODO: 페이징 처리 추가`
- `FIXME`: 알려진 문제, 임시 처리 → `// FIXME: 동시성 이슈 발생 가능, 분산 락 적용 필요`
- 담당자/날짜는 Git 이력으로 추적 가능하므로 생략

## 주석 작성하지 않는 경우

- getter/setter, 단순 위임 메서드
- 메서드명만으로 의도가 명확한 경우 (`delete`, `save` 등)
- Lombok 생성 메서드 (`@Builder`, `@Getter` 등)
- 테스트 메서드 (메서드명을 한글 또는 설명적으로 작성)
