---
paths:
  - "springboot-boilerplate/src/main/java/**/repository/**/*.java"
  - "src/main/java/**/repository/**/*.java"
  - "springboot-boilerplate/src/main/java/**/mapper/**/*.java"
  - "src/main/java/**/mapper/**/*.java"
  - "springboot-boilerplate/src/main/resources/mapper/**/*.xml"
  - "src/main/resources/mapper/**/*.xml"
---
# 데이터 접근 계층

## 3중 쿼리 메커니즘

이 프로젝트는 **JPA Query Method**, **QueryDSL**, **MyBatis** 3가지 쿼리 방식을 동시 지원.

### 1. JPA Query Method (Spring Data JPA)

**위치**: `domain/repository/` 패키지

```java
// domain/repository/MemberRepository.java
public interface MemberRepository extends JpaRepository<Member, Long> {
    List<Member> findAllByNameContainingAndEmailContaining(
        String name, String email, Sort sort);
}
```

- 간단한 CRUD 및 조건 조회에 적합
- `JpaRepository<Entity, ID>` 상속
- PK 반환이 필요한 저장 작업에 사용 (QueryDSL은 PK 미반환)

### 2. QueryDSL

**위치**: 각 패키지의 `repository/` 패키지

```java
// sample/repository/SampleRepository.java
@Repository
@RequiredArgsConstructor
public class SampleRepository {
    private final JPAQueryFactory jpaQueryFactory;

    public SampleDto.MemberResponse selectMember(SampleDto.MemberRequest request) {
        QMember member = QMember.member;
        return jpaQueryFactory
            .select(Projections.fields(SampleDto.MemberResponse.class,
                member.id, member.name, member.email))
            .from(member)
            .where(member.id.eq(request.getId()))
            .setHint(Constants.HIBERNATE_SQL_COMMENT, "SampleRepository.selectMember")
            .fetchOne();
    }
}
```

- 동적 쿼리, 복잡한 조건에 적합
- `Projections.fields()`로 DTO 직접 매핑
- `BooleanBuilder`로 동적 WHERE 조건 조합
- `setHint(Constants.HIBERNATE_SQL_COMMENT, "클래스.메서드")`로 SQL 코멘트 추적
- **주의**: QueryDSL insert는 생성된 PK를 반환하지 않음

### 3. MyBatis

**위치**: 인터페이스 - `mapper/`, XML - `resources/mapper/`

```java
// mapper/SampleMapper.java
@Mapper
public interface SampleMapper {
    List<SampleDto.MemberResponse> selectMembers(SampleDto.MemberRequest memberRequest);
}
```

```xml
<!-- resources/mapper/sample/SampleMapper.xml -->
<select id="selectMembers" parameterType="MemberRequest" resultType="MemberResponse">
    SELECT id, name, email
    FROM member
    WHERE name LIKE CONCAT('%', #{name}, '%')
    AND email LIKE CONCAT('%', #{email}, '%')
    ORDER BY id DESC
</select>
```

- 복잡한 네이티브 SQL 제어에 적합
- DTO에 `@Alias` 어노테이션으로 타입 별칭 등록
- `base-package` 자동 스캔으로 alias 등록됨

## Read/Write DataSource 분리

`LazyConnectionDataSourceProxy`를 사용하여 트랜잭션 종류에 따라 자동 라우팅:

```java
// 읽기 전용 → Read DataSource로 라우팅
@Transactional(readOnly = true)
public List<MemberResponse> getMembers(...) { ... }

// 쓰기 → Write DataSource로 라우팅
@Transactional
public MemberResponse insertMember(...) { ... }
```

**설정 구조** (`datasource-{profile}.yml`):
```yaml
main:
  datasource:
    driver-class-name: software.aws.rds.jdbc.mysql.Driver
    writer-jdbc-url: jdbc:mysql:aws://write-endpoint:3306/db_name
    reader-jdbc-url: jdbc:mysql:aws://read-endpoint:3306/db_name
    username: db_user
    password: db_password
    hikari:
      maximum-pool-size: 5
      minimum-idle: 2
      idle-timeout: 10000
      max-life-time: 300000
      connection-test-query: SELECT 1
```

## Entity 작성 규칙

```java
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString
@Table(name = "table_name")
@Entity
public class EntityName {

    @Column(name = "column_name", nullable = false)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 연관 관계
    @Builder.Default
    @ToString.Exclude
    @OneToMany(mappedBy = "member", fetch = FetchType.LAZY)
    private List<Todo> todos = new ArrayList<>();
}
```

- `@NoArgsConstructor(access = PROTECTED)`: JPA 프록시용
- `@AllArgsConstructor(access = PRIVATE)`: Builder 전용
- `@ToString.Exclude`: 양방향 관계 순환 참조 방지
- `@Builder.Default`: 컬렉션 필드 기본값

## JPA 설정

- `ddl-auto: none` (자동 스키마 생성 비활성화)
- `open-in-view: false` (OSIV 비활성화)
- `use_sql_comments: true` (SQL 코멘트 활성화)
- `highlight_sql: true` (SQL 하이라이트, 개발용)
- `database-platform: org.hibernate.dialect.MySQLDialect` (Hibernate 7.x, 버전 자동 감지)

## 트랜잭션 규칙

- 조회: `@Transactional(readOnly = true)` 필수 (Read DB 라우팅)
- 변경: `@Transactional` 필수 (Write DB 라우팅)
- readOnly 트랜잭션에서 쓰기 시도 시 예외 발생
