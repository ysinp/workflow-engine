import { createContext, ReactNode, useCallback, useContext, useMemo, useState } from 'react';
import type { Account } from '../types';

const STORAGE_KEY = 'kyc-demo.auth';

interface StoredSession {
  token: string;
  account: Account;
}

export type LogoutReason = 'expired';

export interface AuthContextValue {
  token: string | null;
  account: Account | null;
  isAuthenticated: boolean;
  /** Why the last session ended, if it was not a user-initiated sign-out. */
  logoutReason: LogoutReason | null;
  login: (token: string, account: Account) => void;
  logout: (reason?: LogoutReason) => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

// Every storage access is wrapped in try/catch: localStorage can throw (or be
// unavailable) in private browsing or when site data is blocked. In that case
// the session simply lives in memory for this tab.
function readSession(): StoredSession | null {
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY);
    if (!raw) return null;
    const parsed = JSON.parse(raw) as Partial<StoredSession> | null;
    if (parsed && typeof parsed.token === 'string' && parsed.token && parsed.account) {
      return { token: parsed.token, account: parsed.account };
    }
    return null;
  } catch {
    return null;
  }
}

function writeSession(session: StoredSession | null): void {
  try {
    if (session) {
      window.localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
    } else {
      window.localStorage.removeItem(STORAGE_KEY);
    }
  } catch {
    // ignore - storage unavailable
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<StoredSession | null>(() => readSession());
  const [logoutReason, setLogoutReason] = useState<LogoutReason | null>(null);

  const login = useCallback((token: string, account: Account) => {
    const next = { token, account };
    writeSession(next);
    setSession(next);
    setLogoutReason(null);
  }, []);

  const logout = useCallback((reason?: LogoutReason) => {
    writeSession(null);
    setSession(null);
    setLogoutReason(reason ?? null);
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      token: session?.token ?? null,
      account: session?.account ?? null,
      isAuthenticated: Boolean(session?.token),
      logoutReason,
      login,
      logout,
    }),
    [session, logoutReason, login, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth must be used inside <AuthProvider>.');
  }
  return ctx;
}
