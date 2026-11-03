import { useNavigate } from 'react-router-dom';
import { useUserStore } from '@/app/stores/useUserStore';
import type { MemberRole } from '@/features/auth/types/user';
import { resetSessionExpiredGuard } from '@/shared/utils/sessionExpired';
import { parseEmailFromToken, parseRoleFromToken } from '@/shared/utils/token';

/**
 * 로그인/인증 성공 후 토큰을 파싱하여 스토어에 저장하고 /todos로 이동합니다.
 * 새 세션이 시작되므로 세션 만료 가드를 초기화해 다음 만료도 다시 안내되게 합니다.
 */
const useLoginSuccess = () => {
  const setUser = useUserStore((state) => state.setUser);
  const setAccessToken = useUserStore((state) => state.setAccessToken);
  const setRefreshToken = useUserStore((state) => state.setRefreshToken);
  const navigate = useNavigate();

  return (accessToken: string, refreshToken?: string) => {
    const email = parseEmailFromToken(accessToken) ?? '';
    const role = parseRoleFromToken(accessToken) as MemberRole;

    setUser({ userId: email, name: email, email, role });
    setAccessToken(accessToken);
    if (refreshToken) setRefreshToken(refreshToken);
    resetSessionExpiredGuard();
    void navigate('/todos');
  };
};

export default useLoginSuccess;
