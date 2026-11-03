# 테스트 유형별 상세 규칙

## Service Mock 단위 테스트

```java
@ExtendWith(MockitoExtension.class)
class XxxServiceMockTest { ... }
```

- `@InjectMocks`로 실제 Service 인스턴스를 생성하고, `@Mock`으로 Repository·Mapper를 가짜로 대체합니다.
  실제 DB 없이 비즈니스 로직만 빠르게 검증할 수 있어 피드백 루프가 짧습니다.
- `given(...).willReturn(...)` BDD 스타일을 사용합니다. `when/thenReturn`보다 의도가 명확합니다.
- `verify(...)`로 Mock 메서드 호출 여부를 검증합니다. 호출 자체가 핵심 동작일 때 유용합니다.
- 예외 Mock: `given(...).willThrow(new ApiException(ApiStatus.RESOURCE_NOT_FOUND))`

## 통합 테스트 (Service / Repository / Mapper)

```java
@SpringBootTest
@ActiveProfiles("local")
@Transactional
class XxxServiceTest { ... }
```

- `@BeforeEach`에서 JPA Repository로 테스트 데이터를 삽입한 뒤 반드시 `.flush()`를 호출합니다.
  MyBatis는 JPA 1차 캐시를 우회해 DB에서 직접 읽으므로, flush 없이는 INSERT가 DB에 반영되지 않아
  MyBatis 쿼리가 데이터를 찾지 못합니다.
- `@Transactional`로 각 테스트가 끝난 뒤 자동 롤백되어 테스트 간 독립성이 보장됩니다.

## Controller MockMvc 테스트

```java
@SpringBootTest
class XxxControllerTest {

  private MockMvc mockMvc;

  @Autowired
  private WebApplicationContext wac;

  @MockitoBean
  private XxxService xxxService;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
  }
}
```

**Spring Boot 4.x 주의:**
- `@AutoConfigureMockMvc`는 Spring Boot 4.x에서 MockMvc 관련 패키지가 변경되어 동작하지 않습니다.
  `MockMvcBuilders.webAppContextSetup(wac).build()`로 직접 설정합니다.
- `@MockBean` 대신 `@MockitoBean`을 사용합니다. Spring Boot 4.x에서 `@MockBean`이 deprecated됩니다.

**응답 검증 패턴:**
```java
mockMvc.perform(get("/api/xxx/{id}", 1L))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.statusCode").value("OK"))
    .andExpect(jsonPath("$.data.필드명").value(기댓값));
```

## 예외 테스트

```java
assertThatThrownBy(() -> service.getXxx(request))
    .isInstanceOf(ApiException.class);
```

예외 타입 검증만으로 충분한 경우가 많습니다.
상태 코드까지 검증하려면 `.extracting("status").isEqualTo(HttpStatus.NOT_FOUND)`를 추가합니다.
