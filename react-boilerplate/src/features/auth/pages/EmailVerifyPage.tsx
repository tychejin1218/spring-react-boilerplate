import { useEffect, useRef, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { getVerifyEmail, postResendVerification } from '@/features/auth/apis/authApi';
import AuthCard from '@/features/auth/components/AuthCard';
import useLoginSuccess from '@/features/auth/hooks/useLoginSuccess';
import Button from '@/shared/components/ui/Button';
import { getErrorMessage } from '@/shared/utils/apiError';

const EmailVerifyPage = () => {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token');
  const email = searchParams.get('email') ?? '';

  const [resendLoading, setResendLoading] = useState(false);
  const [resendMessage, setResendMessage] = useState('');
  const [error, setError] = useState('');

  const onLoginSuccess = useLoginSuccess();

  // 이미 인증 요청을 보낸 토큰. 인증 토큰은 1회용이라 같은 토큰으로 두 번 요청하면 두 번째가 실패한다.
  // 개발 모드의 StrictMode 는 effect 를 두 번 실행하므로, 컴포넌트가 다시 마운트돼도 유지되는 ref 로 막는다.
  const requestedTokenRef = useRef<string | null>(null);

  // 토큰이 있으면 자동으로 인증 처리
  useEffect(() => {
    if (!token || requestedTokenRef.current === token) return;
    requestedTokenRef.current = token;

    // 응답은 effect 정리와 관계없이 반영한다. 정리 시점에 응답을 버리면 성공한 인증이 화면에 반영되지 않는다
    getVerifyEmail(token)
      .then((res) => {
        const { accessToken, refreshToken } = res.data;
        onLoginSuccess(accessToken, refreshToken);
      })
      .catch((err: unknown) => {
        setError(getErrorMessage(err, '인증 링크가 유효하지 않거나 만료되었습니다.'));
      });
  }, [token]); // eslint-disable-line react-hooks/exhaustive-deps

  const handleResend = async () => {
    if (!email) return;
    setResendLoading(true);
    setResendMessage('');
    setError('');

    try {
      await postResendVerification(email);
      setResendMessage('인증 메일을 재발송했습니다. 받은편지함을 확인해주세요.');
    } catch (err) {
      setError(getErrorMessage(err, '재발송에 실패했습니다.'));
    } finally {
      setResendLoading(false);
    }
  };

  return (
    <AuthCard centered>
      {token ? (
        <>
          {error ? (
            <>
              <p className="mb-4 text-red-500">{error}</p>
              <Link to="/" className="text-sm text-blue-600 hover:underline">
                로그인 페이지로 돌아가기
              </Link>
            </>
          ) : (
            <p className="text-gray-600">이메일 인증 중...</p>
          )}
        </>
      ) : (
        <>
          <h1 className="mb-2 text-2xl font-bold text-gray-800">이메일 인증</h1>
          <p className="mb-1 text-sm text-gray-600">
            <span className="font-medium">{email}</span>으로 인증 메일을 발송했습니다.
          </p>
          <p className="mb-6 text-sm text-gray-500">받은편지함에서 링크를 클릭해 인증을 완료해주세요.</p>

          {resendMessage && <p className="mb-4 text-sm text-green-600">{resendMessage}</p>}
          {error && <p className="mb-4 text-sm text-red-500">{error}</p>}

          <Button onClick={() => void handleResend()} loading={resendLoading} disabled={!email} className="w-full">
            {resendLoading ? '발송 중...' : '인증 메일 재발송'}
          </Button>

          <p className="mt-4 text-sm text-gray-500">
            <Link to="/" className="text-blue-600 hover:underline">
              로그인 페이지로 돌아가기
            </Link>
          </p>
        </>
      )}
    </AuthCard>
  );
};

export default EmailVerifyPage;
