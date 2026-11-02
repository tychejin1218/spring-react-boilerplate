# React 보일러플레이트 태스크

---

## Phase 1 — 기반 다지기

### 1. Soft Delete
- [x] 삭제 후 UI 동작 확인 (변경 없어야 함)

### 2. 페이지네이션 + 필터링
- [x] Todo 목록에 페이지 UI (이전/다음 버튼)
- [x] 완료/미완료 필터 탭 추가

### 3. 테스트 작성
- [x] `useTodos` 훅 Vitest 테스트 (MSW 활용)
- [x] Playwright E2E — 로그인 → Todo 생성 → 완료 → 삭제

---

## Phase 2 — 인증 강화

### 4. RBAC (역할 기반 접근 제어)
- [x] 역할별 라우트 보호 (`AdminRoute` 컴포넌트)
- [x] 관리자 페이지 기본 틀 추가

### 5. Rate Limiting
- [x] 429 응답 처리 + 재시도 대기 UI

---

## Phase 3 — 기능 확장

### 6. 파일 업로드
- [x] Todo에 이미지 첨부 기능 (버튼 클릭 → 파일 선택 → 업로드)

### 7. 실시간 알림 (SSE)
- [x] `useNotifications` 훅 — fetch 기반 SSE 수신
- [x] 완료 시 Toast 알림 표시

---

## Phase 4 — 운영 준비

### 8. Refresh Token Rotation
- [x] BE 응답 형식 맞게 갱신 로직 확인

### 9. 이메일 인증
- [x] 인증 메일 안내 페이지 및 재발송 버튼

### 10. 앱 컨테이너화
- [x] `Dockerfile` 작성 (nginx 기반)

### 11. 모니터링 / 12. 캐시 전략
- BE 전담 항목, FE 작업 없음

### 13. 비밀번호 재설정
- [x] 비밀번호 찾기 / 재설정 페이지
