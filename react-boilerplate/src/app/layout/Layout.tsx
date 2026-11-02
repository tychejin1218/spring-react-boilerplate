import { Outlet, useNavigate } from 'react-router-dom';
import { useUserStore } from '@/app/stores/useUserStore';
import { postLogout } from '@/features/auth/apis/authApi';
import NotificationSubscriber from '@/features/notification/components/NotificationSubscriber'; // [예제: notification] 알림 feature 제거 시 삭제

const Layout = () => {
  const user = useUserStore((state) => state.user);
  const clearUser = useUserStore((state) => state.clearUser);
  const refreshToken = useUserStore((state) => state.refreshToken);
  const navigate = useNavigate();

  const onClickLogout = async () => {
    if (refreshToken) {
      try {
        await postLogout(refreshToken);
      } catch {
        // 로그아웃 API 실패 시에도 클라이언트 상태는 초기화
      }
    }
    clearUser();
    navigate('/');
  };

  return (
    <div className="min-h-screen">
      <NotificationSubscriber /> {/* [예제: notification] 알림 feature 제거 시 삭제 */}
      <header className="border-b border-gray-200 bg-white">
        <nav className="flex items-center justify-between px-6 py-3">
          <h1 className="text-xl font-bold">React Boilerplate</h1>
          {user && (
            <div className="flex items-center gap-4">
              <span className="text-sm text-gray-600">{user.name}</span>
              <button className="rounded bg-gray-200 px-3 py-1 text-sm text-gray-700 hover:bg-gray-300" onClick={onClickLogout}>
                로그아웃
              </button>
            </div>
          )}
        </nav>
      </header>
      <main className="p-4">
        <Outlet />
      </main>
    </div>
  );
};

export default Layout;
