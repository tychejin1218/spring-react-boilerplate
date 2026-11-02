import { lazy, type ComponentType } from 'react';
import { createBrowserRouter, type RouteObject } from 'react-router-dom';

import Layout from '@/app/layout/Layout';
import RouteGuard from '@/features/auth/components/RouteGuard';
import RouterErrorFallback from '@/shared/components/common/RouterErrorFallback';
import SuspenseBoundary from '@/shared/components/common/SuspenseBoundary';

// 페이지 컴포넌트 lazy loading
const LoginPage = lazy(() => import('@/features/auth/pages/LoginPage'));
const RegisterPage = lazy(() => import('@/features/auth/pages/RegisterPage'));
const EmailVerifyPage = lazy(() => import('@/features/auth/pages/EmailVerifyPage'));
const AuthErrorPage = lazy(() => import('@/features/auth/pages/AuthErrorPage'));
const PasswordResetRequestPage = lazy(() => import('@/features/auth/pages/PasswordResetRequestPage'));
const PasswordResetPage = lazy(() => import('@/features/auth/pages/PasswordResetPage'));
const HomePage = lazy(() => import('@/app/pages/HomePage'));
const NotFoundPage = lazy(() => import('@/app/pages/NotFoundPage'));

// SuspenseBoundary 래핑 헬퍼 (lazy loading + 데이터 페칭 로딩/에러 처리)
const withSuspense = (Component: ComponentType) => (
  <SuspenseBoundary>
    <Component />
  </SuspenseBoundary>
);

// ---------------------------------------------------------------------------
// 예제 기능 라우트
// 예제 기능을 제거할 때는 해당 블록(lazy import + 라우트 배열)과 아래 router 의 스프레드 한 줄을 지운다.
// (README "예제 기능 제거 방법" 참고)
// ---------------------------------------------------------------------------

// [예제: todo] 할 일 관리 (인증 필요, Layout 내부)
const TodoPage = lazy(() => import('@/features/todo/pages/TodoPage'));
const todoRoutes: RouteObject[] = [{ path: '/todos', element: withSuspense(TodoPage) }];

// [예제: admin] 관리자 전용 (ROLE_ADMIN 필요, 별도 RouteGuard + Layout)
const AdminPage = lazy(() => import('@/features/admin/pages/AdminPage'));
const adminRoutes: RouteObject[] = [
  {
    element: <RouteGuard requiredRole="ROLE_ADMIN" />,
    errorElement: <RouterErrorFallback />,
    children: [
      {
        element: <Layout />,
        children: [{ path: '/admin', element: withSuspense(AdminPage) }],
      },
    ],
  },
];

const router = createBrowserRouter([
  // 공개 라우트
  {
    path: '/',
    index: true,
    element: withSuspense(LoginPage),
  },
  {
    path: '/register',
    element: withSuspense(RegisterPage),
  },
  {
    path: '/verify-email',
    element: withSuspense(EmailVerifyPage),
  },
  {
    path: '/password/reset-request',
    element: withSuspense(PasswordResetRequestPage),
  },
  {
    path: '/password/reset',
    element: withSuspense(PasswordResetPage),
  },
  // 인증 실패 페이지 (/error?message= 리다이렉트)
  {
    path: '/error',
    element: withSuspense(AuthErrorPage),
  },
  // 인증 필요
  {
    element: <RouteGuard />,
    errorElement: <RouterErrorFallback />,
    children: [
      {
        element: <Layout />,
        children: [
          {
            path: '/home',
            element: withSuspense(HomePage),
          },
          ...todoRoutes, // [예제: todo]
        ],
      },
    ],
  },
  ...adminRoutes, // [예제: admin]
  {
    path: '*',
    element: withSuspense(NotFoundPage),
  },
]);

export default router;
