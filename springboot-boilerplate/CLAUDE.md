## 프로젝트 개요

Spring Boot 백엔드 API 서비스

- **Java 25** / **Spring Boot 4.1.2** / **Gradle 9.3.0**
- **DB**: MySQL 8.0+ (AWS RDS Aurora) - Read/Write 분리 (`LazyConnectionDataSourceProxy`)
- **ORM/Query**: JPA + QueryDSL 5.1.0 + MyBatis 4.1.0
- **캐시**: Redis (AWS ElastiCache)
- **암호화**: Jasypt + AWS KMS
- **코드 품질**: Checkstyle 12.3.1 (Google Style) + PMD 7.21.0 + JaCoCo 0.8.14

## 빌드 & 실행

```bash
./gradlew build                    # 빌드 (테스트 포함)
./gradlew test                     # 테스트
./gradlew bootRun                  # 실행 (local, :9091, context-path: /api)
./gradlew checkstyleMain           # Checkstyle 검사
./gradlew pmdMain                  # PMD 검사
./gradlew checkstyleMain pmdMain   # 전체 정적 분석
./gradlew clean test -Pcoverage    # 테스트 + 커버리지 리포트 생성 (최소 50%, -PcoverageMinimum 조정)
```

## 프로파일

`local` | `dev` | `stg` | `prd` - 각 프로파일별 설정 분리:
- `config/datasource/datasource-{profile}.yml` - DataSource
- `config/environment/environment-{profile}.yml` - 환경 설정

## 아키텍처

### 4계층 구조

```
Controller → Service → Repository(QueryDSL) / Mapper(MyBatis) → Database
                     → domain/repository(JPA)
```

### 패키지 구조

```
src/main/java/com/example/api/
├── common/              # 공통 (예외, 응답, 상수, 유틸)
│   ├── advice/          # ExceptionAdvice (전역 예외 핸들러)
│   ├── exception/       # ApiException
│   ├── response/        # BaseResponse<T>, ErrorResponse
│   ├── type/            # ApiStatus enum (응답코드 정의)
│   └── constants/       # Constants (BASE_PACKAGE 등)
├── config/              # 설정 (DataSource, Redis, WebMvc 등)
├── domain/              # 도메인 공통
│   ├── entity/          # JPA Entity (모든 Entity 여기에)
│   └── repository/      # JPA Repository (JpaRepository 상속)
└── {패키지}/              # 기능 패키지 (sample 참조)
    ├── controller/      # REST Controller
    ├── service/         # 비즈니스 로직
    ├── dto/             # DTO (outer class 내 static inner class)
    ├── repository/      # QueryDSL Custom Repository
    └── mapper/          # MyBatis Mapper 인터페이스
```

### Read/Write DataSource 분리

`MainDataSourceConfig`에서 `LazyConnectionDataSourceProxy`로 라우팅:
- `@Transactional(readOnly = true)` → Reader DataSource
- `@Transactional` → Writer DataSource

### 쿼리 선택 기준

| 메커니즘 | 용도 | 위치 |
|---------|------|------|
| JPA Query Method | 단순 CRUD, 단건 조회 | `domain/repository/` |
| QueryDSL | 동적 쿼리, 복잡한 조건 | `{패키지}/repository/` |
| MyBatis | 네이티브 SQL, 프로시저 | `{패키지}/mapper/` + `resources/mapper/*.xml` |

## 코드 작성 규칙

### 필수 준수 사항

1. **Google Checkstyle** 준수 (2칸 들여쓰기) - `config/checkstyle/google-checkstyle.xml`
2. **PMD 규칙** 준수 - `config/pmd/custom-ruleset.xml`
3. 조회 메서드: `@Transactional(readOnly = true)` (Reader DS 라우팅)
4. 변경 메서드: `@Transactional`
5. 컨트롤러 응답: `BaseResponse.ok(data)` 또는 `BaseResponse.ok()`
6. 예외: `throw new ApiException(HttpStatus.NOT_FOUND, ApiStatus.NOT_FOUND)` 형태 (HTTP 상태코드 + 응답코드 명시)
7. Entity는 `domain/entity/`에, JPA Repository는 `domain/repository/`에 배치
8. **`System.out.println` 사용 금지** — 서비스·테스트 코드 모두 `@Slf4j` + `log.*` 사용
   - 일반 흐름: `log.info` / 디버그 출력: `log.debug` / 경고: `log.warn` / 오류: `log.error`

### DTO 패턴

패키지별 `XXXDto` 클래스 내 static inner class로 정의. MyBatis 사용 시 `@Alias` 필수.
단건 조회: `XxxRequest`(id만) / 목록 조회: `XxxListRequest`(필터 조건) 분리.

**정적 팩토리 메서드 필수** — Controller·Service에서 `.builder()...build()` 직접 사용 금지:
- `of(params...)` — Request DTO 생성 (컨트롤러 파라미터 → DTO)
- `from(entity)` — Response DTO 생성 (Entity → DTO 변환)
- 테스트 코드는 `.builder()` 사용 허용

상세: [naming-conventions.md](.claude/rules/naming-conventions.md)

### 네이밍 규칙

- Controller/Service: `getXXX`(단건), `getXXXList`(목록), `insertXXX`, `updateXXX`, `deleteXXX`
- Repository/Mapper: `selectXXX`(단건), `selectXXXList`(목록), `selectXXXCount`, `insertXXX`, `updateXXX`

상세: [naming-conventions.md](.claude/rules/naming-conventions.md)

## Claude Code 자동화 도구

스킬과 에이전트는 모노레포 루트 `.claude/`에 있으며 BE 전용은 `be-` 접두사가 붙는다.
Gradle 명령은 `cd springboot-boilerplate && ./gradlew ...` 형태로 실행한다.

### 스킬 (`/xxx`) — 명시적 호출

| 스킬 | 역할 |
|------|------|
| `/be-crud <Entity 파일>` | `be-crud-generator` 에이전트로 DTO·Repository·Service·Controller·Test 생성 |
| `/be-review <파일명>` | `be-code-reviewer` 에이전트로 버그·성능·보안·컨벤션 코드 리뷰 |
| `/be-perf <파일명>` | `be-performance-inspector` 에이전트로 N+1·readOnly 누락·페이징·쿼리 비효율 감지 |
| `/be-refactor <파일명>` | 리팩토링 (분석 → 계획 승인 → 실행) |
| `/be-api-doc <파일명>` | Swagger ControllerDocs 어노테이션 추가 |
| `/be-explain <파일명>` | 코드 흐름·로직 설명 |
| `/be-check [파일명]` | Google Style 포맷 정렬 + Checkstyle/PMD 정적 분석 |
| `/be-test <파일명>` | 테스트 코드 생성 (Mock·통합·MockMvc·Repository·Mapper 전 유형 지원) |
| `/be-cleanup <파일명>` | import 정리 + 임시 코드 제거 |
| `/be-commit [JIRA키]` | git diff 기반 커밋 메시지 생성 후 커밋 (사용자 확인 필수) |

### 에이전트 — 대화 맥락에 따라 자동 호출

| 에이전트 | 자동 호출 트리거 |
|---------|---------------|
| `be-debugger` | BE 에러 메시지·스택 트레이스 공유 시 |
| `be-mybatis-specialist` | 복잡한 MyBatis 동적 쿼리·Bulk·resultMap 작성 요청 시 |
| `be-schema-designer` | 새 테이블 설계·Entity 생성 요청 시 |

### Hooks

스크립트는 루트 `.claude/hooks/`에 있고, 루트에서 시작한 세션은 루트 `.claude/settings.json`, 이 디렉토리에서 시작한 세션은 `springboot-boilerplate/.claude/settings.json`에 등록되어 있다:
- `be-java-notice.sh`: `.java` 편집 후 포맷 정렬·정적 분석 필요 안내
- `be-commit-gate.sh`: BE 변경이 스테이징된 `git commit` 전 `checkstyleMain pmdMain` 실행, 실패 시 커밋 차단

## JetBrains MCP (선택)

IntelliJ의 JetBrains MCP를 쓰는 환경에서는 `reformat_file`로 Google Java Style 정렬을 할 수 있다.
도구 호출 시 **항상** `projectPath`에 `springboot-boilerplate/`의 **절대 경로**를 넘긴다
(여러 프로젝트가 동시에 열려 있으면 미지정 시 오류). 경로는 세션 시작 시 `pwd`로 확인한다.
JetBrains MCP가 없는 환경에서는 포맷 정렬 단계를 건너뛰고 `./gradlew checkstyleMain pmdMain` 결과로 들여쓰기·공백 오류를 직접 수정한다.

### 상세 문서

`.claude/rules/` 디렉토리 참조:
- [패키지 구성 가이드](.claude/rules/package.md) - 새 패키지 추가 시 필독
- [아키텍처](.claude/rules/architecture.md) | [네이밍](.claude/rules/naming-conventions.md)
- [API 응답 & 예외](.claude/rules/api-response.md) | [데이터 접근](.claude/rules/data-access.md)
- [설정 & 인프라](.claude/rules/configuration.md) | [OpenAPI](.claude/rules/openapi.md) | [테스트](.claude/rules/testing.md) | [주석](.claude/rules/commenting.md)
- [DB 네이밍 컨벤션](.claude/rules/db-conventions.md) - 테이블·컬럼·인덱스 네이밍 및 DDL 작성 규칙
- [날짜/시간 처리](.claude/rules/datetime.md) - UTC/KST 변환, ZonedDateTime 패턴
