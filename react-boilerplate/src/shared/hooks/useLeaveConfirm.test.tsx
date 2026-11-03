import { act, fireEvent, render, screen } from '@testing-library/react';
import { createMemoryRouter, RouterProvider, useNavigate } from 'react-router-dom';
import { useDialogStore } from '@/app/stores/useDialogStore';
import { CONFIRM_MESSAGES } from '@/shared/constants/messages';
import { useLeaveConfirm } from '@/shared/hooks/useLeaveConfirm';
import { handleSessionExpired, registerSessionExpiredNavigator, resetSessionExpiredGuard } from '@/shared/utils/sessionExpired';

/** 열린 확인 다이얼로그에 응답한다 (Dialog 컴포넌트 대신 store 로 resolve) */
const answerConfirm = async (ok: boolean) => {
  const dialog = useDialogStore.getState().dialogs.at(-1);
  if (!dialog) throw new Error('열린 확인 다이얼로그가 없습니다');
  await act(async () => useDialogStore.getState().close(dialog.id, ok));
};

const dialogCount = () => useDialogStore.getState().dialogs.length;

const renderForm = (isDirty: boolean) => {
  const FormScreen = () => {
    const navigate = useNavigate();
    const { leaveWithoutConfirm } = useLeaveConfirm(isDirty);
    return (
      <>
        <button onClick={() => void navigate('/next')}>이동</button>
        <button onClick={() => void navigate('/form?tab=2')}>탭 변경</button>
        <button onClick={() => leaveWithoutConfirm(() => void navigate('/next'))}>저장 후 이동</button>
      </>
    );
  };

  const router = createMemoryRouter(
    [
      { path: '/form', element: <FormScreen /> },
      { path: '/next', element: <div>다음 화면</div> },
    ],
    { initialEntries: ['/form'] },
  );
  render(<RouterProvider router={router} />);
  return { pathname: () => router.state.location.pathname };
};

describe('useLeaveConfirm', () => {
  beforeEach(() => {
    resetSessionExpiredGuard();
    useDialogStore.setState({ dialogs: [] });
  });

  it('작성 중이 아니면 확인 없이 이동한다', async () => {
    const { pathname } = renderForm(false);

    fireEvent.click(screen.getByRole('button', { name: '이동' }));

    expect(await screen.findByText('다음 화면')).toBeInTheDocument();
    expect(dialogCount()).toBe(0);
    expect(pathname()).toBe('/next');
  });

  it('작성 중이면 확인 다이얼로그를 띄우고, 취소하면 현재 화면에 남는다', async () => {
    const { pathname } = renderForm(true);

    fireEvent.click(screen.getByRole('button', { name: '이동' }));
    expect(useDialogStore.getState().dialogs[0]).toMatchObject({ type: 'confirm', content: CONFIRM_MESSAGES.LEAVE_UNSAVED });

    await answerConfirm(false);

    expect(pathname()).toBe('/form');
    expect(screen.getByRole('button', { name: '이동' })).toBeInTheDocument();
  });

  it('작성 중이어도 확인하면 이동한다', async () => {
    const { pathname } = renderForm(true);

    fireEvent.click(screen.getByRole('button', { name: '이동' }));
    await answerConfirm(true);

    expect(await screen.findByText('다음 화면')).toBeInTheDocument();
    expect(pathname()).toBe('/next');
  });

  it('같은 경로의 쿼리스트링 변경은 확인하지 않는다', () => {
    renderForm(true);

    fireEvent.click(screen.getByRole('button', { name: '탭 변경' }));

    expect(dialogCount()).toBe(0);
  });

  it('leaveWithoutConfirm 으로 감싼 이동은 확인 없이 통과한다', async () => {
    const { pathname } = renderForm(true);

    fireEvent.click(screen.getByRole('button', { name: '저장 후 이동' }));

    expect(await screen.findByText('다음 화면')).toBeInTheDocument();
    expect(dialogCount()).toBe(0);
    expect(pathname()).toBe('/next');
  });

  it('세션 만료 후의 이동은 막지 않는다', async () => {
    const unregister = registerSessionExpiredNavigator(() => {});
    const { pathname } = renderForm(true);
    act(() => {
      handleSessionExpired();
    });

    fireEvent.click(screen.getByRole('button', { name: '이동' }));

    expect(await screen.findByText('다음 화면')).toBeInTheDocument();
    expect(dialogCount()).toBe(0);
    expect(pathname()).toBe('/next');
    unregister();
  });

  it('작성 중이면 새로고침/탭 닫기(beforeunload)를 막는다', () => {
    renderForm(true);
    const dirtyEvent = new Event('beforeunload', { cancelable: true });
    window.dispatchEvent(dirtyEvent);
    expect(dirtyEvent.defaultPrevented).toBe(true);
  });

  it('작성 중이 아니면 beforeunload 를 막지 않는다', () => {
    renderForm(false);
    const event = new Event('beforeunload', { cancelable: true });
    window.dispatchEvent(event);
    expect(event.defaultPrevented).toBe(false);
  });
});
