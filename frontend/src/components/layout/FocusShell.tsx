import { Link, Outlet } from 'react-router-dom';
import { ThemeToggle } from '../ui/ThemeToggle';
import { Logo } from '../ui/Logo';

/** Distraction-free shell for an assessment attempt (P18). */
export function FocusShell() {
  return (
    <div className="flex min-h-screen flex-col bg-bg">
      <header className="sticky top-0 z-20 flex h-14 shrink-0 items-center justify-between border-b border-border bg-surface px-4 sm:px-6">
        <Logo />
        <div className="flex items-center gap-3">
          <Link
            to="/candidate/assessments"
            className="text-sm font-medium text-text-secondary hover:text-text"
          >
            Exit
          </Link>
          <ThemeToggle />
        </div>
      </header>
      <main className="mx-auto w-full max-w-4xl flex-1 px-4 py-6 sm:px-8">
        <Outlet />
      </main>
    </div>
  );
}
