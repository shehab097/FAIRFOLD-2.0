import { Link, NavLink, Outlet } from 'react-router-dom';
import { useAuth } from '../../app/auth/AuthContext';
import { cn } from '../../lib/cn';
import { Logo } from '../ui/Logo';

function portalHome(role?: string): string {
  if (role === 'CANDIDATE') return '/candidate';
  if (role === 'EMPLOYER_HR' || role === 'EMPLOYER_MANAGER') return '/employer/jobs';
  if (role === 'INTERVIEWER') return '/employer/interviews';
  if (role === 'ADMIN') return '/admin';
  return '/account';
}

export function PublicShell() {
  const { user } = useAuth();

  return (
    <div className="flex min-h-screen flex-col bg-bg">
      <header className="border-b border-border border-t-2 border-t-primary bg-surface">
        <div className="mx-auto flex h-14 max-w-[1100px] items-center justify-between px-4 sm:px-6">
          <div className="flex items-center gap-6">
            <Logo />
            <NavLink
              to="/"
              end
              className={({ isActive }) =>
                cn(
                  'text-sm font-medium text-text-secondary hover:text-text',
                  isActive && 'text-primary-text',
                )
              }
            >
              Jobs
            </NavLink>
          </div>
          <nav className="flex items-center gap-3">
            {user ? (
              <>
                <Link
                  to={portalHome(user.role)}
                  className="border border-border-strong bg-bg px-3 py-1.5 text-xs font-semibold uppercase tracking-[0.06em] text-text hover:bg-raised"
                >
                  Workspace
                </Link>
                <Link
                  to="/account"
                  className="bg-primary px-3 py-1.5 text-xs font-semibold uppercase tracking-[0.06em] text-white hover:bg-primary-hover"
                >
                  {user.firstName}
                </Link>
              </>
            ) : (
              <>
                <Link
                  to="/login"
                  className="border border-border-strong bg-bg px-3.5 py-1.5 text-sm font-medium text-text hover:bg-raised"
                >
                  Log in
                </Link>
                <Link
                  to="/register"
                  className="bg-primary px-3.5 py-1.5 text-sm font-semibold text-white hover:bg-primary-hover"
                >
                  Register
                </Link>
              </>
            )}
          </nav>
        </div>
      </header>
      <main className="mx-auto w-full max-w-[1100px] flex-1 px-4 py-6 sm:px-6">
        <Outlet />
      </main>
      <footer className="border-t border-border bg-surface py-4 text-xs text-text-subtle">
        <div className="mx-auto flex max-w-[1100px] flex-wrap items-center justify-between gap-2 px-4 sm:px-6">
          <span>FairFold · Fair, evidence-based hiring platform</span>
          <span>Anonymised screening · Explained scores · People decide</span>
        </div>
      </footer>
    </div>
  );
}
