---
name: be-check
description: springboot-boilerplate 에서 사용자가 "check", "정적 분석", "checkstyle", "pmd", "코드 검사", "포맷 정렬"을 요청할 때 이 스킬을 사용합니다. BE Java 코드를 생성하거나 수정하거나 리팩토링했다면, 명시적으로 요청하지 않아도 반드시 이 스킬을 실행하세요. 코드 변경이 있었다면 항상 마지막 단계로 자동 실행합니다.
argument-hint: "[파일명]"
allowed-tools: Bash
---

# 정적 분석 & 포맷 검증

Google Java Style 포맷 정렬과 Checkstyle + PMD 정적 분석을 순서대로 실행합니다.

포맷 정렬을 먼저 하는 이유는 Checkstyle이 들여쓰기·공백·줄 길이를 검사하기 때문에,
정렬 없이 분석하면 실제 코드 문제가 아닌 스타일 오류가 섞여 노이즈가 발생합니다.

## 실행 순서

1. JetBrains MCP `reformat_file`로 Google Java Style 자동 정렬
   - `projectPath`는 `springboot-boilerplate/`의 절대 경로를 넘긴다 (BE CLAUDE.md의 JetBrains 규칙 참조)
2. `springboot-boilerplate/` 디렉토리에서 `./gradlew checkstyleMain pmdMain` 실행 (루트 세션이면 `cd springboot-boilerplate &&` 를 앞에 붙이고, 이미 그 디렉토리에서 시작한 세션이면 생략)
3. 오류가 있으면 내용을 분석해 수정한 뒤 재실행
4. 통과하면 결과 보고

---

대상: $ARGUMENTS
