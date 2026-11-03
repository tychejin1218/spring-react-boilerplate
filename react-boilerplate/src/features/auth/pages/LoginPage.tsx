import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { Link } from 'react-router-dom';
import { postLogin } from '@/features/auth/apis/authApi';
import AuthCard from '@/features/auth/components/AuthCard';
import useLoginSuccess from '@/features/auth/hooks/useLoginSuccess';
import { loginFormSchema, type LoginFormValues } from '@/features/auth/types/authForm';
import type { ErrorResponse } from '@/shared/types/api';
import { API_STATUS_CODES } from '@/shared/constants/apiStatus';
import useRetryCountdown from '@/shared/hooks/useRetryCountdown';
import Button from '@/shared/components/ui/Button';
import FormField from '@/shared/components/ui/FormField';
import Input from '@/shared/components/ui/Input';

const LoginPage = () => {
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<LoginFormValues>({ resolver: zodResolver(loginFormSchema), defaultValues: { email: '', password: '' } });
  const { retrySeconds, startCountdown } = useRetryCountdown();
  const onLoginSuccess = useLoginSuccess();

  const onSubmit = async (values: LoginFormValues) => {
    try {
      const res = await postLogin(values);
      const { accessToken, refreshToken } = res.data;
      onLoginSuccess(accessToken, refreshToken);
    } catch (err) {
      const apiErr = err as ErrorResponse;
      if (apiErr?.statusCode === API_STATUS_CODES.SYSTEM_RATE_LIMIT_EXCEEDED) {
        startCountdown(60);
      }
      setError('root', { message: apiErr?.message ?? '로그인에 실패했습니다.' });
    }
  };

  return (
    <AuthCard title="로그인">
      {/* noValidate: 브라우저 기본 검증 대신 zod 스키마 메시지를 사용 */}
      <form onSubmit={(e) => void handleSubmit(onSubmit)(e)} noValidate className="flex flex-col gap-4">
        <FormField id="email" label="이메일" error={errors.email?.message}>
          <Input type="email" placeholder="user@example.com" autoComplete="email" required {...register('email')} />
        </FormField>

        <FormField id="password" label="비밀번호" error={errors.password?.message}>
          <Input type="password" placeholder="비밀번호 입력" autoComplete="current-password" required {...register('password')} />
        </FormField>

        {errors.root && (
          <p role="alert" className="text-sm text-red-500">
            {errors.root.message}
          </p>
        )}

        <Button type="submit" loading={isSubmitting} disabled={retrySeconds > 0}>
          {retrySeconds > 0 ? `${retrySeconds}초 후 재시도` : isSubmitting ? '로그인 중...' : '로그인'}
        </Button>
      </form>

      <p className="mt-4 text-center text-sm text-gray-500">
        계정이 없으신가요?{' '}
        <Link to="/register" className="text-blue-600 hover:underline">
          회원가입
        </Link>
      </p>
      <p className="mt-2 text-center text-sm text-gray-500">
        <Link to="/password/reset-request" className="text-blue-600 hover:underline">
          비밀번호를 잊으셨나요?
        </Link>
      </p>
    </AuthCard>
  );
};

export default LoginPage;
