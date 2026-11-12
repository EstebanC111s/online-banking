import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { api, readSession, writeSession, type Session } from './api';

interface AuthState {
  session: Session | null;
  expiredNotice: boolean;
  login: (email: string, password: string) => Promise<Session>;
  logout: () => void;
  clearExpiredNotice: () => void;
}

const AuthContext = createContext<AuthState | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<Session | null>(() => readSession());
  const [expiredNotice, setExpiredNotice] = useState(false);

  const logout = useCallback(() => {
    writeSession(null);
    setSession(null);
  }, []);

  useEffect(() => {
    const onExpired = () => {
      logout();
      setExpiredNotice(true);
    };
    window.addEventListener('gocomezbo:session-expired', onExpired);
    return () => window.removeEventListener('gocomezbo:session-expired', onExpired);
  }, [logout]);

  useEffect(() => {
    if (!session) return;
    const ms = new Date(session.expiresAt).getTime() - Date.now();
    const id = window.setTimeout(() => {
      logout();
      setExpiredNotice(true);
    }, Math.max(ms, 0));
    return () => window.clearTimeout(id);
  }, [session, logout]);

  const login = useCallback(async (email: string, password: string) => {
    const s = await api<Session>('/api/auth/login', { method: 'POST', body: { email, password } });
    writeSession(s);
    setSession(s);
    setExpiredNotice(false);
    return s;
  }, []);

  const value = useMemo(
    () => ({ session, expiredNotice, login, logout, clearExpiredNotice: () => setExpiredNotice(false) }),
    [session, expiredNotice, login, logout],
  );
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth debe usarse dentro de AuthProvider');
  return ctx;
}
