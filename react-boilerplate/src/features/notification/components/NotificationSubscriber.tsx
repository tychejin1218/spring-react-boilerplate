import { useNotifications } from '@/features/notification/hooks/useNotifications';

/**
 * 실시간 알림 구독 컴포넌트
 *
 * - 로그인 상태에서 SSE(/notifications/subscribe)를 구독하고 수신한 알림을 Toast 로 띄운다.
 * - UI 를 렌더하지 않으며, Layout 에 한 줄로 배치해 알림 기능을 켜고 끈다.
 * - 알림 feature 를 제거할 때는 Layout 의 `<NotificationSubscriber />` 와 import 한 줄만 지우면 된다.
 */
const NotificationSubscriber = () => {
  useNotifications();
  return null;
};

export default NotificationSubscriber;
