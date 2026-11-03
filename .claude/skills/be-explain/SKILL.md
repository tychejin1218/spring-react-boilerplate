---
name: be-explain
description: springboot-boilerplate 전용. 지정된 파일의 역할, 어노테이션 의미, 코드 흐름을 설명합니다.
argument-hint: "<파일명>"
allowed-tools: Read, Glob, Grep
---

# BE 코드 설명

지정된 파일을 처음 보는 개발자도 이해할 수 있게 설명해주세요.

## 사전 준비

대상 파일과 연관 파일(Entity, DTO, Repository 등)을 함께 읽어 전체 맥락을 파악하세요.

## 설명 항목

### 1. 클래스 역할
- 이 클래스가 4계층(Controller → Service → Repository → DB) 중 어디에 위치하는지
- 어떤 도메인/기능을 담당하는지

### 2. 주요 어노테이션 의미
프로젝트에서 자주 쓰이는 어노테이션을 맥락에 맞게 설명:
- `@Transactional(readOnly = true)` / `@Transactional` — Reader/Writer DB 라우팅
- `@Entity`, `@Table`, `@Column` — JPA 테이블 매핑
- `@RequiredArgsConstructor` — 생성자 주입 자동 생성
- `@Alias` — MyBatis 타입 별칭
- 기타 파일에 등장하는 어노테이션

### 3. 메서드별 역할
각 public 메서드에 대해:
- 어떤 요청을 받아서 무엇을 반환하는지
- 내부에서 어떤 계층(JPA / QueryDSL / MyBatis)을 호출하는지
- 예외가 발생하는 조건

### 4. 데이터 흐름
요청 → 응답까지의 흐름을 단계별로 서술:
```
예) HTTP 요청 → Controller → Service → QueryDSL Repository → DB → DTO 변환 → BaseResponse 반환
```

### 5. 핵심 비즈니스 로직
코드만 봐서는 의도를 파악하기 어려운 부분을 중점 설명

## 출력 형식

- 전문 용어는 간단히 풀어서 설명
- 코드 스니펫을 인용하며 설명
- 필요 시 흐름도(텍스트 형식)로 시각화

---

대상: $ARGUMENTS
