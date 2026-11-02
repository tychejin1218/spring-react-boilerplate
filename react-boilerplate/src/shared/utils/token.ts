type JwtPayload = { sub?: string; role?: string };

const decodePayload = (token: string): JwtPayload => {
  try {
    return JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/'))) as JwtPayload;
  } catch {
    return {};
  }
};

/** JWT payload에서 sub(email) 추출 */
export const parseEmailFromToken = (token: string): string => decodePayload(token).sub ?? '';

/** JWT payload에서 role 추출 */
export const parseRoleFromToken = (token: string): string => decodePayload(token).role ?? 'ROLE_USER';
