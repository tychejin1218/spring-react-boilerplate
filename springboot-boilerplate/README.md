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
| 데이터베이스 | MySQL 8.0+ (AWS RDS Aurora, Read/Write 분리) |
| ORM / 쿼리 | JPA + QueryDSL 5.1.0 + MyBatis 4.1.0 |
| 캐시 | Redis 7.0 (AWS ElastiCache) |
| 인증 | JWT (JJWT 0.12.6) + Spring Security |
| 암호화 | Jasypt 3.0.5 + AWS KMS |
| 파일 스토리지 | AWS S3 |
| API 문서 | SpringDoc OpenAPI 3.0 (Swagger UI) |
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
- 서버 포트: `8080`
- Context-path: `/api`
- 전체 base URL: `http://localhost:8080/api`

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
| `jwt.access-token-validity-ms` | Access Token 유효 기간 | `7200000` (2시간) |
| `jwt.refresh-token-validity-ms` | Refresh Token 유효 기간 | `604800000` (7일) |

---

## API 목록

### 인증 API (`/api/auth`)

| 메서드 | 경로 | 설명 | 인증 필요 |
|-------|------|------|---------|
| POST | `/api/auth/register` | 회원가입 (이메일/이름/비밀번호) | X |
| POST | `/api/auth/login` | 로그인 | X |
| POST | `/api/auth/logout` | 로그아웃 (Refresh Token 무효화) | X |
| POST | `/api/auth/token/refresh` | Access/Refresh Token 재발급 | X |
| POST | `/api/auth/token/temp` | 임시 토큰 발급 (local/dev/stg 전용) | X |

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

## Swagger UI

로컬 환경에서 Swagger UI를 통해 API를 테스트할 수 있습니다.

| 항목 | URL |
|------|-----|
| Swagger UI | http://localhost:8080/api/swagger-ui.html |
| API Docs (JSON) | http://localhost:8080/api/api-docs |

> Swagger 화면 우상단 **Authorize** 버튼에서 JWT 토큰을 입력하면 인증이 필요한 API를 테스트할 수 있습니다.
> 로컬/dev/stg 환경에서는 `/api/auth/token/temp` 엔드포인트로 이메일만으로 임시 토큰을 발급받을 수 있습니다.

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
# 테스트 + JaCoCo 커버리지 리포트 생성
./gradlew clean test -Pcoverage
```

커버리지 리포트 위치:
- HTML: `build/jacoco/html/index.html`
- XML: `build/jacoco/report.xml`

---

## 빌드

```bash
# 전체 빌드 (테스트 미실행)
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
# 출력: ENC(XXXXXXXXXXXXXXXX...)
```

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
