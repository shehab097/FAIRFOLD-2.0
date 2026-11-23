import type { ReactNode } from 'react';
import { Link, NavLink, Outlet } from 'react-router-dom';
import { useAuth } from '../../app/auth/AuthContext';
import { cn } from '../../lib/cn';
import { Logo } from '../ui/Logo';
import { ThemeToggle } from '../ui/ThemeToggle';
import { PortalShell } from './PortalShell';

export function PublicShell({ children }: { children?: ReactNode }) {
  const { user } = useAuth();

  // Signed-in users keep the full PortalShell (left sidebar + top bar) on public routes like Browse jobs.
  if (user) {
    return <PortalShell />;
  }

  return (
    <div className="flex min-h-screen flex-col bg-bg">
      <header className="sticky top-0 z-20 border-b border-border bg-surface">
        <div className="mx-auto flex h-14 max-w-[1280px] items-center justify-between px-4 sm:px-6">
          <div className="flex items-center gap-6">
            <Logo />
            <NavLink
              to="/jobs"
              end
              className={({ isActive }) =>
                cn(
                  'rounded-lg px-2.5 py-1 text-sm font-medium text-text-secondary transition-colors hover:text-text',
                  isActive && 'text-primary-text',
                )
              }
            >
              Browse jobs
            </NavLink>
          </div>
          <nav className="flex items-center gap-2.5">
            <ThemeToggle />
            <Link
              to="/login"
              className="rounded-lg border border-border-strong bg-surface px-3.5 py-1.5 text-sm font-medium text-text transition-colors hover:bg-raised"
            >
              Log in
            </Link>
            <Link
              to="/register"
              className="rounded-lg bg-primary px-3.5 py-1.5 text-sm font-semibold text-white shadow-sm transition-colors hover:bg-primary-hover"
            >
              Register
            </Link>
          </nav>
        </div>
      </header>
      <main className="mx-auto w-full max-w-[1280px] flex-1 px-4 py-6 sm:px-6">
        {children ?? <Outlet />}
      </main>
      <footer className="border-t border-border bg-surface py-4 text-xs text-text-subtle">
        <div className="mx-auto flex max-w-[1280px] flex-wrap items-center justify-between gap-2 px-4 sm:px-6">
          <span>FairFold · Fair, evidence-based hiring platform</span>
          <nav className="flex items-center gap-4" aria-label="Legal">
            <Link to="/terms" className="transition-colors hover:text-text">
              Terms of Service
            </Link>
            <Link to="/privacy" className="transition-colors hover:text-text">
              Privacy Policy
            </Link>
          </nav>
        </div>
      </footer>
    </div>
  );
}
