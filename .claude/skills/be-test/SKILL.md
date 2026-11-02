---
name: be-test
description: springboot-boilerplate 의 지정된 클래스의 JUnit 5 테스트 코드를 생성합니다. Service(Mock/통합), Controller(MockMvc), Repository, Mapper 테스트 중 적합한 유형을 선택하여 작성합니다.
argument-hint: "<클래스명>"
disable-model-invocation: true
---

# 테스트 코드 생성

지정된 클래스에 대한 테스트 코드를 작성해주세요.

## 사전 준비

시작 전 반드시 아래를 읽으세요:
- `springboot-boilerplate/.claude/rules/testing.md` — 테스트 전체 가이드
- `springboot-boilerplate/src/test/java/{basePackage}/sample/` — 기존 테스트 패턴 참조
- 대상 클래스 소스 코드 전체 + 직접 의존하는 클래스 읽기

## 테스트 유형 선택

대상 클래스 성격에 따라 적합한 유형을 선택하세요:

| 대상 | 테스트 유형 | 주요 어노테이션 |
|------|-----------|--------------|
| Service (비즈니스 로직) | **Mock 단위 테스트** | `@ExtendWith(MockitoExtension.class)` |
| Service (실제 DB 검증) | **통합 테스트** | `@SpringBootTest @ActiveProfiles("local") @Transactional` |
| Controller | **MockMvc 테스트** | `@SpringBootTest` + `MockMvcBuilders` + `@MockitoBean` |
| Repository (QueryDSL) | **통합 테스트** | `@SpringBootTest @ActiveProfiles("local") @Transactional` |
| Mapper (MyBatis) | **통합 테스트** | `@SpringBootTest @ActiveProfiles("local") @Transactional` |

> Service는 Mock(빠름·비즈니스 로직 검증) + 통합(실제 쿼리·트랜잭션 검증) 둘 다 생성하세요.

## 필수 준수 규칙

### 공통
- Given / When / Then 주석 구분 필수
- 정상 케이스 + 예외·엣지 케이스 모두 포함
- 메서드명: `{대상메서드}_{시나리오}` (예: `getMember_success`, `getMember_notFound`)
- `@DisplayName`: 클래스·메서드 모두 한국어
  - Service/Repository: `"기능 - 시나리오"`
  - Controller: `"HTTP메서드 /경로 - 시나리오"` (예: `"GET /member/{id} - 성공"`)
- `@TestMethodOrder(MethodOrderer.OrderAnnotation.class)` + `@Order` 순서 지정

### Service Mock 단위 테스트
- `@InjectMocks`: 실제 Service 생성
- `@Mock`: 의존성 (Repository, Mapper) Mock 처리
- `given(...).willReturn(...)` BDD 스타일
- `verify(...)`: Mock 메서드 호출 여부 검증
- 예외 Mock: `given(...).willThrow(new ApiException(HttpStatus.NOT_FOUND, ApiStatus.NOT_FOUND))`

### 통합 테스트 (Service / Repository / Mapper)
- `@BeforeEach`: JPA Repository로 테스트 데이터 삽입 후 반드시 `.flush()` 호출
  - **이유**: MyBatis가 JPA 1차 캐시를 우회하므로 flush 없이는 INSERT가 DB에 미반영
- 각 테스트는 `@Transactional` 자동 롤백으로 독립 유지

### Controller MockMvc 테스트 (Spring Boot 4.x 주의)
- `@AutoConfigureMockMvc` 사용 금지 — Spring Boot 4.x에서 패키지 변경으로 미동작
- `MockMvcBuilders.webAppContextSetup(wac).build()` 방식으로 직접 설정
- `@MockitoBean`으로 Service 계층 모킹 (`@MockBean` 사용 금지)
- BaseResponse 구조 검증: `jsonPath("$.statusCode").value("200")`
- 데이터 검증: `jsonPath("$.data.필드명").value(기댓값)`

### 예외 테스트
```java
assertThatThrownBy(() -> service.getXxx(request))
    .isInstanceOf(ApiException.class);
```

## 파일 위치

```
src/test/java/{basePackage}/{패키지}/{계층}/{클래스명}Test.java
src/test/java/{basePackage}/{패키지}/service/{클래스명}MockTest.java  ← Service Mock 전용
```

## 후처리

파일 생성 후:
1. JetBrains MCP `reformat_file`로 생성된 각 파일 정렬 (`projectPath`는 `springboot-boilerplate/`의 절대 경로, BE CLAUDE.md의 JetBrains 규칙 참조)
2. `springboot-boilerplate/` 디렉토리에서 `./gradlew checkstyleMain pmdMain` 실행 (루트 세션이면 `cd springboot-boilerplate &&` 를 앞에 붙이고, 이미 그 디렉토리에서 시작한 세션이면 생략)
3. 오류 발생 시 분석하여 수정 후 재실행

---

대상 클래스: $ARGUMENTS
