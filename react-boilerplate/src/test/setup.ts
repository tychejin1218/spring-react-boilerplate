import '@testing-library/jest-dom/vitest';
import { server } from '@/mocks/server';

beforeAll(() => server.listen({ onUnhandledRequest: 'bypass' }));
// 테스트마다 server.use()로 덮어쓴 핸들러를 초기화해 다음 테스트로 새어 나가지 않게 한다
afterEach(() => server.resetHandlers());
afterAll(() => server.close());
