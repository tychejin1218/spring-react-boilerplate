/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_APP_ENV: 'local' | 'dev' | 'stg' | 'prd';
  readonly VITE_API_BASE_URL: string;
  /** MSW 활성화 여부. env.ts 에서 'true' 문자열만 true 로 변환한다 (선택) */
  readonly VITE_ENABLE_MSW?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
