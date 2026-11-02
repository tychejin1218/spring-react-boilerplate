import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { http } from 'msw';
import { env } from '@/app/config/env';
import { server } from '@/mocks/server';
import PasswordResetRequestPage from '@/features/auth/pages/PasswordResetRequestPage';
import PasswordResetPage from '@/features/auth/pages/PasswordResetPage';

const BASE = env.VITE_API_BASE_URL;

const renderWithRouter = (ui: React.ReactElement, { initialEntries = ['/'] } = {}) => render(<MemoryRouter initialEntries={initialEntries}>{ui}</MemoryRouter>);

// ---------------------------------------------------------------------------
// PasswordResetRequestPage
// ---------------------------------------------------------------------------
describe('PasswordResetRequestPage', () => {
  it('이메일 입력 폼이 렌더링된다', () => {
    renderWithRouter(<PasswordResetRequestPage />);

    expect(screen.getByRole('heading', { name: '비밀번호 재설정' })).toBeInTheDocument();
    expect(screen.getByRole('textbox')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '재설정 메일 발송' })).toBeInTheDocument();
  });

  it('이메일 입력 후 제출 시 API가 호출된다', async () => {
    const user = userEvent.setup();
    renderWithRouter(<PasswordResetRequestPage />);

    await user.type(screen.getByRole('textbox'), 'test@example.com');
    await user.click(screen.getByRole('button', { name: '재설정 메일 발송' }));

    await waitFor(() => {
      expect(screen.getByText(/비밀번호 재설정 메일을 발송했습니다/)).toBeInTheDocument();
    });
  });

  it('성공 시 안내 메시지가 표시된다', async () => {
    const user = userEvent.setup();
    renderWithRouter(<PasswordResetRequestPage />);

    await user.type(screen.getByRole('textbox'), 'test@example.com');
    await user.click(screen.getByRole('button', { name: '재설정 메일 발송' }));

    await waitFor(() => {
      expect(screen.getByText(/test@example.com/)).toBeInTheDocument();
      expect(screen.getByText(/받은편지함에서 링크를 클릭해/)).toBeInTheDocument();
    });
  });

  it('존재하지 않는 이메일로 제출 시 에러 메시지가 표시된다', async () => {
    const user = userEvent.setup();
    renderWithRouter(<PasswordResetRequestPage />);

    await user.type(screen.getByRole('textbox'), 'unknown@example.com');
    await user.click(screen.getByRole('button', { name: '재설정 메일 발송' }));

    await waitFor(() => {
      expect(screen.getByText('존재하지 않는 이메일입니다.')).toBeInTheDocument();
    });
  });

  it('빈 이메일로 제출 시 유효성 검사가 동작한다', () => {
    renderWithRouter(<PasswordResetRequestPage />);

    const emailInput = screen.getByRole('textbox');
    expect(emailInput).toBeRequired();
  });

  it('로딩 중 버튼이 비활성화된다', async () => {
    server.use(
      http.post(`${BASE}/auth/password/reset-request`, () => {
        return new Promise<never>(() => {
          // 응답을 보내지 않아 로딩 상태를 유지
        });
      }),
    );

    const user = userEvent.setup();
    renderWithRouter(<PasswordResetRequestPage />);

    await user.type(screen.getByRole('textbox'), 'test@example.com');

    // fireEvent로 제출하여 비동기 대기 없이 로딩 상태를 확인
    fireEvent.click(screen.getByRole('button', { name: '재설정 메일 발송' }));

    await waitFor(() => {
      expect(screen.getByRole('button', { name: '발송 중...' })).toBeDisabled();
    });
  });
});

// ---------------------------------------------------------------------------
// PasswordResetPage
// ---------------------------------------------------------------------------
describe('PasswordResetPage', () => {
  const renderResetPage = (search = '?token=valid-token') =>
    renderWithRouter(
      <Routes>
        <Route path="/password/reset" element={<PasswordResetPage />} />
        <Route path="/" element={<div>로그인 페이지</div>} />
      </Routes>,
      { initialEntries: [`/password/reset${search}`] },
    );

  it('URL 쿼리파라미터에서 token을 읽는다', () => {
    renderResetPage('?token=my-reset-token');

    expect(screen.getByRole('heading', { name: '새 비밀번호 설정' })).toBeInTheDocument();
  });

  it('새 비밀번호 + 확인 입력 폼이 렌더링된다', () => {
    renderResetPage();

    expect(screen.getByPlaceholderText('8자 이상 입력')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('비밀번호 다시 입력')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '비밀번호 변경' })).toBeInTheDocument();
  });

  it('비밀번호 불일치 시 에러가 표시된다', async () => {
    const user = userEvent.setup();
    renderResetPage();

    await user.type(screen.getByPlaceholderText('8자 이상 입력'), 'newpassword1');
    await user.type(screen.getByPlaceholderText('비밀번호 다시 입력'), 'different99');
    await user.click(screen.getByRole('button', { name: '비밀번호 변경' }));

    expect(screen.getByText('비밀번호가 일치하지 않습니다.')).toBeInTheDocument();
  });

  it('성공 시 로그인 페이지로 이동할 수 있는 링크가 표시된다', async () => {
    const user = userEvent.setup();
    renderResetPage();

    await user.type(screen.getByPlaceholderText('8자 이상 입력'), 'newpassword1');
    await user.type(screen.getByPlaceholderText('비밀번호 다시 입력'), 'newpassword1');
    await user.click(screen.getByRole('button', { name: '비밀번호 변경' }));

    await waitFor(() => {
      expect(screen.getByText('비밀번호가 성공적으로 변경되었습니다.')).toBeInTheDocument();
    });

    const loginLink = screen.getByRole('link', { name: '로그인하기' });
    expect(loginLink).toHaveAttribute('href', '/');
  });

  it('token이 없을 때 에러 처리가 된다', () => {
    renderResetPage('');

    expect(screen.getByText('유효하지 않은 링크입니다.')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: '로그인 페이지로 돌아가기' })).toBeInTheDocument();
  });
});
