# Spring Boot + React 보일러플레이트

Spring Boot 백엔드와 React 프론트엔드를 함께 담은 모노레포 (`spring-react-boilerplate`)

---

## 프로젝트 구조

```
spring-react-boilerplate/
├── springboot-boilerplate/   # Spring Boot 백엔드 API 서버
└── react-boilerplate/        # React 프론트엔드 앱
```

## 전체 동작 설명

구성 요소, 요청 처리 순서, 인증, 캐시, 알림, 파일 업로드, 환경 설정은 [docs/ARCHITECTURE.md](./docs/ARCHITECTURE.md) 에 정리되어 있다.
백엔드 개발자가 FE 코드를 읽고 변경을 검토하는 방법은 [docs/FRONTEND_FOR_BACKEND_DEVS.md](./docs/FRONTEND_FOR_BACKEND_DEVS.md) 를 본다 (Spring 대응표, 다르게 동작하는 것, 리뷰 체크 항목).

## 프로젝트 소개

| 프로젝트 | 설명 | 문서 |
|---------|------|------|
| [springboot-boilerplate](./springboot-boilerplate) | Spring Boot 4 · Java 25 · JPA · Redis · JWT | [README](./springboot-boilerplate/README.md) |
| [react-boilerplate](./react-boilerplate) | React 19 · Vite 7 · TypeScript · React Query · Zustand | [README](./react-boilerplate/README.md) |

---

## 로컬 개발 환경

### 1. 인프라 실행 (MySQL, Redis, MinIO, MailHog)

```bash
cd springboot-boilerplate
docker-compose up -d
```

### 2. git hook 연결 (클론 후 한 번)

```bash
git config core.hooksPath .githooks
```

커밋 전에 바뀐 프로젝트의 정적 검사가 돕니다 (BE: Checkstyle + PMD, FE: ESLint + Prettier + tsc). 테스트는 CI 에서만 돕니다.

### 3. 백엔드 / 프론트엔드 실행

각 프로젝트 README 참고:
- [springboot-boilerplate/README.md](./springboot-boilerplate/README.md)
- [react-boilerplate/README.md](./react-boilerplate/README.md)

---

## CI

GitHub Actions 워크플로(`.github/workflows/`)가 PR 과 `main` 푸시에서 변경된 프로젝트의 품질 게이트를 실행합니다.
커밋 시점에는 루트 `.githooks/pre-commit` 이 스테이징된 프로젝트의 정적 검사(BE Checkstyle/PMD, FE ESLint/Prettier/tsc)만 빠르게 돌리고, 인프라가 필요한 테스트는 CI 가 맡습니다.

`be-ci.yml` 은 `springboot-boilerplate/**` 변경 시 MySQL/Redis 서비스 컨테이너를 띄워 `./gradlew checkstyleMain pmdMain` 과 `./gradlew test -Pcoverage`(JaCoCo 최소 50%)를 실행하고 커버리지 리포트를 아티팩트로 올립니다.
`fe-ci.yml` 은 `react-boilerplate/**` 변경 시 ESLint, Prettier, `tsc`, Vitest, `build:prd` 를 실행하고 별도 job 에서 Playwright E2E(chromium, MSW 모드)를 실행합니다.
같은 브랜치에서 새 실행이 시작되면 진행 중인 이전 실행은 자동으로 취소됩니다.

## 통합 테스트 (ego lite)

CI 의 Playwright E2E 는 MSW 목업으로 화면만 확인합니다. 실제 BE, MySQL, Redis, MinIO, MailHog 를 함께 확인하려면
로컬 인프라와 BE(9091), FE(5173, MSW 꺼짐)를 띄운 뒤 Claude Code 에서 아래를 실행합니다.

```
/qa-scenario all            # 전체 (세션 미활동 만료 시나리오 제외)
/qa-scenario QA-AUTH-04     # 지정한 시나리오만
```

시나리오는 [docs/qa/scenarios.md](./docs/qa/scenarios.md), 결과는 `.qa-reports/<시각>/report.md` 에 남습니다(git 제외).
테스트 계정은 `qa-` 접두사로 만들고 실행이 끝나면 자동으로 지웁니다.

---

## 개발 방식 (Claude Code)

FE와 BE를 함께 다루면 모노레포 루트에서, 한쪽만 다루면 해당 프로젝트 디렉토리에서 Claude Code를 실행합니다.
스킬과 에이전트는 루트 `.claude/`에 있어 어느 위치에서 시작해도 로드되고, 각 프로젝트의 규칙은
해당 디렉토리의 `CLAUDE.md`와 `.claude/rules/`에서 파일을 다룰 때 자동으로 로드됩니다.
hooks와 permissions는 시작 위치의 `.claude/settings.json`만 적용되므로 루트와 각 프로젝트에 하나씩 있습니다.

```bash
# FE + BE
cd spring-react-boilerplate && claude

# BE 만
cd spring-react-boilerplate/springboot-boilerplate && claude

# FE 만
cd spring-react-boilerplate/react-boilerplate && claude
```

스킬과 에이전트는 `fe-` / `be-` 접두사로 구분합니다 (예: `/fe-check`, `/be-crud`). 전체 목록은 루트 `CLAUDE.md` 참고.
스킬은 절차가 길거나 도구를 묶는 작업(정적 분석, 생성, 테스트 생성, QA)에만 두고, 리뷰나 설명처럼 말로 시켜도 되는 작업은 `.claude/rules/` 의 규칙에 맡깁니다.
파일을 만드는 스킬은 Claude 가 상황에 맞게 제안하되 실행 전에 승인 대화상자가 뜹니다(`skill-confirm.sh` hook). `/qa-scenario` 만 직접 실행합니다.

`.claude/settings.json` 의 `permissions.deny` 는 비밀이 들어갈 수 있는 파일(`.env.local`, `.env.*.local`, `*.pem`, `*.key`, prd 설정)과 빌드 산출물만 막습니다.
커밋된 설정 파일(`react-boilerplate/env/.env.dev` 등, BE 의 dev/stg yml)은 환경변수 이름과 공개 값만 담고 있어 막지 않습니다.

두 프로젝트는 REST API로 연동됩니다. API 스펙 변경 시 백엔드에서 먼저 수정하고 프론트엔드에 반영하는 순서로 작업합니다.

## 한쪽만 떼어 쓸 때

스킬, 에이전트, hook 스크립트는 루트 `.claude/` 에 있고 하위 `settings.json` 은 `../.claude/hooks/` 를 가리킵니다.
그래서 `springboot-boilerplate/` 나 `react-boilerplate/` 디렉토리만 복사하면 스킬과 에이전트가 사라지고 hook 이 깨집니다.
한쪽만 쓰려면 아래 순서로 옮깁니다 (BE 기준, FE 는 `be` 를 `fe` 로 바꿉니다).

```bash
cp -r springboot-boilerplate /path/to/new-project && cd /path/to/new-project
mkdir -p .claude/skills .claude/agents .claude/hooks
cp -r /path/to/spring-react-boilerplate/.claude/skills/be-* .claude/skills/
cp /path/to/spring-react-boilerplate/.claude/agents/be-*.md .claude/agents/
cp /path/to/spring-react-boilerplate/.claude/hooks/common.sh /path/to/spring-react-boilerplate/.claude/hooks/skill-confirm.sh /path/to/spring-react-boilerplate/.claude/hooks/be-*.sh .claude/hooks/
cp -r /path/to/spring-react-boilerplate/.githooks . && git config core.hooksPath .githooks
sed -i '' 's#/../.claude/hooks/#/.claude/hooks/#g' .claude/settings.json
```

그 뒤에 손으로 정리할 것:
- 루트 `CLAUDE.md` 의 "공통 규칙" 절을 하위 `CLAUDE.md` 맨 앞에 옮깁니다 (이모지 금지, 한국어, 커밋 정책).
- 하위 `CLAUDE.md` 와 스킬 문서에서 `cd springboot-boilerplate && ` 접두사와 `springboot-boilerplate/` 경로를 지웁니다 (단독 프로젝트에서는 현재 디렉토리가 곧 프로젝트).
- hook 스크립트는 경로를 저장소 루트 기준으로 보고 `springboot-boilerplate/` 로 시작하는지 검사합니다. 단독 저장소에서는 `common.sh` 의 `hook_project_of` 에서 `springboot-boilerplate/*` 패턴을 `*` 로, `.githooks/pre-commit` 의 `^springboot-boilerplate/` 를 `^` 로 바꾸고 FE 블록은 지웁니다.
- `.github/workflows/be-ci.yml` 을 가져가면 `paths` 필터와 `working-directory`, 아티팩트 경로에서 `springboot-boilerplate/` 를 지웁니다.
- `/qa-scenario` 는 FE 와 BE 를 함께 쓰는 스킬이라 가져가지 않습니다.

## 새 프로젝트로 시작할 때 바꿀 이름

이름이 여러 곳에 흩어져 있어 하나씩 바꿔야 합니다. 바꾼 뒤 `git grep -n -i boilerplate` 로 남은 곳을 확인합니다.

| 구분 | 현재 값 | 위치 |
|------|---------|------|
| Java 패키지 | `com.example.api` | `src/main/java`, `src/test/java` 디렉토리와 `package` 선언, `common/constants/Constants.BASE_PACKAGE`, `logback-spring.xml` 의 logger 이름, `resources/mapper/**/*.xml` 의 `namespace`, `build.gradle` 의 `group` |
| 애플리케이션 이름 | `springboot-boilerplate` | `settings.gradle` 의 `rootProject.name`, `application.yml` 의 `spring.application.name`, `docker-compose.yml` 의 `container_name` 들, `docker/prometheus/prometheus.yml` 의 `job_name`, `config/pmd/custom-ruleset.xml` 의 description |
| DB 이름과 계정 | `boilerplate_db`, `boilerplate` / `boilerplate` | `docker-compose.yml`, `docker/init.sql` 의 `USE`, `datasource-local.yml`, `.github/workflows/be-ci.yml` 의 MySQL 서비스 환경변수 |
| Redis 네임스페이스 | `APP` | `common/type/RedisKeys.NAMESPACE` (Redis 하나를 여러 서비스가 같이 쓸 때 키가 섞이지 않게 서비스 이름으로) |
| S3 버킷 | `boilerplate-bucket` | `environment-{profile}.yml` 의 `aws.s3.bucket-name`, Swagger 예시 값(`file/dto/FileDto.java` 등), `react-boilerplate/src/mocks/fileHandlers.ts` 의 목업 URL |
| 메일 발신자와 제목 | `noreply@boilerplate.local`, `[Boilerplate]` | `environment-{profile}.yml` 의 `mail.from`, `auth/service/EmailService.java` 의 `setSubject` |
| FE 패키지 이름 | `react-boilerplate` | `package.json`, `package-lock.json` 의 `name` |
| FE 화면 이름 | `React Boilerplate` | `index.html` 의 `<title>`, `src/app/layout/Layout.tsx` 상단 바, `src/app/pages/HomePage.tsx` 문구 |
| CI 경로 필터 | `springboot-boilerplate/**`, `react-boilerplate/**` | `.github/workflows/*.yml` (디렉토리 이름을 바꿀 때만) |

JWT 비밀키, Jasypt 비밀번호, DB 비밀번호는 이름이 아니라 비밀값이므로 prd 에서는 환경변수로 넣습니다 (`environment-prd.yml`, `datasource-prd.yml` 은 환경변수 이름만 담고 있습니다).
