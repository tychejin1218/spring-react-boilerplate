import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { Link, useSearchParams } from 'react-router-dom';
import { postPasswordReset } from '@/features/auth/apis/authApi';
import AuthCard from '@/features/auth/components/AuthCard';
import { passwordResetFormSchema, type PasswordResetFormValues } from '@/features/auth/types/authForm';
import type { ErrorResponse } from '@/shared/types/api';
import Button from '@/shared/components/ui/Button';
import FormField from '@/shared/components/ui/FormField';
import Input from '@/shared/components/ui/Input';

const PasswordResetPage = () => {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token') ?? '';

  const [success, setSuccess] = useState(false);
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<PasswordResetFormValues>({ resolver: zodResolver(passwordResetFormSchema), defaultValues: { newPassword: '', confirmPassword: '' } });

  const onSubmit = async ({ newPassword }: PasswordResetFormValues) => {
    try {
      await postPasswordReset({ token, newPassword });
      setSuccess(true);
    } catch (err) {
      const apiErr = err as ErrorResponse;
      setError('root', { message: apiErr?.message ?? '비밀번호 재설정에 실패했습니다.' });
    }
  };

  if (!token) {
    return (
      <AuthCard centered>
        <p className="mb-4 text-red-500">유효하지 않은 링크입니다.</p>
        <Link to="/" className="text-sm text-blue-600 hover:underline">
          로그인 페이지로 돌아가기
        </Link>
      </AuthCard>
    );
  }

  return (
    <AuthCard title="새 비밀번호 설정">
      {success ? (
        <div className="text-center">
          <p className="mb-4 text-sm text-green-600">비밀번호가 성공적으로 변경되었습니다.</p>
          <Link to="/" className="inline-block w-full rounded bg-blue-600 py-2 text-center text-sm font-medium text-white hover:bg-blue-700">
            로그인하기
          </Link>
        </div>
      ) : (
        // noValidate: 브라우저 기본 검증 대신 zod 스키마 메시지를 사용
        <form onSubmit={(e) => void handleSubmit(onSubmit)(e)} noValidate className="flex flex-col gap-4">
          <FormField id="newPassword" label="새 비밀번호" error={errors.newPassword?.message}>
            <Input type="password" placeholder="8자 이상 입력" autoComplete="new-password" required {...register('newPassword')} />
          </FormField>

          <FormField id="confirmPassword" label="비밀번호 확인" error={errors.confirmPassword?.message}>
            <Input type="password" placeholder="비밀번호 다시 입력" autoComplete="new-password" required {...register('confirmPassword')} />
          </FormField>

          {errors.root && (
            <p role="alert" className="text-sm text-red-500">
              {errors.root.message}
            </p>
          )}

          <Button type="submit" loading={isSubmitting}>
            {isSubmitting ? '변경 중...' : '비밀번호 변경'}
          </Button>
        </form>
      )}
    </AuthCard>
  );
};

export default PasswordResetPage;
