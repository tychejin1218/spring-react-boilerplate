import type { ReactNode } from 'react';

interface Props {
  /** 카드 상단 제목 (h1). 없으면 생략 */
  title?: string;
  /** true 면 카드 내용을 가운데 정렬한다 (안내/결과 화면용) */
  centered?: boolean;
  children: ReactNode;
}

/**
 * 인증 화면 공통 카드 래퍼
 * - 로그인, 회원가입, 이메일 인증, 비밀번호 재설정, 인증 실패 페이지가 공유하는 화면 중앙 카드 레이아웃
 */
const AuthCard = ({ title, centered = false, children }: Props) => (
  <div className="flex min-h-screen flex-col items-center justify-center bg-gray-50 px-4">
    <div className={`w-full max-w-sm rounded-lg bg-white p-8 shadow ${centered ? 'text-center' : ''}`}>
      {title && <h1 className="mb-6 text-center text-2xl font-bold text-gray-800">{title}</h1>}
      {children}
    </div>
  </div>
);

export default AuthCard;
