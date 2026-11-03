---
name: qa-scenario
description: docs/qa/scenarios.md 의 통합 테스트 시나리오를 실제 로컬 인프라(BE, MySQL, Redis, MinIO, MailHog)에 붙여 ego lite 브라우저로 실행하고, 화면과 부수 효과를 확인해 결과를 보고합니다.
argument-hint: "[all | 시나리오 ID ...]  예: all, QA-TODO-01 QA-AUTH-04"
disable-model-invocation: true
allowed-tools: Bash, Read, Write, Glob, Grep
---

# /qa-scenario — ego lite 통합 테스트 실행

시나리오 원본은 `docs/qa/scenarios.md` 다. 시나리오를 바꾸고 싶으면 그 문서를 고치고, 이 스킬은 실행 방법만 정한다.
경로는 모두 저장소 루트 기준이다. 도우미는 `.claude/skills/qa-scenario/scripts/qa.sh` (이하 `qa.sh`)를 쓴다.

## 인자

- `all` 또는 인자 없음: 목록 순서대로 전부 실행한다. 단 `QA-SESSION-01` 은 제외하고, `QA-AUTH-05` 는 항상 맨 마지막에 실행한다.
- 시나리오 ID 하나 이상: 지정한 것만 실행한다. `QA-SESSION-01` 은 문서의 "준비" 단계를 사용자가 끝냈다고 확인한 뒤에만 실행한다.

## 다른 포트의 서버를 대상으로 실행할 때

기본 대상은 BE `http://localhost:9091/api`, FE `http://localhost:5173` 이다. 작업 브랜치 코드를 따로 띄워 확인할 때는
메일 링크와 CORS 도 같은 FE 를 가리키게 띄우고, `qa.sh` 호출마다 환경변수로 대상을 넘긴다.

```bash
# BE (springboot-boilerplate)
./gradlew bootRun --args='--server.port=9095 --spring.profiles.active=local --cors.allowed-origins=http://localhost:5174 --mail.verify-url=http://localhost:5174/verify-email --mail.password-reset-url=http://localhost:5174/password/reset'
# FE (react-boilerplate)
VITE_APP_ENV=local VITE_API_BASE_URL=http://localhost:9095/api VITE_ENABLE_MSW=false npx vite --port 5174 --strictPort
# qa.sh
QA_BE_URL=http://localhost:9095/api QA_FE_URL=http://localhost:5174 .claude/skills/qa-scenario/scripts/qa.sh preflight
```

## 절차

### 1. 준비

1. `docs/qa/scenarios.md` 를 읽고 실행할 시나리오와 공통 규칙을 확인한다.
2. `qa.sh preflight` 를 실행한다. 하나라도 `[FAIL]` 이면 시나리오를 실행하지 말고, 실패 항목과 해결 방법을 보고하고 멈춘다.
3. `ego-browser skill` 을 실행해 현재 설치된 ego lite 사용법을 읽고 따른다. 출력 끝에 `[ego-browser:skill] end of guide` 줄이 없으면 파일로 저장해 나눠 읽는다.
4. 실행 시각을 `RUN=$(date +%s)` 처럼 정해 두고, 테스트 계정 이메일에 붙인다 (예: `qa-todo-$RUN@example.com`).
5. 결과 폴더를 만든다: `.qa-reports/<YYYYMMDD-HHMMSS>/` (git 에서 제외된 폴더). 스크린샷과 보고서를 여기에 둔다.

### 2. 브라우저

- TaskSpace 는 실행 전체에서 **하나만** 쓴다: `taskSpace("qa-scenario <RUN>")`. 이후 라운드는 출력한 `spaceId` 로 이어 간다.
- 시나리오마다 로그인 상태가 섞이지 않게, 시작할 때 `page.evaluate(() => localStorage.clear())` 후 `/` 로 이동한다. 쿠키나 브라우저 전체 저장소는 지우지 않는다 (ego-browser 의 clearing-state 안내 참고).
- 요소는 스냅샷의 ref 나 `loc=role:...`, `text="..."` 로 찾는다. 좌표 클릭은 스냅샷으로 대상을 못 찾을 때만 쓴다.
- 기다릴 때는 고정 대기 대신 `waitForURL`, `waitForSelector`, `waitForFunction` 을 쓴다. SSE 토스트처럼 비동기로 뜨는 것만 최대 10초까지 기다린다.
- 입력칸은 `loc=css:input[name="email"]` 처럼 name 속성으로 찾는다 (`input[type=email]` 은 맞지 않을 수 있다).
  `text=회원가입` 처럼 제목과 버튼에 같은 글자가 있으면 모호하다는 오류가 나므로 `loc=role:button[name='회원가입']` 을 쓴다.
- `waitForFunction()` 은 조건이 참이 됐는지만 돌려준다. 화면의 값(이미지 주소, 토큰 등)은 그 뒤에 `page.evaluate()` 로 읽는다.
- 주소가 바뀐 직후의 스냅샷은 이전 화면일 수 있다. 다음 화면 요소를 `waitForSelector()` 로 기다린 뒤 스냅샷을 찍는다.
- 메일 링크, 토큰처럼 셸에서 얻은 값은 스크립트에 환경변수로 넘어가지 않는다. 임시 파일에 쓰고 스크립트에서 절대 경로로 읽는다.
- Access Token 은 `page.evaluate(() => JSON.parse(localStorage.getItem("user-store")).state.accessToken)` 로 읽는다.
- 첫 시나리오에서 실제 BE 에 붙었는지 확인한다: 화면 동작 뒤 DB 나 MailHog 에 부수 효과가 생겨야 한다. 안 생기면 FE 가 MSW 모드일 수 있으니 멈추고 보고한다.

### 3. 시나리오 실행

시나리오마다:

1. 문서의 **사전 조건**을 `qa.sh` 로 준비한다 (계정은 `qa.sh create-user`).
2. **단계**를 브라우저로 수행한다. 한 라운드에 여러 동작을 묶고, 라운드 끝에 다음 판단에 필요한 스냅샷을 출력한다.
3. **기대 결과**를 화면으로 확인하고, **부수 효과**를 `qa.sh sql`, `qa.sh redis`, `qa.sh mail-count`, `qa.sh api` 로 확인한다.
4. 판정한다.
   - `PASS`: 기대 결과와 부수 효과가 모두 맞음
   - `FAIL`: 하나라도 다름. 그 시점 스크린샷을 결과 폴더에 저장하고, 관련 API 응답의 `X-Trace-Id` 를 기록한다
   - `BLOCKED`: 앞 단계 실패나 환경 문제로 끝까지 진행하지 못함
5. 실패해도 다음 시나리오로 넘어간다. 같은 동작을 무작정 반복하지 않는다. 한 번 더 확인할 때는 현재 화면부터 다시 관찰한다.

### 4. 정리

1. `qa.sh cleanup` 으로 `qa-` 계정과 데이터를 지운다.
2. `task.finish({ keep: [] })` 로 TaskSpace 를 닫는다. 정확히 한 번, 마지막에만 호출한다.
3. 출력에 `[ego-browser:notice]` 가 있으면 사용자에게 ego lite 업데이트가 있다고 알린다. 업그레이드는 사용자 승인 후에만 한다.

### 5. 보고

`.qa-reports/<시각>/report.md` 에 쓰고, 사용자에게도 요약한다.

```markdown
# QA 결과 <시각>

| ID | 결과 | 비고 |
|----|------|------|
| QA-AUTH-01 | PASS | |
| QA-AUTH-04 | FAIL | 재설정 메일 링크가 404 화면으로 이동 |

## 실패 상세
### QA-AUTH-04
- 단계: 3
- 기대: "새 비밀번호 설정" 화면
- 실제: 404 화면 (URL: http://localhost:5173/reset-password?token=...)
- 증거: screenshots/QA-AUTH-04-step3.png, X-Trace-Id: 3f9a1c0b7d2e4a61
- 추정 원인: (코드나 설정에서 확인한 내용. 확인하지 않았으면 "미확인")
```

## 지켜야 할 것

- **샘플 계정(user1, user2, admin)으로 로그인하지 않는다.** 로그인하면 그 계정의 Refresh Token 이 바뀌어 사용자의 기존 세션이 끊긴다. 관리자가 필요하면 `qa.sh create-user <email> <pw> ADMIN`.
- `qa-` 로 시작하지 않는 데이터는 만들거나 지우지 않는다. `qa.sh sql` 로 UPDATE, DELETE 를 할 때는 조건에 `qa-` 이메일을 반드시 넣는다.
- `react-boilerplate/env/.env.*` 와 읽기 금지된 설정 파일은 읽지 않는다. 다른 경로(Vite 가 변환한 모듈 등)로 우회해서 값을 보지도 않는다.
- 실행 중 발견한 버그는 **보고만 하고 고치지 않는다.** 수정은 사용자가 요청할 때 따로 한다.
- 이 스킬은 시나리오 문서를 고치지 않는다. 시나리오와 실제 동작이 달라 문서가 틀렸다고 판단되면 보고서에 "시나리오 수정 제안"으로 적는다.
