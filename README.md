# spring-react-boilerplate

Spring Boot + React 보일러플레이트

---

## 프로젝트 구조

```
spring-react-boilerplate/
├── springboot-boilerplate/   # Spring Boot 백엔드 API 서버
└── react-boilerplate/        # React 프론트엔드 앱
```

## 프로젝트 소개

| 프로젝트 | 설명 | 문서 |
|---------|------|------|
| [springboot-boilerplate](./springboot-boilerplate) | Spring Boot 4 · Java 25 · JPA · Redis · JWT | [README](./springboot-boilerplate/README.md) |
| [react-boilerplate](./react-boilerplate) | React 19 · Vite 7 · TypeScript · React Query · Zustand | [README](./react-boilerplate/README.md) |

---

## 로컬 개발 환경

### 1. 인프라 실행 (MySQL + Redis)

```bash
cd springboot-boilerplate
docker-compose up -d
```

### 2. 백엔드 / 프론트엔드 실행

각 프로젝트 README 참고:
- [springboot-boilerplate/README.md](./springboot-boilerplate/README.md)
- [react-boilerplate/README.md](./react-boilerplate/README.md)

---

## 개발 방식 (Claude Code)

모노레포 루트에서 Claude Code를 실행합니다. 공통 설정(hooks, 스킬, 에이전트)은 루트 `.claude/`에 있고,
각 프로젝트의 규칙은 해당 디렉토리의 `CLAUDE.md`와 `.claude/rules/`에서 파일을 다룰 때 자동으로 로드됩니다.

```bash
cd spring-react-boilerplate
claude
```

스킬과 에이전트는 `fe-` / `be-` 접두사로 구분합니다 (예: `/fe-check`, `/be-review`). 전체 목록은 루트 `CLAUDE.md` 참고.
에이전트 팀을 쓸 때는 팀원을 `fe-*` / `be-*` 에이전트 타입으로 만들어야 수정 범위 제한 hook이 동작합니다.

두 프로젝트는 REST API로 연동됩니다. API 스펙 변경 시 백엔드에서 먼저 수정하고 프론트엔드에 반영하는 순서로 작업합니다.
