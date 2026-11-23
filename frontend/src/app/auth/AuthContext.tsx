import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from 'react';
import type { UserDto } from '../../features/auth/types';
import * as authApi from '../../api/auth';
import type { LoginResult } from '../../features/auth/types';

interface AuthContextValue {
  user: UserDto | null;
  initializing: boolean;
  signIn: (email: string, password: string) => Promise<LoginResult>;
  verifyMfa: (mfaToken: string, code: string) => Promise<void>;
  register: (input: authApi.RegisterInput) => Promise<void>;
  acceptInvite: (input: authApi.AcceptInviteInput) => Promise<void>;
  signOut: () => Promise<void>;
  updateUser: (user: UserDto) => void;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserDto | null>(null);
  const [initializing, setInitializing] = useState(true);

  useEffect(() => {
    let active = true;
    authApi.bootstrapSession().then((session) => {
      if (!active) return;
      if (session) setUser(session.user);
      setInitializing(false);
    });

    const handleSessionExpired = () => {
      setUser(null);
    };
    window.addEventListener('auth:session-expired', handleSessionExpired);

    return () => {
      active = false;
      window.removeEventListener('auth:session-expired', handleSessionExpired);
    };
  }, []);

  const signIn = useCallback(async (email: string, password: string) => {
    const result = await authApi.login(email, password);
    if ('accessToken' in result) {
      setUser(result.user);
    }
    return result;
  }, []);

  const verifyMfa = useCallback(async (mfaToken: string, code: string) => {
    const session = await authApi.verifyMfa(mfaToken, code);
    setUser(session.user);
  }, []);

  const register = useCallback(async (input: authApi.RegisterInput) => {
    const session = await authApi.register(input);
    setUser(session.user);
  }, []);

  const acceptInvite = useCallback(async (input: authApi.AcceptInviteInput) => {
    const session = await authApi.acceptInvite(input);
    setUser(session.user);
  }, []);

  const signOut = useCallback(async () => {
    await authApi.logout();
    setUser(null);
  }, []);

  const updateUser = useCallback((next: UserDto) => {
    setUser(next);
  }, []);

  return (
    <AuthContext.Provider value={{ user, initializing, signIn, verifyMfa, register, acceptInvite, signOut, updateUser }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
