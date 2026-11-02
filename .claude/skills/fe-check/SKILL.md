---
name: fe-check
description: react-boilerplate 에서 "check", "lint", "포맷", "코드 검사", "정적 분석"을 요청할 때 이 스킬을 사용합니다. FE 코드를 생성하거나 수정하거나 리팩토링했다면, 명시적으로 요청하지 않아도 반드시 이 스킬을 실행하세요. FE 코드 변경이 있었다면 항상 마지막 단계로 자동 실행합니다.
argument-hint: "[파일명]"
allowed-tools: Bash
---

# FE ESLint & Prettier 검증

`react-boilerplate/` 에서 ESLint 정적 분석과 Prettier 포맷 체크를 순서대로 실행합니다.
모든 명령은 `react-boilerplate/` 디렉토리에서 실행합니다. 루트에서 시작한 세션이면 `cd react-boilerplate && ...` 로, 이미 `react-boilerplate/` 에서 시작한 세션이면 `cd` 없이 실행합니다.

Prettier 포맷 체크를 먼저 하는 이유는 ESLint의 `eslint-plugin-prettier`가 포맷 규칙을 ESLint 오류로 보고하기 때문에,
포맷을 먼저 정렬하면 실제 코드 품질 문제와 스타일 오류를 구분하기 쉽습니다.

## 실행 순서

1. `npm run lint` 실행 — ESLint 검사
2. 오류가 있으면 `npm run lint:fix`로 자동 수정 시도
3. `npm run format:check` 실행 — Prettier 포맷 체크
4. 포맷 오류가 있으면 `npm run format`으로 자동 수정
5. 수정 후 `npm run lint`를 재실행하여 최종 확인
6. 통과하면 결과 보고

## 자동 수정 불가 오류 처리

자동 수정으로 해결되지 않는 오류는:
- 오류 내용을 분석하여 원인 설명
- 수동 수정 방법 안내
- 수정 후 재실행 요청

---

대상: $ARGUMENTS
