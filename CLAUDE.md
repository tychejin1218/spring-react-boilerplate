# 프로젝트 개요

Spring Boot 백엔드 + React 프론트엔드 보일러플레이트 모노레포.

```
spring-react-boilerplate/
├── .claude/                  # 모노레포 공통 Claude Code 설정 (settings, hooks, skills, agents)
├── springboot-boilerplate/   # Spring Boot 4 · Java 25 · JPA · QueryDSL · MyBatis · Redis
│   ├── CLAUDE.md             # BE 규칙 (BE 파일을 다룰 때 자동 로드)
│   └── .claude/
│       ├── settings.json     # BE 단독 실행용 settings (permissions, hooks)
│       └── rules/            # BE 상세 규칙 (해당 경로 파일 편집 시 자동 로드)
└── react-boilerplate/        # React 19 · Vite 7 · TypeScript · TanStack Query · Zustand
    ├── CLAUDE.md             # FE 규칙 (FE 파일을 다룰 때 자동 로드)
    └── .claude/
        ├── settings.json     # FE 단독 실행용 settings (permissions, hooks)
        └── rules/            # FE 상세 규칙 (해당 경로 파일 편집 시 자동 로드)
```

## Claude Code 실행 위치

| 작업 범위 | 시작 위치 | 로드되는 것 |
|-----------|-----------|-------------|
| FE + BE 를 함께 다루는 작업, 에이전트 팀 | 루트 (`claude`) | 루트 CLAUDE.md, 루트 `.claude/settings.json`(hooks, permissions), 하위 CLAUDE.md 는 해당 파일을 다룰 때 로드 |
| BE 만 | `cd springboot-boilerplate && claude` | 루트 + BE CLAUDE.md, `springboot-boilerplate/.claude/settings.json` |
| FE 만 | `cd react-boilerplate && claude` | 루트 + FE CLAUDE.md, `react-boilerplate/.claude/settings.json` |

스킬과 에이전트는 루트 `.claude/`에 있으며 어느 위치에서 시작해도 상위 디렉토리를 거슬러 올라가 로드된다.
반면 `.claude/settings.json`(hooks, permissions)은 상위 디렉토리에서 상속되지 않으므로, 하위 프로젝트에는
자체 완결형 `settings.json`을 두고 루트의 `.claude/hooks/` 스크립트를 `../.claude/hooks/` 경로로 참조한다.
하위에서 시작한 세션이 다른 프로젝트 파일까지 다뤄야 하면 `claude --add-dir ../react-boilerplate` 처럼 추가한다.
어떤 CLAUDE.md 가 로드됐는지는 세션에서 `/context` 로 확인한다.

## 각 프로젝트 규칙

하위 프로젝트의 `CLAUDE.md`는 그 디렉토리의 파일을 읽거나 편집할 때 자동으로 로드된다.
작업 전 담당 프로젝트의 `CLAUDE.md`와 `.claude/rules/`를 먼저 읽는다.

- FE 규칙: [react-boilerplate/CLAUDE.md](react-boilerplate/CLAUDE.md)
- BE 규칙: [springboot-boilerplate/CLAUDE.md](springboot-boilerplate/CLAUDE.md)
- 전체 동작 설명 (인증, 캐시, 알림, 파일 업로드 흐름): [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)

## 공통 규칙

- 이모지 사용 금지 (응답, 코드, 문서 모두)
- 응답, 주석, 문서는 한국어로 작성
- 커밋과 푸시는 사용자가 명시적으로 요청할 때만 수행
- 빌드/테스트 명령은 해당 프로젝트 디렉토리에서 실행 (루트 세션: `cd springboot-boilerplate && ./gradlew ...`, `cd react-boilerplate && npm run ...`; 하위에서 시작한 세션은 `cd` 생략)

## Claude Code 도구 (루트 `.claude/`)

스킬과 에이전트는 모두 루트 `.claude/`에 있고 이름에 `fe-` / `be-` 접두사가 붙는다.

| 구분 | FE | BE |
|------|----|----|
| 정적 분석 | `/fe-check` | `/be-check` |
| 코드 리뷰 | `/fe-review` (`fe-code-reviewer`) | `/be-review` (`be-code-reviewer`) |
| 코드 설명 | `/fe-explain` | `/be-explain` |
| 생성 | `/fe-scaffold` (`fe-feature-scaffolder`) | `/be-crud` (`be-crud-generator`) |
| 테스트 생성 | `/fe-vitest-gen`, `/fe-e2e-gen` | `/be-test` |
| 기타 | | `/be-perf`, `/be-refactor`, `/be-api-doc`, `/be-cleanup`, `/be-commit` |
| 자동 호출 에이전트 | `fe-debugger` | `be-debugger`, `be-mybatis-specialist`, `be-schema-designer`, `be-performance-inspector` |

Hooks(스크립트는 루트 `.claude/hooks/`, 등록은 루트와 각 하위 프로젝트의 `.claude/settings.json`):
- `fe-format.sh`: `react-boilerplate/` 소스 편집 후 Prettier + ESLint --fix (node_modules 설치 필요)
- `be-java-notice.sh`: `springboot-boilerplate/` Java 편집 후 포맷/정적 분석 필요 안내
- `be-commit-gate.sh`: BE 변경이 스테이징된 `git commit` 전 Checkstyle + PMD 실행, 실패 시 차단
- `team-scope-guard.sh`: 에이전트 팀 팀원의 수정 범위 강제 (아래 참고, 루트 settings 에만 등록)

hook 스크립트는 파일 경로를 항상 저장소 루트 기준으로 정규화하므로 루트 세션과 하위 세션 양쪽에서 동작한다.

시크릿과 빌드 산출물(`build/`, `dist/`, `node_modules/`) 보호는 각 `settings.json`의 `permissions.deny` Read 규칙으로 처리한다 (`.claudeignore`는 Claude Code가 지원하지 않음).
워크트리 세션에서는 루트 settings 의 `worktree.symlinkDirectories` 로 `react-boilerplate/node_modules` 를 메인 체크아웃과 공유한다.

## 에이전트 팀 사용 시 규칙

- 팀원은 반드시 `fe-*` 또는 `be-*` 에이전트 정의로 생성한다 (예: "fe-feature-scaffolder 타입으로 팀원을 만들어"). 이 접두사로 `team-scope-guard.sh`가 수정 범위를 강제한다.
  - `fe-*` 팀원: `react-boilerplate/` 하위만 수정
  - `be-*` 팀원: `springboot-boilerplate/` 하위만 수정 (`springboot-boilerplate/docker-compose.yml` 포함)
- 루트 파일(`CLAUDE.md`, `.claude/`, `README.md`)은 리드만 수정한다
- 작업 시작 전 담당 프로젝트의 CLAUDE.md와 `.claude/rules/`를 먼저 읽는다
- 태스크 시작 시 TaskUpdate로 status를 in_progress로, 완료 시 completed로 변경한다
- 작업 완료 후 해당 프로젝트의 TASKS.md 체크박스를 업데이트한다
- API 스펙 변경은 BE에서 먼저 확정하고 FE에 반영한다

## 인프라

두 프로젝트는 REST API로 연동된다.

| 항목 | 값 |
|------|-----|
| BE 포트 | 9091 (context-path: /api) |
| FE 포트 | 5173 (개발), 80 (Docker) |
| API Base URL | http://localhost:9091/api |

로컬 인프라 실행:
```bash
cd springboot-boilerplate
docker-compose up -d
```
