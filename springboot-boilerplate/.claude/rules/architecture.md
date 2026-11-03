---
paths:
  - "springboot-boilerplate/src/main/java/**/*.java"
  - "src/main/java/**/*.java"
---
# 아키텍처

계층 구조와 패키지 트리는 `springboot-boilerplate/CLAUDE.md` 의 "아키텍처" 를 본다. 이 파일은 그 밖의 원칙만 둔다.

## 새 기능 패키지 추가 시

`sample` 패키지를 참고한다. 전체 절차와 체크리스트는 `.claude/skills/be-crud/new-package-guide.md` (`/be-crud` 가 사용).

- Entity 는 `domain/entity/`, JPA Repository 는 `domain/repository/` 에 둔다
- 기능 패키지에는 `controller/`, `service/`, `dto/`, 필요할 때만 `repository/`(QueryDSL), `mapper/`(MyBatis) 를 둔다
- MyBatis XML 은 `resources/mapper/{패키지명}/` 에 둔다

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
