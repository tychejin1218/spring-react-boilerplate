---
name: be-cleanup
description: springboot-boilerplate Java 코드에 대해 사용자가 "cleanup", "코드 정리", "import 정리", "임시 코드 제거", "System.out 제거", "주석 코드 삭제"를 요청할 때 이 스킬을 사용합니다. 코드 리뷰나 PR 전에 정리가 필요하다고 판단되면 명시적 요청 없이도 먼저 제안하세요.
argument-hint: "[파일명]"
disable-model-invocation: true
---

# BE 코드 정리

불필요한 import와 임시 코드를 제거해 코드베이스를 깔끔하게 유지합니다.
로직은 건드리지 않고 형식만 정리하는 것이 원칙입니다.

## 사전 준비

대상 파일을 읽고 현재 상태를 파악한 뒤 진행합니다.

## 정리 항목

### 1. Import 정리

사용하지 않는 import와 중복 import를 제거합니다.
와일드카드 import(`*`)는 어떤 클래스가 쓰이는지 명확하지 않아 유지보수에 방해가 되므로
개별 import로 교체합니다.

### 2. 임시 코드 제거

- `System.out.println` 제거 — 운영 환경에서 로그가 stdout으로 흘러 로그 시스템을 우회합니다.
  로깅이 필요하면 `@Slf4j` + `log.debug/info/error`로 교체합니다.
- 주석 처리된 코드 블록 제거 — git이 히스토리를 관리하므로 주석으로 코드를 보존할 필요가 없습니다.
- `// TODO`, `// FIXME` — 즉시 처리 가능한 것은 처리하고, 아닌 것은 그대로 둡니다.

### 3. 후처리

정리가 끝나면 `/be-check $ARGUMENTS`를 실행해 포맷 정렬과 정적 분석을 검증합니다.

## 주의

- 비즈니스 규칙을 설명하는 의미 있는 주석은 유지합니다.
- 확신이 없는 코드는 임의로 삭제하지 않습니다.

---

대상: $ARGUMENTS
