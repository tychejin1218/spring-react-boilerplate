import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { Link } from 'react-router-dom';
import { postPasswordResetRequest } from '@/features/auth/apis/authApi';
import AuthCard from '@/features/auth/components/AuthCard';
import { passwordResetRequestFormSchema, type PasswordResetRequestFormValues } from '@/features/auth/types/authForm';
import { getErrorMessage } from '@/shared/utils/apiError';
import Button from '@/shared/components/ui/Button';
import FormField from '@/shared/components/ui/FormField';
import Input from '@/shared/components/ui/Input';

const PasswordResetRequestPage = () => {
  // 발송 성공한 이메일 (null 이면 아직 입력 단계)
  const [sentEmail, setSentEmail] = useState<string | null>(null);
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<PasswordResetRequestFormValues>({ resolver: zodResolver(passwordResetRequestFormSchema), defaultValues: { email: '' } });

  const onSubmit = async ({ email }: PasswordResetRequestFormValues) => {
    try {
      await postPasswordResetRequest(email);
      setSentEmail(email);
    } catch (err) {
      setError('root', { message: getErrorMessage(err, '요청에 실패했습니다.') });
    }
  };

  return (
    <AuthCard title="비밀번호 재설정">
      {sentEmail !== null ? (
        <div className="text-center">
          <p className="mb-2 text-sm text-gray-600">
            <span className="font-medium">{sentEmail}</span>으로 비밀번호 재설정 메일을 발송했습니다.
          </p>
          <p className="mb-6 text-sm text-gray-500">받은편지함에서 링크를 클릭해 비밀번호를 재설정해주세요.</p>
          <Link to="/" className="text-sm text-blue-600 hover:underline">
            로그인 페이지로 돌아가기
          </Link>
        </div>
      ) : (
        <>
          <p className="mb-4 text-center text-sm text-gray-500">가입한 이메일을 입력하면 비밀번호 재설정 링크를 보내드립니다.</p>

          {/* noValidate: 브라우저 기본 검증 대신 zod 스키마 메시지를 사용 */}
          <form onSubmit={(e) => void handleSubmit(onSubmit)(e)} noValidate className="flex flex-col gap-4">
            <FormField id="email" label="이메일" error={errors.email?.message}>
              <Input type="email" placeholder="user@example.com" autoComplete="email" required {...register('email')} />
            </FormField>

            {errors.root && (
              <p role="alert" className="text-sm text-red-500">
                {errors.root.message}
              </p>
            )}

            <Button type="submit" loading={isSubmitting}>
              {isSubmitting ? '발송 중...' : '재설정 메일 발송'}
            </Button>
          </form>

          <p className="mt-4 text-center text-sm text-gray-500">
            <Link to="/" className="text-blue-600 hover:underline">
              로그인 페이지로 돌아가기
            </Link>
          </p>
        </>
      )}
    </AuthCard>
  );
};

export default PasswordResetRequestPage;
