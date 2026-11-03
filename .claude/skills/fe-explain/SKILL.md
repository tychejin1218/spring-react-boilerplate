---
name: fe-explain
description: react-boilerplate 파일의 역할, 사용 패턴, 데이터 흐름을 설명합니다.
argument-hint: "<파일명>"
allowed-tools: Read, Glob, Grep
---

# FE 코드 설명

`react-boilerplate/` 의 지정된 파일을 처음 보는 개발자도 이해할 수 있게 설명해주세요.

## 사전 준비

대상 파일과 연관 파일(타입 정의, API 함수, 훅, 관련 컴포넌트 등)을 함께 읽어 전체 맥락을 파악하세요.

## 설명 항목

### 1. 파일 역할
- 이 파일이 3계층(app / features / shared) 중 어디에 위치하는지
- 어떤 도메인/기능을 담당하는지
- 컴포넌트, 훅, API 함수, 유틸 중 어떤 종류인지

### 2. 주요 패턴 및 라이브러리 의미
프로젝트에서 자주 쓰이는 패턴을 맥락에 맞게 설명:
- `useQuery` / `useMutation` — 서버 상태 관리 및 캐시 무효화
- `queryKey` 팩토리 — 일관된 캐시 키 관리
- `select` 옵션 — 응답 데이터 변환 및 파생 상태
- MSW 핸들러 — 개발/테스트 환경 API Mocking
- `SuspenseBoundary` / `ErrorBoundary` — 비동기 로딩·에러 처리
- 기타 파일에 등장하는 패턴

### 3. 함수/컴포넌트별 역할
각 export된 함수 및 컴포넌트에 대해:
- 어떤 입력을 받아서 무엇을 반환/렌더링하는지
- 내부에서 어떤 라이브러리(TanStack Query, Axios, Zustand 등)를 사용하는지
- 에러 또는 엣지 케이스 처리 방식

### 4. 데이터 흐름
요청부터 화면 표시까지의 흐름을 단계별로 서술:
```
예) 사용자 액션 → 컴포넌트 이벤트 핸들러 → useMutation → API 함수 → Axios 인터셉터 → API 서버
                                                        → onSuccess → invalidateQueries → useQuery 재실행 → 화면 갱신
```

### 5. 핵심 비즈니스 로직
코드만 봐서는 의도를 파악하기 어려운 부분을 중점 설명:
- 조건부 렌더링 로직
- 상태 업데이트 패턴
- 에러 처리 전략

## 출력 형식

- 전문 용어는 간단히 풀어서 설명
- 코드 스니펫을 인용하며 설명
- 필요 시 흐름도(텍스트 형식)로 시각화

---

대상: $ARGUMENTS
