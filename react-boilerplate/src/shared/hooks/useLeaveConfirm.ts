import { useCallback, useEffect, useRef } from 'react';
import { useBlocker } from 'react-router-dom';
import { CONFIRM_MESSAGES } from '@/shared/constants/messages';
import { useDialog } from '@/shared/hooks/useDialog';
import { isSessionExpiredHandled } from '@/shared/utils/sessionExpired';

/**
 * 작성 중인 내용이 있을 때 페이지 이탈을 확인받는 훅
 *
 * - 앱 안 이동(Link, navigate, 브라우저 뒤로가기): react-router useBlocker 로 막고 useDialog.confirm 으로 묻는다.
 *   [확인] 이면 이동, [취소] 면 현재 화면에 남는다. 경로(pathname)가 바뀔 때만 묻고, 같은 페이지의 쿼리스트링 변경은 통과시킨다.
 * - 새로고침, 탭 닫기, 외부 주소 이동: beforeunload 로 브라우저 기본 확인 창을 띄운다 (문구는 브라우저가 정해 바꿀 수 없다).
 * - 세션 만료로 로그인 페이지로 보내는 이동은 막지 않는다. 막으면 세션이 끝났는데 화면만 남아 이후 요청이 모두 실패한다.
 *
 * useBlocker 는 data router(createBrowserRouter / createMemoryRouter) 안에서만 동작한다.
 * 한 화면에 blocker 는 하나만 둘 수 있으므로 페이지 컴포넌트에서 한 번만 호출한다.
 *
 * @param isDirty 작성 중 여부. react-hook-form 이면 formState.isDirty 를 넘긴다
 * @param message 확인 다이얼로그 문구
 * @returns leaveWithoutConfirm: 저장 성공 후처럼 확인 없이 이동할 때 이동 함수를 감싸서 호출한다
 */
export const useLeaveConfirm = (isDirty: boolean, message: string = CONFIRM_MESSAGES.LEAVE_UNSAVED) => {
  const { confirm } = useDialog();
  // 화면이 스스로 이동하는 중인지 (저장 성공 후 이동 등). true 면 확인을 건너뛴다
  const skipRef = useRef(false);

  const blocker = useBlocker(
    ({ currentLocation, nextLocation }) => isDirty && !skipRef.current && !isSessionExpiredHandled() && currentLocation.pathname !== nextLocation.pathname,
  );

  useEffect(() => {
    if (blocker.state !== 'blocked') return;

    // 확인을 기다리는 동안 화면이 언마운트되면 결과를 무시한다
    let active = true;
    void confirm(message).then((ok) => {
      if (!active) return;
      if (ok) blocker.proceed();
      else blocker.reset();
    });
    return () => {
      active = false;
    };
  }, [blocker, confirm, message]);

  useEffect(() => {
    if (!isDirty) return;

    const onBeforeUnload = (event: BeforeUnloadEvent) => {
      if (skipRef.current || isSessionExpiredHandled()) return;
      event.preventDefault();
      // 일부 브라우저(구형 Chrome 등)는 returnValue 를 설정해야 확인 창을 띄운다
      event.returnValue = '';
    };
    window.addEventListener('beforeunload', onBeforeUnload);
    return () => window.removeEventListener('beforeunload', onBeforeUnload);
  }, [isDirty]);

  /**
   * 확인 없이 이동. 저장을 마쳤거나 화면이 이미 따로 확인을 받은 경우에 쓴다.
   * 이동하면 화면이 언마운트되므로 플래그를 되돌리지 않아도 다음 진입에 영향이 없다.
   */
  const leaveWithoutConfirm = useCallback((leave: () => void) => {
    skipRef.current = true;
    leave();
  }, []);

  return { leaveWithoutConfirm };
};
