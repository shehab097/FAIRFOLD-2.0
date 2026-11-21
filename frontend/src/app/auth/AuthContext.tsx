import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from 'react';
import type { UserDto } from '../../features/auth/types';
import * as authApi from '../../api/auth';

interface AuthContextValue {
  user: UserDto | null;
  initializing: boolean;
  signIn: (email: string, password: string) => Promise<void>;
  register: (input: authApi.RegisterInput) => Promise<void>;
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
    const session = await authApi.login(email, password);
    setUser(session.user);
  }, []);

  const register = useCallback(async (input: authApi.RegisterInput) => {
    const session = await authApi.register(input);
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
    <AuthContext.Provider value={{ user, initializing, signIn, register, signOut, updateUser }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
