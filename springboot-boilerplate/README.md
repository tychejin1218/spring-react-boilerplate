# Spring Boot 보일러플레이트

백엔드 API 서비스 개발을 위한 Spring Boot 보일러플레이트 프로젝트입니다.
JWT 인증, Redis 캐시, JPA/QueryDSL/MyBatis 다중 데이터 접근, Read/Write DataSource 분리 등
실무 수준의 설정이 사전 구성되어 있습니다.

---

## 기술 스택

| 분류 | 기술 |
|------|------|
| 언어 / 프레임워크 | Java 25, Spring Boot 4.1.2 |
| 빌드 | Gradle 9.3.0 |
| 데이터베이스 | MySQL 8.0+ (Read/Write 분리, AWS RDS Aurora 는 선택) |
| ORM / 쿼리 | JPA + QueryDSL 7.7 (OpenFeign fork) + MyBatis 4.1.0 |
| 캐시 | Redis 7.0 (AWS ElastiCache 클러스터는 선택) |
| 인증 | JWT (JJWT 0.12.6) + Spring Security |
| 설정 암호화 | Jasypt 3.0.5 (PBEWITHHMACSHA512ANDAES_256) |
| 파일 스토리지 | AWS S3 (로컬: MinIO, 운영: IAM Role 기반 DefaultCredentialsProvider) |
| API 문서 | SpringDoc OpenAPI 3.2.0 (Swagger UI, local/dev/stg 만 활성) |
| 코드 품질 | Checkstyle 12.3.1 (Google Style) + PMD 7.21.0 + JaCoCo 0.8.14 |

---

## 프로젝트 구조

```
src/main/java/com/example/api/
├── ApiApplication.java
├── common/                  # 공통 모듈
│   ├── advice/              # ExceptionAdvice (전역 예외 핸들러)
│   ├── component/           # RedisComponent, S3Component
│   ├── constants/           # Constants (BASE_PACKAGE 등)
│   ├── controller/          # HealthController (/health)
│   ├── exception/           # ApiException
│   ├── response/            # BaseResponse<T>, ErrorResponse
│   └── type/                # ApiStatus enum, RedisKeys enum
├── config/                  # 설정 클래스
│   ├── jwt/                 # JwtTokenProvider
│   ├── redis/               # RedisStandaloneConfig, RedisClusterConfig, RedisTemplateConfig
│   ├── security/            # SecurityConfig, JwtAuthenticationFilter, LoggingFilter
│   ├── aws/                 # S3Config
│   ├── web/                 # TraceIdFilter (요청 추적 ID), RateLimitInterceptor
│   ├── AsyncConfig.java     # @Async 활성화, MDC(traceId) 전파
│   ├── JasyptConfig.java
│   ├── MainDataSourceConfig.java
│   └── OpenApiConfig.java
├── domain/                  # 도메인 공통
│   ├── entity/              # Member, Todo (JPA Entity)
│   └── repository/          # MemberRepository, TodoRepository (JpaRepository)
├── auth/                    # 인증 모듈
│   ├── controller/          # AuthController, TempAuthController (local/dev/stg)
│   ├── dto/                 # AuthDto
│   └── service/             # AuthService, TempAuthService
├── todo/                    # 할 일 모듈
│   ├── controller/          # TodoController
│   ├── dto/                 # TodoDto
│   └── service/             # TodoService
├── user/                    # 회원 모듈
│   ├── controller/          # UserController
│   ├── dto/                 # UserDto
│   └── service/             # UserService
└── sample/                  # 샘플 모듈 (JPA/QueryDSL/MyBatis 사용 예시)
    ├── controller/          # SampleController
    ├── dto/                 # SampleDto
    ├── mapper/              # SampleMapper (MyBatis)
    ├── repository/          # SampleRepository (QueryDSL)
    └── service/             # SampleService
```

**아키텍처 흐름**

```
Controller → Service → Repository (QueryDSL) / Mapper (MyBatis) → Database
                     → domain/repository (JPA)
```

**Read/Write DataSource 분리**

- `@Transactional(readOnly = true)` → Reader DataSource
- `@Transactional` → Writer DataSource

---

## 로컬 개발 환경 설정

### 1. 사전 요구사항

- Java 25
- Docker / Docker Compose

### 2. Docker 실행 (MySQL + Redis)

```bash
docker-compose up -d
```

| 컨테이너 | 포트 | 접속 정보 |
|---------|------|---------|
| MySQL 8.0 | 3306 | DB: `boilerplate_db`, User: `boilerplate`, PW: `boilerplate` |
| Redis 7.0 | 6379 | 인증 없음 |

### 3. 애플리케이션 실행

```bash
./gradlew bootRun
```

- 기본 프로파일: `local`
- 서버 포트: `9091`
- Context-path: `/api`
- 전체 base URL: `http://localhost:9091/api`

---

## 환경 변수 / 프로파일

애플리케이션은 4개의 프로파일을 지원합니다.

| 프로파일 | 설명 | 로깅 |
|---------|------|------|
| `local` | 로컬 개발 | 콘솔 출력 |
| `dev` | 개발 서버 | 파일 출력 |
| `stg` | 스테이징 서버 | 파일 출력 |
| `prd` | 운영 서버 | 파일 출력 |

각 프로파일별 설정 파일 위치:

```
src/main/resources/
├── application.yml                         # 공통 설정 (포트, context-path 등)
└── config/
    ├── datasource/
    │   ├── datasource-local.yml            # 로컬 DB 설정 (Docker MySQL)
    │   ├── datasource-dev.yml
    │   ├── datasource-stg.yml
    │   └── datasource-prd.yml
    └── environment/
        ├── environment-local.yml           # JWT, Jasypt, Swagger, AWS 설정
        ├── environment-dev.yml
        ├── environment-stg.yml
        └── environment-prd.yml
```

**주요 환경 변수**

| 환경 변수 | 설명 | 기본값 (local) |
|---------|------|--------------|
| `JASYPT_ENCRYPTOR_PASSWORD` | Jasypt 암호화 비밀번호 | `local-default` |
| `jwt.secret` | JWT 서명 키 (256비트 이상) | `environment-local.yml` 참조 |
| `jwt.access-token-validity-ms` | Access Token 유효 기간. 미활동 만료보다 짧아야 함 | `900000` (15분) |
| `jwt.session-idle-timeout-ms` (`JWT_SESSION_IDLE_TIMEOUT_MS`) | 미활동 만료. Refresh Token 의 Redis TTL 이며 재발급마다 연장 | `1800000` (30분) |
| `jwt.session-absolute-timeout-ms` (`JWT_SESSION_ABSOLUTE_TIMEOUT_MS`) | 최초 로그인 기준 절대 만료. 계속 활동해도 이 시간이 지나면 재로그인 | `43200000` (12시간) |

---

## API 목록

### 인증 API (`/api/auth`)

| 메서드 | 경로 | 설명 | 인증 필요 |
|-------|------|------|---------|
| POST | `/api/auth/register` | 회원가입 (이메일/이름/비밀번호) | X |
| POST | `/api/auth/login` | 로그인 | X |
| POST | `/api/auth/logout` | 로그아웃 (Refresh Token 무효화) | X |
| POST | `/api/auth/token/refresh` | Access/Refresh Token 재발급 | X |
| POST | `/api/auth/token/temp` | 임시 토큰 발급 (local 전용) | X |

### 회원 API (`/api/users`)

| 메서드 | 경로 | 설명 | 인증 필요 |
|-------|------|------|---------|
| GET | `/api/users/me` | 내 정보 조회 | O |

### 할 일 API (`/api/todos`)

| 메서드 | 경로 | 설명 | 인증 필요 |
|-------|------|------|---------|
| GET | `/api/todos` | 내 할 일 목록 조회 | O |
| POST | `/api/todos` | 할 일 추가 | O |
| PATCH | `/api/todos/{id}/complete` | 할 일 완료 처리 | O |
| DELETE | `/api/todos/{id}` | 할 일 삭제 | O |

### 샘플 API (`/api/sample`)

| 메서드 | 경로 | 설명 |
|-------|------|------|
| GET | `/api/sample/member` | QueryDSL 회원 목록 조회 |
| GET | `/api/sample/member/{id}` | QueryDSL 회원 단건 조회 |
| GET | `/api/sample/members/repository` | JPA Repository 회원 목록 조회 |
| GET | `/api/sample/members/mapper` | MyBatis 회원 목록 조회 |

**인증 방식**: JWT Bearer Token (`Authorization: Bearer {accessToken}`)

---

## 예제 기능 제거 방법

todo, admin, notification, 파일 업로드는 참고용 예제입니다 (FE 와 같은 구성). 공통 파일에 걸린 등록 지점에는 `[예제: ...]` 주석이 붙어 있어,
`grep -rn "\[예제: todo\]" .` 처럼 찾아 아래 디렉토리와 함께 지우면 제거됩니다.
제거 후 `./gradlew clean build` 로 컴파일, 테스트, Checkstyle, PMD 가 모두 통과하는지 확인하세요. FE 쪽은 [FE README](../react-boilerplate/README.md#예제-기능-제거-방법) 를 봅니다.

경로는 `src/main/java/com/example/api/` (테스트는 `src/test/java/com/example/api/`) 기준입니다.

| 예제 기능 | 삭제할 디렉토리/파일 | 지울 등록 지점 (`[예제: ...]` 주석) |
|----------|-------------------|-------------------------------|
| todo (할 일 관리) | `todo/`, `domain/entity/Todo.java`, `domain/repository/TodoRepository.java`, 테스트 `todo/` | `ApiStatus`: `TODO_NOT_FOUND`, `OpenApiConfig`: `todoApi()` 빈, `Member`: `todos` 필드와 관련 import, `docker/init.sql`: todo 테이블 DROP/CREATE/INSERT |
| 파일 업로드 (todo 이미지) | `file/`, `common/component/S3Component.java`, `config/aws/`, 테스트 `file/`, `common/component/S3ComponentMockTest.java` | `ApiStatus`: `FILE_CONTENT_TYPE_INVALID`, `build.gradle`: `awssdk:s3` 의존성, `environment-{profile}.yml`: `aws.s3` 설정, `docker-compose.yml`: `minio` 서비스, `minio_data` 볼륨, app 의 `AWS_S3_ENDPOINT` 와 `depends_on` 항목. todo 를 남기고 업로드만 뺄 때는 `TodoController`/`TodoService` 의 이미지 URL 수정 API 와 `todo.image_url` 컬럼도 함께 정리 |
| admin (관리자 API) | `admin/`, 테스트 `admin/` | 없음. `MemberRole`, `@PreAuthorize` 처리(`ExceptionAdvice` 의 403)는 공통 기능이므로 유지 |
| notification (SSE 알림) | `notification/`, `config/redis/RedisPubSubConfig.java` | `RedisKeys`: `NOTIFICATION_CHANNEL` (마지막 상수라 앞 줄 끝을 `;` 로 바꾼다), `TodoService`: `TodoCompleteEvent` 발행 줄과 import, `eventPublisher` 필드 |

todo 와 notification 은 서로 엮여 있습니다. todo 를 지우면 `notification/event/TodoCompleteEvent` 를 받는 곳도 함께 지우거나 다른 이벤트로 바꿔야 합니다.
QA 시나리오(`docs/qa/scenarios.md`)에서도 해당 기능 시나리오를 지웁니다: todo `QA-TODO-01`, 파일 업로드 `QA-TODO-02`, notification `QA-NOTI-01`, admin `QA-ADMIN-01`.

---

## Swagger UI

로컬 환경에서 Swagger UI를 통해 API를 테스트할 수 있습니다.

| 항목 | URL |
|------|-----|
| Swagger UI | http://localhost:9091/api/swagger-ui.html |
| API Docs (JSON) | http://localhost:9091/api/api-docs |

> Swagger 화면 우상단 **Authorize** 버튼에서 JWT 토큰을 입력하면 인증이 필요한 API를 테스트할 수 있습니다.
> 로컬 환경에서만 `/api/auth/token/temp` 엔드포인트로 이메일만으로 임시 토큰을 발급받을 수 있습니다.

---

## 코드 품질 검사

```bash
# Checkstyle 검사 (Google Style 기준)
./gradlew checkstyleMain

# PMD 정적 분석
./gradlew pmdMain

# 전체 정적 분석 (Checkstyle + PMD)
./gradlew checkstyleMain pmdMain
```

검사 결과 리포트 위치:
- Checkstyle: `build/checkstyle-output/checkstyle-report.xml`
- PMD: `build/pmd-output/pmd-report.xml`

---

## 테스트 실행

```bash
# 테스트 실행
./gradlew test

# 테스트 + JaCoCo 커버리지 리포트 생성 (라인 커버리지 50% 미만이면 실패, -PcoverageMinimum 으로 조정)
./gradlew clean test -Pcoverage
```

커버리지 리포트 위치:
- HTML: `build/jacoco/html/index.html`
- XML: `build/jacoco/report.xml`

---

## 빌드

```bash
# 전체 빌드 (테스트 포함)
./gradlew build

# 빌드 산출물
# build/libs/app.jar
```

---

## Jasypt 암호화

`environment-{profile}.yml`에 민감한 값을 `ENC(암호문)` 형태로 저장합니다.

### 새 ENC() 값 생성

```bash
./gradlew jasyptEncrypt --args="<비밀번호> <평문>"

# 예시
./gradlew jasyptEncrypt --args="local-default mySecretValue"
# 출력: XXXXXXXXXXXXXXXX... (암호문만 출력되므로 ENC(...) 로 감싸서 yml 에 적용)
```

> `JasyptConfig` 와 동일한 알고리즘(PBEWITHHMACSHA512ANDAES_256, RandomSalt/RandomIv, base64)으로 암호화합니다.
> 랜덤 Salt/IV 를 사용하므로 같은 평문이라도 실행할 때마다 다른 암호문이 나오며, 모두 정상 복호화됩니다.

### yml에 적용

```yaml
jwt:
  secret: ENC(XXXXXXXXXXXXXXXX...)
```

> 로컬 기본 비밀번호: `local-default`
> 운영 환경에서는 `JASYPT_ENCRYPTOR_PASSWORD` 환경변수로 주입합니다.

---

## QueryDSL Q클래스 생성

JPA Entity 추가·변경 시 Q클래스를 재생성해야 합니다.

```bash
./gradlew compileJava
# 생성 위치: src/main/generated/
```

> `src/main/generated/`는 `.gitignore`에 포함되어 있어 커밋되지 않습니다.

---

## Docker 볼륨 초기화

테이블 구조 변경 등으로 DB를 초기화해야 할 때:

```bash
# 컨테이너 + 볼륨 삭제
docker-compose down -v

# 재시작 (init.sql 자동 실행 → 테이블 재생성)
docker-compose up -d
```

> `docker/init.sql`이 MySQL 최초 기동 시 자동으로 실행됩니다.
