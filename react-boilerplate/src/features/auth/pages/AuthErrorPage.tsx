import { Link } from 'react-router-dom';
import AuthCard from '@/features/auth/components/AuthCard';

const AuthErrorPage = () => {
  const params = new URLSearchParams(window.location.search);
  const message = params.get('message') ?? '알 수 없는 오류가 발생했습니다.';

  return (
    <AuthCard centered>
      <p className="mb-2 text-xl font-bold text-red-600">인증 실패</p>
      <p className="mb-6 text-sm text-gray-600">{decodeURIComponent(message)}</p>
      <Link to="/" className="text-sm text-blue-600 hover:underline">
        로그인 페이지로 돌아가기
      </Link>
    </AuthCard>
  );
};

export default AuthErrorPage;
