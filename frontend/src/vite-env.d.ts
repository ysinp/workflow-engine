/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_SUBMISSION_API?: string;
  readonly VITE_ACCOUNTS_API?: string;
  readonly VITE_ORCHESTRATION_API?: string;
  readonly VITE_IDENTIFICATION_API?: string;
  readonly VITE_KYC_API?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
