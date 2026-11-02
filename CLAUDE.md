# 프로젝트 개요

Spring Boot 백엔드 + React 프론트엔드 보일러플레이트 모노레포

```
spring-react-boilerplate/
├── springboot-boilerplate/   # Spring Boot 4 · Java 25 · JPA · QueryDSL · MyBatis · Redis
└── react-boilerplate/        # React 19 · Vite 7 · TypeScript · TanStack Query · Zustand
```

## 각 프로젝트 상세 규칙

각 프로젝트의 CLAUDE.md와 `.claude/rules/`를 반드시 읽고 작업한다.

- FE 규칙: @react-boilerplate/CLAUDE.md
- BE 규칙: @springboot-boilerplate/CLAUDE.md

## 에이전트 팀 사용 시 공통 규칙

- 이모지 사용 금지 (응답, 코드, 문서 모두)
- 각 팀원은 자신이 담당하는 프로젝트 디렉토리 밖의 파일은 수정하지 않는다
  - FE 팀원: `react-boilerplate/` 하위만
  - BE 팀원: `springboot-boilerplate/` 하위만
  - 단, 루트의 `docker-compose.yml`은 BE 팀원이 담당
- 작업 시작 전 담당 프로젝트의 CLAUDE.md와 `.claude/rules/`를 먼저 읽는다
- 태스크 시작 시 TaskUpdate로 status를 in_progress로, 완료 시 completed로 변경한다
- 작업 완료 후 해당 프로젝트의 TASKS.md 체크박스를 업데이트한다

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
