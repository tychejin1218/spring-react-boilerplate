---
name: be-commit
description: springboot-boilerplate 전용. git diff 기반으로 커밋 메시지를 생성하고 커밋을 수행합니다.
allowed-tools: Bash, Read
disable-model-invocation: true
---

# BE 커밋 메시지 자동 생성

아래 순서로 커밋을 진행하세요.

## 1. 변경 사항 분석

`git diff`와 `git status`를 실행하여 변경된 파일과 내용을 파악하세요.

변경 파일을 성격별로 그룹화하여 **커밋을 분리할 수 있는지 판단**하세요.

- 독립적인 작업 단위(기능 변경, 설정 변경, 도구 설정 등)가 섞여 있으면 **분리 커밋을 제안**하세요.
- 분리가 필요한 경우, 각 그룹별 파일 목록과 커밋 메시지 초안을 모두 보여주고 확인을 받은 뒤 **순서대로 커밋**하세요.

## 2. 커밋 메시지 생성

분석 결과를 바탕으로 아래 정책에 맞는 커밋 메시지 초안을 작성하세요.

### 형식

```
<type>: <JIRA-KEY> <description>

[optional body]

[optional footer]
```

### Type 종류

| Type | 용도 |
|------|------|
| feat | 새 기능 추가 |
| fix | 버그 수정 |
| docs | 문서 수정 |
| style | 코드 포맷, 세미콜론 등 (로직 변경 없음) |
| refactor | 리팩토링 (기능 변경 없음) |
| test | 테스트 추가/수정 |
| build | 빌드 시스템, 의존성 변경 |
| ci | CI/CD 설정 변경 |
| perf | 성능 개선 |
| chore | 기타 작업 |

### 작성 규칙

- **제목**: `<type>: <JIRA-KEY> <description>` 한 줄로 간결하게
- **본문** (선택): 세부 변경 내용과 이유를 `-` 목록으로 작성

### 예시

```
feat: DD-123 JWT 인증 시스템 추가

사용자 인증을 위해 JWT 기반의 인증 시스템을 구현했습니다.
- 로그인 요청 시 JWT 발급 및 헤더 검증 로직 추가
- 토큰 만료 처리 및 리프레시 토큰 기능 구현
```

## 3. 사용자 확인

생성한 커밋 메시지 초안을 보여주고 수정 여부를 확인하세요.

## 4. 커밋 실행

확인이 완료되면 `git add`와 `git commit`을 실행하세요.

---

JIRA 이슈 키: $ARGUMENTS
