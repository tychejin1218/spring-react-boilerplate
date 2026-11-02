---
paths:
  - "springboot-boilerplate/src/test/java/**/*.java"
  - "src/test/java/**/*.java"
---
# 테스트 가이드

## 프레임워크

- **JUnit 5** (Jupiter)
- **Spring Boot Test** (`@SpringBootTest`)
- **MockMvc** (Controller 테스트)
- **Mockito** (`@Mock`, `@InjectMocks`, `@MockitoBean`)

## 테스트 실행

```bash
# 테스트 실행
./gradlew test

# 테스트 + 커버리지 리포트 실행 (BUNDLE 라인 커버리지가 coverageMinimum(기본 0.50) 미만이면 실패)
./gradlew clean test -Pcoverage

# 특정 테스트 클래스 실행
./gradlew test --tests "com.example.api.sample.service.SampleServiceTest"
```

> **참고**: 테스트는 `./gradlew test`와 `./gradlew build`에서 항상 실행된다. `-Pcoverage`는 JaCoCo 리포트 생성과 최소 커버리지 검증만 추가로 켠다. `@SpringBootTest` 테스트는 로컬 MySQL/Redis(`docker-compose up -d`)가 떠 있어야 통과한다.

## 테스트 구조

```
src/test/java/com/example/api/
├── ApiApplicationTest.java   # Spring Context 로드 테스트
├── sample/                   # 테스트 패턴 참조 구현 (아래 5종)
│   ├── controller/SampleControllerTest.java     # Controller MockMvc 테스트
│   ├── mapper/SampleMapperTest.java             # MyBatis Mapper 통합 테스트
│   ├── repository/SampleRepositoryTest.java     # QueryDSL Repository 통합 테스트
│   └── service/SampleServiceTest.java           # Service 통합 테스트 (실제 DB)
│       service/SampleServiceMockTest.java       # Service Mock 단위 테스트 (DB 불필요)
└── auth, common, config, file, todo, user, util/   # 기능별 테스트 (sample 과 동일 패턴)
```

## 테스트 네이밍 규칙

### 메서드명

`{대상메서드}_{시나리오}` 형태로 작성:

| 예시 | 설명 |
|------|------|
| `getMember_success` | 회원 조회 성공 |
| `getMember_notFound` | 존재하지 않는 회원 조회 |
| `selectMembers_all` | 전체 목록 조회 |
| `selectMembers_withNameFilter` | 이름 필터 조회 |

> Google Java Style에서 테스트 메서드명의 언더스코어(`_`) 사용을 허용함.

### @DisplayName

클래스와 메서드 **모두** 한국어로 테스트 목적을 명시:

```java
@DisplayName("샘플 서비스 테스트")           // 클래스: 테스트 대상 설명
class SampleServiceTest {

    @Test
    @DisplayName("회원 단건 조회 - 성공")     // 메서드: {기능} - {시나리오}
    void getMember_success() { ... }

    @Test
    @DisplayName("회원 단건 조회 - 존재하지 않는 회원")
    void getMember_notFound() { ... }
}
```

- 클래스: `"XXX 테스트"` 형태
- 메서드: `"기능 설명 - 시나리오"` 형태 (`-` 로 구분)
- Controller 메서드: `"HTTP메서드 /경로 - 시나리오"` 형태 (예: `"GET /sample/member/{id} - 성공"`)

## 테스트 공통 규칙

- `System.out.println` 사용 금지 → `log.debug`로 출력 (`@Slf4j` 필수)
- Spring 빈이 필요한 경우 직접 생성하지 않고 `@Autowired`로 주입받아 사용
- 전체 컨텍스트가 불필요한 경우 `@ContextConfiguration`으로 필요한 빈만 로드

```java
// 잘못된 예 — 빈을 직접 생성하거나 System.out 사용 금지
PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();
System.out.println(encryptor.encrypt("value"));

// 올바른 예 — 최소 컨텍스트로 빈 주입, log.debug 출력
@Slf4j
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = JasyptConfig.class)
@TestPropertySource(properties = "jasypt.encryptor.password=local-default")
class SomeUtilTest {

    @Autowired
    private StringEncryptor stringEncryptor;

    @Test
    void encrypt() {
        log.debug("ENC({})", stringEncryptor.encrypt("value"));
    }
}
```

## 테스트 패턴

### Service 통합 테스트

```java
@Slf4j
@SpringBootTest
@ActiveProfiles("local")
@Transactional
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("샘플 서비스 테스트")
class SampleServiceTest {

    @Autowired
    private SampleService sampleService;

    @Autowired
    private MemberRepository memberRepository;

    private Long savedMemberId;

    @BeforeEach
    void setup() {
        Member member = memberRepository.save(Member.builder()
            .name("admin")
            .email("admin@test.com")
            .build());
        memberRepository.flush();   // MyBatis가 JPA 1차 캐시를 우회하므로 flush 필수
        savedMemberId = member.getId();
    }

    @Test
    @Order(1)
    @DisplayName("회원 단건 조회 - 성공")
    void getMember_success() {
        // given
        SampleDto.MemberRequest request = SampleDto.MemberRequest.builder()
            .id(savedMemberId).build();

        // when
        SampleDto.MemberResponse response = sampleService.getMember(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(savedMemberId);
    }

    @Test
    @Order(2)
    @DisplayName("회원 단건 조회 - 존재하지 않는 회원")
    void getMember_notFound() {
        // given
        SampleDto.MemberRequest request = SampleDto.MemberRequest.builder()
            .id(999999L).build();

        // when & then
        assertThatThrownBy(() -> sampleService.getMember(request))
            .isInstanceOf(ApiException.class);
    }
}
```

- `@ActiveProfiles("local")`: local 프로파일로 실행
- `@Transactional`: 테스트 후 자동 롤백 (각 테스트는 독립적)
- `@BeforeEach`: 테스트 데이터 삽입 — DB 상태에 의존하지 않는 자기완결적 테스트
- `memberRepository.flush()`: JPA INSERT를 즉시 DB에 반영 — MyBatis는 JPA 1차 캐시를 우회하므로 필수
- `@TestMethodOrder` + `@Order`: 테스트 실행 순서 지정
- `@DisplayName`: 테스트 목적을 한국어로 명시 (클래스/메서드 모두 적용)

### Service Mock 단위 테스트

Spring Context 없이 Mockito만으로 Service 비즈니스 로직을 검증. DB 불필요하며 실행 속도가 빠름.

```java
@ExtendWith(MockitoExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("샘플 서비스 Mock 테스트")
class SampleServiceMockTest {

    @InjectMocks
    private SampleService sampleService;   // 실제 객체 (의존성은 Mock으로 주입)

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private SampleRepository sampleRepository;

    @Mock
    private SampleMapper sampleMapper;

    @Test
    @Order(1)
    @DisplayName("회원 단건 조회 - 성공")
    void getMember_success() {
        // given
        SampleDto.MemberRequest request = SampleDto.MemberRequest.builder().id(1L).build();
        SampleDto.MemberResponse mockResponse = SampleDto.MemberResponse.builder()
            .id(1L).name("admin").email("admin@test.com").build();
        given(sampleRepository.selectMember(request)).willReturn(mockResponse);

        // when
        SampleDto.MemberResponse response = sampleService.getMember(request);

        // then
        assertThat(response.getName()).isEqualTo("admin");
        verify(sampleRepository).selectMember(request);   // 메서드 호출 여부 검증
    }
}
```

- `@ExtendWith(MockitoExtension.class)`: Spring Context 없이 Mockito만 사용
- `@InjectMocks`: 테스트 대상 객체 생성, `@Mock` 필드를 자동 주입
- `@Mock`: 의존 객체를 Mock으로 대체
- `given(...).willReturn(...)`: Mock 동작 정의
- `verify(...)`: Mock 메서드 호출 여부 검증

| 구분 | 통합 테스트 (`SampleServiceTest`) | Mock 테스트 (`SampleServiceMockTest`) |
|------|----------------------------------|--------------------------------------|
| 애노테이션 | `@SpringBootTest` | `@ExtendWith(MockitoExtension.class)` |
| DB 필요 | O | X |
| 실행 속도 | 느림 | 빠름 |
| 검증 범위 | 실제 쿼리·트랜잭션 포함 | 비즈니스 로직 단독 검증 |

### Controller MockMvc 테스트

> **주의**: Spring Boot 4.x에서 `@AutoConfigureMockMvc` 패키지가 변경됨.
> `MockMvcBuilders.webAppContextSetup(wac)` 방식으로 MockMvc를 직접 설정할 것.

```java
@SpringBootTest
@ActiveProfiles("local")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("샘플 컨트롤러 테스트")
class SampleControllerTest {

    @Autowired
    private WebApplicationContext wac;

    @MockitoBean
    private SampleService sampleService;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    @Test
    @Order(1)
    @DisplayName("GET /sample/member/{id} - 성공")
    void getMember_success() throws Exception {
        // given
        SampleDto.MemberResponse response = SampleDto.MemberResponse.builder()
            .id(1L).name("admin").email("admin@test.com").build();
        given(sampleService.getMember(any())).willReturn(response);

        // when & then
        mockMvc.perform(get("/sample/member/{id}", 1L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.statusCode").value("200"))
            .andExpect(jsonPath("$.data.name").value("admin"));
    }
}
```

- `@MockitoBean`: Service 계층 모킹
- `MockMvcBuilders.webAppContextSetup(wac)`: MockMvc 직접 설정
- `jsonPath`: JSON 응답 필드 검증

### 예외 테스트

`assertThatThrownBy`로 예외 발생 여부 및 타입 검증:

```java
@Test
@DisplayName("회원 단건 조회 - 존재하지 않는 회원")
void getMember_notFound() {
    // given
    SampleDto.MemberRequest request = SampleDto.MemberRequest.builder()
        .id(999999L).build();

    // when & then
    assertThatThrownBy(() -> sampleService.getMember(request))
        .isInstanceOf(ApiException.class);
}
```

- `assertThatThrownBy(...)`: 람다 실행 시 예외 발생 여부 검증
- `.isInstanceOf(ApiException.class)`: 예외 타입 검증
- Mock 테스트에서는 `verify()`로 Mock 호출 여부도 함께 검증

## JaCoCo 커버리지

`-Pcoverage` 옵션으로 테스트 실행 시 JaCoCo 리포트 자동 생성:

- **HTML 리포트**: `build/jacoco/html/index.html`
- **XML 리포트**: `build/jacoco/report.xml`

### 커버리지 측정 제외 대상

- `com/example/api/config/**` - 설정 클래스
- `com/example/api/domain/entity/**` - JPA Entity
- `com/example/api/**/dto/**` - DTO 클래스
- `com/example/api/ApiApplication.class` - 메인 클래스

## 테스트 범위

| 테스트 대상 | 방식 | 주요 검증 |
|------------|------|----------|
| Application | `@SpringBootTest` | Spring Context 정상 로드 확인 |
| Service (통합) | `@SpringBootTest` + `@Transactional` | QueryDSL, JPA, MyBatis CRUD, 트랜잭션, 롤백 |
| Service (Mock) | `@ExtendWith(MockitoExtension.class)` | 비즈니스 로직, 예외 처리, Mock 호출 검증 |
| Controller | `@SpringBootTest` + MockMvc + `@MockitoBean` | HTTP 상태, JSON 응답 구조 |
| Repository | `@SpringBootTest` + `@Transactional` | QueryDSL 쿼리 정합성 |
| Mapper | `@SpringBootTest` + `@Transactional` | MyBatis SQL 매핑 정합성 |
