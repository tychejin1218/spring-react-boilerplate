import { authHandlers } from '@/features/auth/mocks/authHandlers';
import { todoHandlers } from '@/features/todo/mocks/todoHandlers'; // [예제: todo]
import { fileHandlers } from '@/mocks/fileHandlers'; // [예제: file-upload] todo 이미지 업로드용 presigned URL
import { notificationHandlers } from '@/features/notification/mocks/notificationHandlers'; // [예제: notification]

/**
 * MSW 핸들러 통합 등록 지점
 * - 예제 기능을 제거할 때는 해당 import 와 아래 배열의 항목을 함께 지운다 (README "예제 기능 제거 방법" 참고)
 */
export const handlers = [
  ...authHandlers,
  ...todoHandlers, // [예제: todo]
  ...fileHandlers, // [예제: file-upload]
  ...notificationHandlers, // [예제: notification]
];
