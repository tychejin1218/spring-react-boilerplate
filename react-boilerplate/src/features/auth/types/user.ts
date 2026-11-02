import { z } from 'zod';

/**
 * 사용자 역할
 * - 스키마는 BE 응답(Member.role) 검증과 타입 추론에 함께 쓴다.
 * - RouteGuard 의 requiredRole 타입(MemberRole)이 여기서 파생된다.
 */
export const memberRoleSchema = z.enum(['ROLE_USER', 'ROLE_ADMIN']);
export type MemberRole = z.infer<typeof memberRoleSchema>;

/**
 * 사용자
 * - 현재는 JWT payload 를 decode 한 값을 useUserStore 에 담을 때 User 타입으로만 쓰이지만,
 *   /me 같은 사용자 조회 API 를 붙일 때 응답 검증(userSchema.parse)에 그대로 재사용할 수 있도록 Zod 스키마로 유지한다.
 */
export const userSchema = z.object({
  userId: z.string(),
  name: z.string(),
  email: z.string(),
  role: memberRoleSchema,
});

export type User = z.infer<typeof userSchema>;
