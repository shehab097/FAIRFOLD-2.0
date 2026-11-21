import type { ReactNode } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from './auth/AuthContext';
import { homePathForRole } from '../lib/auth';
import type { Role } from '../features/auth/types';

export function FullScreenLoader() {
  return (
    <div className="flex min-h-screen items-center justify-center bg-bg">
      <div className="animate-pulse text-sm text-text-secondary">Loading…</div>
    </div>
  );
}

/** Role-aware landing: signed-in users go to their dashboard, guests to the job board. */
export function HomeRedirect() {
  const { user, initializing } = useAuth();
  if (initializing) return <FullScreenLoader />;
  return <Navigate to={user ? homePathForRole(user.role) : '/jobs'} replace />;
}

export function RequireAuth({ children }: { children: ReactNode }) {
  const { user, initializing } = useAuth();
  const location = useLocation();
  if (initializing) return <FullScreenLoader />;
  if (!user) {
    return (
      <Navigate
        to={`/login?next=${encodeURIComponent(location.pathname + location.search)}`}
        replace
      />
    );
  }
  return <>{children}</>;
}

export function RequireRole({ roles, children }: { roles: Role[]; children: ReactNode }) {
  const { user } = useAuth();
  if (!user) return <Navigate to="/login" replace />;
  if (!roles.includes(user.role)) return <Navigate to="/403" replace />;
  return <>{children}</>;
}

export function GuestOnly({ children }: { children: ReactNode }) {
  const { user, initializing } = useAuth();
  const location = useLocation();
  if (initializing) return <FullScreenLoader />;
  if (user) {
    const next = new URLSearchParams(location.search).get('next');
    return <Navigate to={next ?? homePathForRole(user.role)} replace />;
  }
  return <>{children}</>;
}
