import { useState } from 'react';
import { Link } from 'react-router-dom';
import { postPasswordResetRequest } from '@/features/auth/apis/authApi';
import type { ErrorResponse } from '@/shared/types/api';

const PasswordResetRequestPage = () => {
  const [email, setEmail] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      await postPasswordResetRequest(email);
      setSuccess(true);
    } catch (err) {
      const apiErr = err as ErrorResponse;
      setError(apiErr?.message ?? '요청에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex min-h-screen flex-col items-center justify-center bg-gray-50">
      <div className="w-full max-w-sm rounded-lg bg-white p-8 shadow">
        <h1 className="mb-6 text-center text-2xl font-bold text-gray-800">비밀번호 재설정</h1>

        {success ? (
          <div className="text-center">
            <p className="mb-2 text-sm text-gray-600">
              <span className="font-medium">{email}</span>으로 비밀번호 재설정 메일을 발송했습니다.
            </p>
            <p className="mb-6 text-sm text-gray-500">받은편지함에서 링크를 클릭해 비밀번호를 재설정해주세요.</p>
            <Link to="/" className="text-sm text-blue-600 hover:underline">
              로그인 페이지로 돌아가기
            </Link>
          </div>
        ) : (
          <>
            <p className="mb-4 text-center text-sm text-gray-500">가입한 이메일을 입력하면 비밀번호 재설정 링크를 보내드립니다.</p>

            <form onSubmit={(e) => void handleSubmit(e)} className="flex flex-col gap-4">
              <div className="flex flex-col gap-1">
                <label className="text-sm font-medium text-gray-700">이메일</label>
                <input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="user@example.com"
                  required
                  className="rounded border border-gray-300 px-3 py-2 text-sm text-gray-900 placeholder:text-gray-400 focus:border-blue-500 focus:outline-none"
                />
              </div>

              {error && <p className="text-sm text-red-500">{error}</p>}

              <button
                type="submit"
                disabled={loading}
                className="rounded bg-blue-600 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
              >
                {loading ? '발송 중...' : '재설정 메일 발송'}
              </button>
            </form>

            <p className="mt-4 text-center text-sm text-gray-500">
              <Link to="/" className="text-blue-600 hover:underline">
                로그인 페이지로 돌아가기
              </Link>
            </p>
          </>
        )}
      </div>
    </div>
  );
};

export default PasswordResetRequestPage;
