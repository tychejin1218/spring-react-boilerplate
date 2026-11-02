---
paths:
  - "springboot-boilerplate/src/main/java/**/entity/**/*.java"
  - "src/main/java/**/entity/**/*.java"
  - "springboot-boilerplate/src/main/resources/**/*.sql"
  - "src/main/resources/**/*.sql"
  - "springboot-boilerplate/**/*.sql"
  - "**/*.sql"
---
# DB 네이밍 컨벤션

효율적인 협업과 일관된 데이터베이스 설계를 위한 네이밍 규칙입니다.
본 가이드는 프로젝트 특성에 따라 팀 내 논의를 통해 유연하게 조정할 수 있습니다.

---

## 1. Database Schema Name

- 소문자로 작성, 최대 8자
- DB Alias 이름과 동일하게 작성
- 사이트별 고유한 이름 사용

```
예시: app
```

---

## 2. Table Name

- 소문자 + 언더스코어
- 형식: `<시스템 구분>_<테이블명>`
- 데이터 양이 많을 것으로 예상되는 테이블은 파티셔닝 고려 (학습 이력, 문제 풀이 이력 등)

```
예시: app_user, app_exam, app_question
```

---

## 3. Column Name

- 소문자 + 언더스코어
- 한 단어 8자 이하, 컬럼명 전체 최대 12자 (DB 특성에 따라 예외 가능)
- 약어 조합 + 접미사로 구성

### 컬럼 배치 순서

1. Primary Key
2. NOT NULL 컬럼
3. 외래키(FK) 컬럼
4. 일반 속성 컬럼 (고정 길이 → 가변 길이 / Date → Number → String / 짧은 → 긴 길이)
5. 감사 컬럼 (항상 마지막): `created_at`, `updated_at`, `deleted_at`

### 공통 감사 컬럼 (모든 테이블 필수)

모든 테이블 마지막에 아래 3개 컬럼을 반드시 포함한다. JPA 쪽은 `domain/entity/BaseAudit.java`(`createdAt`, `updatedAt`, `deletedAt`)를 상속하며, 논리 삭제는 `deleted_at IS NULL` 조건과 `@SQLRestriction`으로 처리한다. 값은 모두 UTC로 저장한다.

```sql
created_at    DATETIME  NOT NULL COMMENT '생성일시(UTC)',
updated_at    DATETIME  NOT NULL COMMENT '수정일시(UTC)',
deleted_at    DATETIME  NULL     COMMENT '삭제일시(UTC, NULL이면 미삭제)'
```

생성자/수정자 추적(`created_by`, `updated_by`)이 필요한 테이블은 `BaseAudit` 확장과 함께 팀 논의 후 추가한다.

---

## 4. Index Name

- 형식: `idx_<테이블명>_<일련번호(01~99)>`

```
예시: idx_app_user_01
```

---

## 5. PK (Primary Key) Name

- 형식: `pk_<테이블명>`
- 단, `BIGINT AUTO_INCREMENT PRIMARY KEY` 인라인 방식 사용 시 별도 PK 제약명 생략 가능

```
예시: pk_app_user
```

---

## 6. FK (Foreign Key) Name

- 형식: `fk_<테이블명>_<일련번호(01~99)>`

```sql
예시: CONSTRAINT fk_app_exam_01 FOREIGN KEY (user_id) REFERENCES app_user(id)
```

---

## 7. Unique Key Name

- 형식: `uk_<테이블명>_<일련번호(01~99)>`

```sql
예시: CONSTRAINT uk_app_user_01 UNIQUE (login_id)
```

---

## 8. View Name

- 형식: `view_<테이블명>_<일련번호(01~99)>`

```
예시: view_app_user_01
```

---

## 9. 사용 금지

- Stored Procedure: 사용 금지
- Trigger: 사용 금지
- Function: 공통 코드 조회 함수 외 사용 금지

---

## 10. DDL 작성 형식

기존 프로젝트 SQL 스타일을 기준으로 작성합니다.

```sql
DROP TABLE IF EXISTS app.app_exam;

-- TABLE
CREATE TABLE app.app_exam (
    id              BIGINT          AUTO_INCREMENT PRIMARY KEY COMMENT '시험지 ID',
    user_id         VARCHAR(50)     NOT NULL COMMENT '회원 ID',
    exam_nm         VARCHAR(200)    NOT NULL COMMENT '시험지명',
    -- ... 일반 속성 컬럼 ...
    created_at      DATETIME        NOT NULL COMMENT '생성일시(UTC)',
    updated_at      DATETIME        NOT NULL COMMENT '수정일시(UTC)',
    deleted_at      DATETIME        NULL COMMENT '삭제일시(UTC, NULL이면 미삭제)',
    CONSTRAINT fk_app_exam_01 FOREIGN KEY (user_id) REFERENCES app_user(id)
) COMMENT = '시험지 정보';

-- INDEX
CREATE INDEX idx_app_exam_01 ON app.app_exam (user_id);
CREATE INDEX idx_app_exam_02 ON app.app_exam (created_at);
```

### 작성 규칙 요약

| 항목 | 규칙 |
|------|------|
| DROP | `DROP TABLE IF EXISTS` 항상 선행 |
| PK | `BIGINT AUTO_INCREMENT PRIMARY KEY` 인라인 |
| 컬럼 정렬 | 컬럼명 기준으로 값을 탭 정렬 |
| 코멘트 | 모든 컬럼에 한국어 COMMENT 필수 |
| 코드 참조 | 공통 코드 컬럼은 `(공통코드: XXX_CODE 코드 참조)` 명시 |
| INDEX | CREATE TABLE 이후 별도 `CREATE INDEX` 문으로 분리 |
| UNIQUE INDEX | `CREATE UNIQUE INDEX ux_<테이블명>_<일련번호>` 형식 |

---

## 11. 공통 코드 컬럼 패턴

코드값을 저장하는 컬럼은 `VARCHAR(50)` + 코멘트에 코드 그룹 명시:

```sql
difficulty_cd   VARCHAR(50)  NULL COMMENT '난이도 코드(공통코드: DIFFICULTY 코드 참조)',
status_cd       VARCHAR(50)  NULL COMMENT '상태 코드(공통코드: EXAM_STATUS 코드 참조)',
```

공통 코드 테이블(`app_common_code`) 구조:

```sql
PRIMARY KEY (code_group, code)
-- code_group: 'DIFFICULTY', 'EXAM_STATUS' 등
-- code: 'HIGH', 'MID', 'LOW' 등
```
