-- =============================================================================
-- 초기 테이블 생성 스크립트
-- Docker 컨테이너 최초 실행 시 자동으로 실행됩니다.
-- 실행 시 기존 todo, member 테이블을 DROP 한 뒤 다시 생성하므로 기존 데이터는 모두 삭제됩니다.
-- (MySQL 이미지의 docker-entrypoint-initdb.d 는 볼륨이 비어 있을 때 한 번만 실행됩니다)
-- =============================================================================

USE boilerplate_db;

-- 이 스크립트를 실행하는 mysql 클라이언트의 기본 문자셋은 latin1 이므로
-- 명시하지 않으면 한글 시드 데이터가 이중 인코딩되어 깨진다
SET NAMES utf8mb4;

-- =============================================================================
-- 기존 테이블 삭제 (FK 의존 순서 역순으로 삭제)
-- =============================================================================
DROP TABLE IF EXISTS todo;
DROP TABLE IF EXISTS member;

-- -----------------------------------------------------------------------------
-- member : 회원 정보
--   - email 은 로그인 ID 로 사용되므로 UNIQUE 제약
--   - password 는 BCrypt 인코딩된 값 저장 (평문 저장 금지)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS member (
  id         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '회원 PK',
  email      VARCHAR(200) NOT NULL                COMMENT '이메일 (로그인 ID)',
  name       VARCHAR(100) NOT NULL                COMMENT '이름',
  password   VARCHAR(200) NOT NULL                COMMENT 'BCrypt 인코딩된 비밀번호',
  role           VARCHAR(20)  NOT NULL DEFAULT 'ROLE_USER' COMMENT '역할 (ROLE_USER, ROLE_ADMIN)',
  email_verified TINYINT(1)   NOT NULL DEFAULT 0           COMMENT '이메일 인증 여부 (0: 미인증, 1: 인증)',
  created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP                   COMMENT '생성일시',
  updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
  deleted_at DATETIME              DEFAULT NULL                                COMMENT '삭제일시 (NULL: 정상, NOT NULL: 논리 삭제)',
  PRIMARY KEY (id),
  UNIQUE KEY uq_member_email (email)            -- 이메일 중복 가입 방지
) COMMENT = '회원';

-- -----------------------------------------------------------------------------
-- todo : 할 일 목록
--   - member_id 로 소유자를 식별하며, 회원 삭제 시 연쇄 삭제(CASCADE)
--   - completed : 0 = 미완료, 1 = 완료
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS todo (
  id         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '할 일 PK',
  title      VARCHAR(200) NOT NULL                COMMENT '할 일 내용',
  completed  TINYINT(1)   NOT NULL DEFAULT 0      COMMENT '완료 여부 (0: 미완료, 1: 완료)',
  image_url  VARCHAR(500)          DEFAULT NULL   COMMENT '첨부 이미지 URL',
  member_id  BIGINT       NOT NULL                COMMENT '소유 회원 FK',
  created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP                   COMMENT '생성일시',
  updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
  deleted_at DATETIME              DEFAULT NULL                                COMMENT '삭제일시 (NULL: 정상, NOT NULL: 논리 삭제)',
  PRIMARY KEY (id),
  KEY        ix_todo_member_id (member_id),       -- 회원별 목록 조회 인덱스
  CONSTRAINT fk_todo_member
    FOREIGN KEY (member_id) REFERENCES member (id)
    ON DELETE CASCADE                             -- 회원 삭제 시 할 일도 함께 삭제
) COMMENT = '할 일';

-- =============================================================================
-- 샘플 데이터 (개발 환경 전용)
-- 비밀번호 원문: Test1234!
-- =============================================================================

INSERT INTO member (email, name, password, role, email_verified) VALUES
  ('user1@example.com', '홍길동', '$2a$10$zQV5g9qExngCv2qE9/c5feupavK2VKb.76uITB1LV4RPhG62zoFbu', 'ROLE_USER', 1),
  ('user2@example.com', '김철수', '$2a$10$zQV5g9qExngCv2qE9/c5feupavK2VKb.76uITB1LV4RPhG62zoFbu', 'ROLE_USER', 1),
  ('admin@example.com', '관리자', '$2a$10$zQV5g9qExngCv2qE9/c5feupavK2VKb.76uITB1LV4RPhG62zoFbu', 'ROLE_ADMIN', 1),
  ('deleted@example.com', '탈퇴회원', '$2a$10$zQV5g9qExngCv2qE9/c5feupavK2VKb.76uITB1LV4RPhG62zoFbu', 'ROLE_USER', 1);

-- 논리 삭제된 회원 처리
UPDATE member SET deleted_at = '2026-01-01 00:00:00' WHERE email = 'deleted@example.com';

INSERT INTO todo (title, completed, member_id) VALUES
  ('Spring Boot 프로젝트 세팅',   1, 1),
  ('JWT 인증 구현',               1, 1),
  ('Soft Delete 적용',            0, 1),
  ('페이지네이션 구현',            0, 1),
  ('React 프로젝트 세팅',         1, 2),
  ('로그인 페이지 구현',           1, 2),
  ('Todo 목록 UI 구현',           0, 2);
