import { StrictMode } from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { http, HttpResponse } from 'msw';
import { env } from '@/app/config/env';
import { server } from '@/mocks/server';
import { err, ok } from '@/mocks/response';
import EmailVerifyPage from '@/features/auth/pages/EmailVerifyPage';

const BASE = env.VITE_API_BASE_URL;

const fakeToken = (email: string) => btoa(JSON.stringify({ alg: 'HS256' })) + '.' + btoa(JSON.stringify({ sub: email, role: 'ROLE_USER' })) + '.signature';

describe('EmailVerifyPage', () => {
  it('StrictMode 에서도 인증 요청을 한 번만 보내고, 성공하면 할 일 화면으로 이동한다', async () => {
    // 실제 BE 처럼 인증 토큰은 한 번만 성공한다. 두 번째 요청은 만료 오류
    let calls = 0;
    server.use(
      http.get(`${BASE}/auth/verify-email`, () => {
        calls += 1;
        if (calls > 1) {
          return HttpResponse.json(err('AUTH_VERIFY_TOKEN_INVALID', '인증 링크가 만료되었거나 유효하지 않습니다.', 'GET', '/auth/verify-email'), {
            status: 400,
          });
        }
        const token = fakeToken('verify@example.com');
        return HttpResponse.json(ok({ accessToken: token, refreshToken: token }));
      }),
    );

    render(
      <StrictMode>
        <MemoryRouter initialEntries={['/verify-email?token=one-time-token']}>
          <Routes>
            <Route path="/verify-email" element={<EmailVerifyPage />} />
            <Route path="/todos" element={<p>할 일 화면</p>} />
          </Routes>
        </MemoryRouter>
      </StrictMode>,
    );

    await waitFor(() => {
      expect(screen.getByText('할 일 화면')).toBeInTheDocument();
    });
    expect(calls).toBe(1);
    expect(screen.queryByText('인증 링크가 만료되었거나 유효하지 않습니다.')).not.toBeInTheDocument();
  });

  it('만료된 토큰이면 오류 문구를 보여 준다', async () => {
    server.use(
      http.get(`${BASE}/auth/verify-email`, () =>
        HttpResponse.json(err('AUTH_VERIFY_TOKEN_INVALID', '인증 링크가 만료되었거나 유효하지 않습니다.', 'GET', '/auth/verify-email'), {
          status: 400,
        }),
      ),
    );

    render(
      <MemoryRouter initialEntries={['/verify-email?token=expired-token']}>
        <Routes>
          <Route path="/verify-email" element={<EmailVerifyPage />} />
        </Routes>
      </MemoryRouter>,
    );

    await waitFor(() => {
      expect(screen.getByText('인증 링크가 만료되었거나 유효하지 않습니다.')).toBeInTheDocument();
    });
  });
});
