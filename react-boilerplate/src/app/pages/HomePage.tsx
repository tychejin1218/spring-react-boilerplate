import { Link } from 'react-router-dom';
import { useUserStore } from '@/app/stores/useUserStore';

/**
 * 로그인 후 진입 페이지
 * - 환경 정보나 API 주소는 노출하지 않는다 (필요하면 개발자 도구에서 env 객체를 확인)
 */
const HomePage = () => {
  const user = useUserStore((state) => state.user);

  return (
    <div className="mx-auto max-w-lg py-6">
      <h2 className="mb-2 text-xl font-bold text-gray-800">환영합니다{user ? `, ${user.name}님` : ''}</h2>
      <p className="mb-6 text-sm text-gray-600">React Boilerplate 의 시작 페이지입니다. 이 페이지를 서비스의 메인 화면으로 교체하세요.</p>
      <ul className="flex flex-col gap-2 text-sm">
        <li>
          <Link to="/todos" className="text-blue-600 hover:underline">
            할 일 목록 예제 보기
          </Link>
        </li>
        {user?.role === 'ROLE_ADMIN' && (
          <li>
            <Link to="/admin" className="text-blue-600 hover:underline">
              관리자 페이지 예제 보기
            </Link>
          </li>
        )}
      </ul>
    </div>
  );
};

export default HomePage;
