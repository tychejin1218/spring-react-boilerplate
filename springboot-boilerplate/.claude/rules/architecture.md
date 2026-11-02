---
paths:
  - "springboot-boilerplate/src/main/java/**/*.java"
  - "src/main/java/**/*.java"
---
# 아키텍처 & 패키지 구조

## 계층 구조 (4-tier Layered Architecture)

```
Controller → Service → Repository/Mapper → Database
(Presentation)  (Business)  (Persistence)     (Data)
```

## 패키지 구조

```
com.example.api/
├── ApiApplication.java      # Spring Boot 진입점
├── common/                  # 공통: advice(전역 예외), component(Redis/S3), constants, controller(Health),
│                            #       exception(ApiException), response(BaseResponse/ErrorResponse/PageResponse), type(ApiStatus/RedisKeys)
├── config/                  # 설정: MainDataSourceConfig(R/W 분리 + JPA + MyBatis + QueryDSL), redis/, security/, jwt/, aws/, web/,
│                            #       AsyncConfig, CacheConfig, JasyptConfig, OpenApiConfig, WebMvcConfig
├── domain/                  # entity/(Member, Todo, BaseAudit, MemberRole) + repository/(JPA Repository)
├── sample/                  # 기능 패키지 템플릿 (controller/service/dto/repository/mapper)
└── admin, auth, file, notification, todo, user/   # 기능 패키지 (sample 과 동일 구조)
```

전체 파일 목록은 유지하지 않는다. 구조 원칙은 아래를, 실제 파일은 디렉토리를 직접 탐색한다.

## 새 기능 패키지 추가 시

`sample` 패키지를 참고하여 아래 구조로 생성:

```
com.example.api.{패키지명}/
├── controller/{패키지명}Controller.java
├── service/{패키지명}Service.java
├── dto/{패키지명}Dto.java
├── repository/{패키지명}Repository.java   # QueryDSL (필요시)
└── mapper/{패키지명}Mapper.java           # MyBatis (필요시)
```

- Entity는 `domain/entity/`에 추가
- JPA Repository는 `domain/repository/`에 추가
- MyBatis XML은 `resources/mapper/{패키지명}/`에 추가

## 주요 디자인 패턴

| 패턴 | 적용 위치 |
|------|----------|
| Read/Write 분리 | `LazyConnectionDataSourceProxy` - readOnly 트랜잭션 자동 라우팅 |
| DTO 패턴 | API 계약과 도메인 모델 분리 |
| Repository 패턴 | QueryDSL, MyBatis, JPA Query Method 3중 구현 |
| Builder 패턴 | Lombok `@Builder`로 Entity/DTO 생성 |
| 전역 예외 처리 | `@RestControllerAdvice`로 중앙 집중 예외 처리 |
| Enum 기반 상태 코드 | `ApiStatus`로 타입 안전 상태 관리 |
| 캐시 추상화 | `CacheConfig` + `@Cacheable`/`@CacheEvict` 로 Redis 캐시 적용, 토큰·카운터는 `RedisComponent` |

## 클래스 Javadoc

모든 주요 클래스에 Javadoc 작성 필수. 스타일은 [주석 가이드](commenting.md) 참조.
