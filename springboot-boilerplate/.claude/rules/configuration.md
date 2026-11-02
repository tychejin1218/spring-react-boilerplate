---
paths:
  - "springboot-boilerplate/build.gradle"
  - "build.gradle"
  - "springboot-boilerplate/src/main/resources/**/*.yml"
  - "src/main/resources/**/*.yml"
  - "springboot-boilerplate/src/main/java/**/config/**/*.java"
  - "src/main/java/**/config/**/*.java"
---
# 설정 & 인프라

## 프로파일 구조

```
src/main/resources/
├── application.yml                    # 메인 설정 (서버, 프로파일 그룹)
├── config/
│   ├── datasource/
│   │   ├── datasource-local.yml       # 로컬 DB/Redis 설정
│   │   ├── datasource-dev.yml
│   │   ├── datasource-stg.yml
│   │   └── datasource-prd.yml
│   └── environment/
│       ├── environment-local.yml      # 로컬 환경 설정 (JWT, Jasypt, Swagger, S3/MinIO 등)
│       ├── environment-dev.yml
│       ├── environment-stg.yml
│       └── environment-prd.yml
├── logback-spring.xml                 # 로깅 설정
└── mapper/                            # MyBatis XML 매퍼
    └── sample/SampleMapper.xml
```

### 프로파일 그룹

| 프로파일 | 설정 파일 | 로깅 |
|----------|----------|------|
| local | datasource-local.yml + environment-local.yml | console, local level |
| dev | datasource-dev.yml + environment-dev.yml | file, dev level |
| stg | datasource-stg.yml + environment-stg.yml | file, stg level |
| prd | datasource-prd.yml + environment-prd.yml | file, prd level |

기본 활성 프로파일: `local`

## DataSource 설정

Read/Write 분리 구조 (`MainDataSourceConfig.java`):

- `mainWriterDataSource`: 쓰기용 (prefix: `main.datasource.writer-jdbc-url`)
- `mainReaderDataSource`: 읽기 전용 (prefix: `main.datasource.reader-jdbc-url`)
- `mainDataSource`: `LazyConnectionDataSourceProxy`로 자동 라우팅

**DB**: MySQL 8.0+ (AWS RDS Aurora 는 선택. 로컬은 Docker MySQL)
**드라이버**: `software.aws.rds.jdbc.mysql.Driver` (aws-mysql-jdbc 1.1.15)
**커넥션 풀**: HikariCP (max: 5, min idle: 2)

## Redis 설정

프로파일별 설정 분리 (`config/redis/`):

- `RedisStandaloneConfig.java` - local 프로파일 (Standalone 모드)
- `RedisClusterConfig.java` - dev/stg/prd 프로파일 (Redis 클러스터. AWS ElastiCache 는 선택)
- `RedisTemplateConfig.java` - `StringRedisTemplate` 빈 (객체 캐시는 `CacheConfig` 의 `@Cacheable` 사용)

```yaml
# local (Standalone)
redis:
  stand-alone:
    host: hostname
    port: 16379
    timeout: 10000

# dev/stg/prd (Cluster)
redis:
  cluster:
    nodes:
      - hostname:6379
```

- Lettuce 클라이언트 (클러스터: topology refresh 60초, REPLICA_PREFERRED 읽기)
- `RedisComponent`로 문자열 get/set/delete, TTL 카운터(`increment`) 처리
- `RedisKeys` enum으로 키 이름 + TTL 관리

## 설정 암호화 (Jasypt)

- `JasyptConfig` 에서 `jasyptStringEncryptor` 빈 등록 (PBEWITHHMACSHA512ANDAES_256, RandomSalt/RandomIv, base64)
- 프로퍼티에서 `ENC(암호화된값)` 형태로 사용
- 비밀번호: `jasypt.encryptor.password` (운영은 `JASYPT_ENCRYPTOR_PASSWORD` 환경변수로 주입)
- 암호문 생성: `./gradlew jasyptEncrypt --args="<비밀번호> <평문>"` (build.gradle 의 JavaExec 태스크, `JasyptConfig` 와 동일 설정)

## AWS S3 인증

- `S3Config` 에서 `aws.s3.endpoint` 가 있으면(로컬 MinIO) `access-key`/`secret-key` 정적 자격증명 사용
- endpoint 가 없으면 `DefaultCredentialsProvider` 체인 사용 → ECS/EKS/EC2 의 IAM Role 로 자동 인증

## Bastion 터널링

VPN 없이 로컬에서 AWS 리소스 접근 시 JVM 옵션:

```
-Dbastion=true
-Dssh-host=x.x.x.x
-Dssh-user=ec2-user
-Dkey-path=/home/.ssh/key/bastion.pem
```

## 정적 분석 도구

### Checkstyle

- 버전: 12.3.1 (`build.gradle`의 `checkstyle.toolVersion`과 일치시킬 것)
- 설정: `config/checkstyle/google-checkstyle.xml` (Google Java Style, 2-space 들여쓰기)
- 억제: `config/checkstyle/checkstyle-suppressions.xml`
- `ignoreFailures = false`, `maxWarnings = 0`

### PMD

- 버전: 7.21.0
- 설정: `config/pmd/custom-ruleset.xml`
- `ignoreFailures = false`

### JaCoCo

- 버전: 0.8.14
- `-Pcoverage` 옵션으로 활성화
- HTML/XML 리포트 생성
- 커버리지 측정 제외: config, entity, dto, Application 클래스

```bash
./gradlew checkstyleMain           # Checkstyle 실행
./gradlew pmdMain                  # PMD 실행
./gradlew checkstyleMain pmdMain   # 전체 정적 분석
./gradlew clean test -Pcoverage    # 테스트 + 커버리지 리포트
```

## 빌드 설정 참고

- 테스트는 `-Pcoverage` 옵션 시에만 실행
- 컴파일러 옵션: `-Xlint:unchecked`, `-Xlint:deprecation`
- QueryDSL Q클래스 생성 경로: `src/main/generated` (clean 시 삭제)
- Maven Repository: `mavenCentral()`

## Swagger (SpringDoc OpenAPI)

상세 내용은 [openapi.md](openapi.md) 참조.

- 의존성: `springdoc-openapi-starter-webmvc-ui:3.0.1`
- 활성화: local / dev / stg — 비활성화: prd
- 설정 위치: `config/environment/environment-{profile}.yml`
- Config 클래스: `config/OpenApiConfig.java`

## AWS SDK 의존성

- `software.amazon.awssdk:s3:2.33.7` - S3 파일 관리 (`sdk-core`, `auth`, `regions` 는 전이 의존으로 포함)
