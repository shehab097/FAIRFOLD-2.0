import { Navigate } from 'react-router-dom';
import { useAuth } from '../app/auth/AuthContext';
import { FullScreenLoader } from '../app/guards';
import { homePathForRole } from '../lib/auth';
import { PublicShell } from '../components/layout/PublicShell';
import { LandingPage } from './LandingPage';

/**
 * `/` is the public landing page for guests and a role-aware redirect for
 * signed-in users (candidate dashboard, employer jobs, etc.).
 */
export function HomePage() {
  const { user, initializing } = useAuth();

  if (initializing) return <FullScreenLoader />;
  if (user) return <Navigate to={homePathForRole(user.role)} replace />;

  return (
    <PublicShell>
      <LandingPage />
    </PublicShell>
  );
}
