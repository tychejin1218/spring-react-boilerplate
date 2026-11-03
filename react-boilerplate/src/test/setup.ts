import '@testing-library/jest-dom/vitest';
import { server } from '@/mocks/server';

// Node 26 은 --localstorage-file 옵션 없이 실행하면 전역 localStorage 를 쓸 수 없는 값으로 두는데,
// 이것이 jsdom 의 localStorage 를 가린다. 그러면 zustand persist 가 저장할 때 setItem 오류가 난다.
// 이 경우에만 테스트용 메모리 저장소로 바꿔 끼운다.
if (typeof globalThis.localStorage?.setItem !== 'function') {
  const store = new Map<string, string>();
  const memoryStorage: Storage = {
    get length() {
      return store.size;
    },
    clear: () => store.clear(),
    getItem: (key) => store.get(key) ?? null,
    key: (index) => [...store.keys()][index] ?? null,
    removeItem: (key) => void store.delete(key),
    setItem: (key, value) => void store.set(key, String(value)),
  };
  Object.defineProperty(globalThis, 'localStorage', { value: memoryStorage, configurable: true });
}

beforeAll(() => server.listen({ onUnhandledRequest: 'bypass' }));
// 테스트마다 server.use()로 덮어쓴 핸들러를 초기화해 다음 테스트로 새어 나가지 않게 한다
afterEach(() => server.resetHandlers());
afterAll(() => server.close());
