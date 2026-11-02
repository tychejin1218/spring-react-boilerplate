# Boilerplate

Spring Boot + React 보일러플레이트

---

## 프로젝트 구조

```
claude-code/
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

백엔드와 프론트엔드는 독립적인 터미널에서 각각 Claude Code를 실행합니다.

```bash
# 터미널 1 — 백엔드
cd springboot-boilerplate
claude

# 터미널 2 — 프론트엔드
cd react-boilerplate
claude
```

두 프로젝트는 REST API로 연동됩니다. API 스펙 변경 시 백엔드에서 먼저 수정하고 프론트엔드에 반영하는 순서로 작업합니다.
