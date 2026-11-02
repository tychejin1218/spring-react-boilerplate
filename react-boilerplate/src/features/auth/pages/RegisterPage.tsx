import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { Link, useNavigate } from 'react-router-dom';
import { postRegister } from '@/features/auth/apis/authApi';
import AuthCard from '@/features/auth/components/AuthCard';
import { registerFormSchema, type RegisterFormValues } from '@/features/auth/types/authForm';
import type { ErrorResponse } from '@/shared/types/api';
import { API_STATUS_CODES } from '@/shared/constants/apiStatus';
import useRetryCountdown from '@/shared/hooks/useRetryCountdown';
import Button from '@/shared/components/ui/Button';
import FormField from '@/shared/components/ui/FormField';
import Input from '@/shared/components/ui/Input';

const RegisterPage = () => {
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<RegisterFormValues>({ resolver: zodResolver(registerFormSchema), defaultValues: { email: '', name: '', password: '' } });
  const { retrySeconds, startCountdown } = useRetryCountdown();

  const navigate = useNavigate();

  const onSubmit = async (values: RegisterFormValues) => {
    try {
      await postRegister(values);
      void navigate(`/verify-email?email=${encodeURIComponent(values.email)}`);
    } catch (err) {
      const apiErr = err as ErrorResponse;
      if (apiErr?.statusCode === API_STATUS_CODES.RATE_LIMIT_EXCEEDED) {
        startCountdown(60);
      }
      setError('root', { message: apiErr?.message ?? '회원가입에 실패했습니다.' });
    }
  };

  return (
    <AuthCard title="회원가입">
      {/* noValidate: 브라우저 기본 검증 대신 zod 스키마 메시지를 사용 */}
      <form onSubmit={(e) => void handleSubmit(onSubmit)(e)} noValidate className="flex flex-col gap-4">
        <FormField id="email" label="이메일" error={errors.email?.message}>
          <Input type="email" placeholder="user@example.com" autoComplete="email" required {...register('email')} />
        </FormField>

        <FormField id="name" label="이름" error={errors.name?.message}>
          <Input type="text" placeholder="홍길동" autoComplete="name" required {...register('name')} />
        </FormField>

        <FormField id="password" label="비밀번호" error={errors.password?.message}>
          <Input type="password" placeholder="8자 이상 입력" autoComplete="new-password" required {...register('password')} />
        </FormField>

        {errors.root && (
          <p role="alert" className="text-sm text-red-500">
            {errors.root.message}
          </p>
        )}

        <Button type="submit" loading={isSubmitting} disabled={retrySeconds > 0}>
          {retrySeconds > 0 ? `${retrySeconds}초 후 재시도` : isSubmitting ? '가입 중...' : '회원가입'}
        </Button>
      </form>

      <p className="mt-4 text-center text-sm text-gray-500">
        이미 계정이 있으신가요?{' '}
        <Link to="/" className="text-blue-600 hover:underline">
          로그인
        </Link>
      </p>
    </AuthCard>
  );
};

export default RegisterPage;
