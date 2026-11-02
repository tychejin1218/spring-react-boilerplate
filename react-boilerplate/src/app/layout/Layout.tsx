import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useUserStore } from '@/app/stores/useUserStore';
import { postLogout } from '@/features/auth/apis/authApi';
import NotificationSubscriber from '@/features/notification/components/NotificationSubscriber'; // [예제: notification] 알림 feature 제거 시 삭제
import Button from '@/shared/components/ui/Button';

// 상단 바 메뉴. adminOnly 항목은 ROLE_ADMIN 사용자에게만 노출 (실제 접근 제어는 RouteGuard 가 담당)
const NAV_ITEMS: { to: string; label: string; adminOnly?: boolean }[] = [
  { to: '/todos', label: '할 일' }, // [예제: todo]
  { to: '/admin', label: '관리자', adminOnly: true }, // [예제: admin]
];

const navLinkClass = ({ isActive }: { isActive: boolean }) =>
  `rounded px-3 py-1 text-sm font-medium transition-colors ${isActive ? 'bg-blue-50 text-blue-600' : 'text-gray-600 hover:bg-gray-100 hover:text-gray-800'}`;

/**
 * 인증 영역 공통 레이아웃
 * - 상단 바: 앱 이름(홈 링크), 메뉴, 로그인 이메일, 로그아웃 버튼
 */
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
    void navigate('/');
  };

  const navItems = NAV_ITEMS.filter((item) => !item.adminOnly || user?.role === 'ROLE_ADMIN');

  return (
    <div className="min-h-screen">
      <NotificationSubscriber /> {/* [예제: notification] 알림 feature 제거 시 삭제 */}
      <header className="border-b border-gray-200 bg-white">
        <div className="flex flex-wrap items-center justify-between gap-3 px-6 py-3">
          <div className="flex items-center gap-6">
            <Link to="/home" className="text-xl font-bold text-gray-900">
              React Boilerplate
            </Link>
            <nav aria-label="주 메뉴" className="flex items-center gap-1">
              {navItems.map((item) => (
                <NavLink key={item.to} to={item.to} className={navLinkClass}>
                  {item.label}
                </NavLink>
              ))}
            </nav>
          </div>
          {user && (
            <div className="flex items-center gap-4">
              <span className="text-sm text-gray-600">{user.email}</span>
              <Button variant="secondary" size="sm" onClick={() => void onClickLogout()}>
                로그아웃
              </Button>
            </div>
          )}
        </div>
      </header>
      <main className="p-4">
        <Outlet />
      </main>
    </div>
  );
};

export default Layout;
