---
name: be-refactor
description: springboot-boilerplate 전용. 분석 → 계획 승인 → 실행 순서로 코드를 리팩토링합니다.
argument-hint: "<파일명 또는 패키지 경로>"
allowed-tools: ["Read", "Glob", "Grep", "Write", "Edit", "Bash"]
---

# 리팩토링

지정된 코드를 리팩토링해주세요.

대상은 단일 파일, 여러 파일, 패키지 디렉토리 모두 허용합니다.
패키지 경로가 지정된 경우 하위 파일 전체를 탐색하여 분석하세요.

## 절차

1. **분석**: 대상 파일·패키지를 읽고 현재 코드의 문제점을 항목별로 심층 분석해 보여주세요
2. **계획**: 리팩토링 계획을 단계별로 제시하고 승인을 받으세요
3. **실행**: 승인 후 단계별로 리팩토링을 진행하세요
4. **검증**: 리팩토링 완료 후 `/be-check {대상파일}` 스킬을 실행하세요 (포맷 정렬 + 정적 분석)
5. **테스트**: 기존 테스트가 있으면 실행하여 통과 여부를 확인하세요. 없으면 `/be-test {대상클래스}` 스킬로 테스트를 먼저 작성한 후 리팩토링하세요

## 리팩토링 체크리스트

### 프로젝트 컨벤션 정합성

상세 규칙: [naming-conventions.md](springboot-boilerplate/.claude/rules/naming-conventions.md) | [api-response.md](springboot-boilerplate/.claude/rules/api-response.md) | [commenting.md](springboot-boilerplate/.claude/rules/commenting.md)

- 네이밍: Controller/Service `getXXX`/`getXXXList`, Repository/Mapper `selectXXX`/`selectXXXList`
- `@Transactional(readOnly = true)` / `@Transactional` 정확히 적용
- `BaseResponse.ok()` 응답 래핑 / `ApiException(ApiStatus.XXX)` (HTTP 상태는 ApiStatus 에 정의) 예외 패턴
- DTO: outer class 내 static inner class + `@Alias`
- Javadoc: 모든 public 메서드에 한글 명사형

### 코드 품질
- 중복 코드 추출 (공통 로직 private 메서드화)
- 매직 넘버/문자열 상수화
- 깊은 중첩 제거 (early return 패턴)
- 단일 책임 원칙 준수 (메서드가 너무 많은 일을 하지 않는지)
- 불필요한 null 체크 또는 Optional 남용

### 성능
- N+1 쿼리 제거
- 불필요한 전체 조회 후 필터링 제거
- Reader DataSource 활용 (`readOnly = true` 누락)

## 주의사항

- **기능 변경 없이 구조만 개선**할 것
- 한 번에 너무 많이 변경하지 말고 **단계별로 진행**할 것
- 각 단계마다 변경 이유를 명확히 설명할 것
- 변경 범위가 크면 반드시 사전 승인 후 진행할 것

---

대상: $ARGUMENTS
