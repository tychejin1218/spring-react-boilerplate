import { z } from 'zod';

/**
 * 인증 폼 검증 스키마 (react-hook-form + zodResolver)
 * - 스키마 하나로 폼 값 타입(z.infer)과 검증 규칙을 함께 정의한다
 */

const MIN_PASSWORD_LENGTH = 8;
const PASSWORD_LENGTH_MESSAGE = '비밀번호는 8자 이상이어야 합니다.';

const emailField = z.string().min(1, '이메일을 입력해주세요.').pipe(z.email('올바른 이메일 형식이 아닙니다.'));

export const loginFormSchema = z.object({
  email: emailField,
  password: z.string().min(1, '비밀번호를 입력해주세요.'),
});
export type LoginFormValues = z.infer<typeof loginFormSchema>;

export const registerFormSchema = z.object({
  email: emailField,
  name: z.string().min(1, '이름을 입력해주세요.'),
  password: z.string().min(MIN_PASSWORD_LENGTH, PASSWORD_LENGTH_MESSAGE),
});
export type RegisterFormValues = z.infer<typeof registerFormSchema>;

export const passwordResetRequestFormSchema = z.object({
  email: emailField,
});
export type PasswordResetRequestFormValues = z.infer<typeof passwordResetRequestFormSchema>;

export const passwordResetFormSchema = z
  .object({
    newPassword: z.string().min(MIN_PASSWORD_LENGTH, PASSWORD_LENGTH_MESSAGE),
    confirmPassword: z.string().min(1, '비밀번호를 다시 입력해주세요.'),
  })
  .refine((values) => values.newPassword === values.confirmPassword, {
    message: '비밀번호가 일치하지 않습니다.',
    path: ['confirmPassword'],
  });
export type PasswordResetFormValues = z.infer<typeof passwordResetFormSchema>;
