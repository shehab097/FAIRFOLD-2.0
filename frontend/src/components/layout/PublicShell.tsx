import { Link, Outlet } from 'react-router-dom';

export function PublicShell() {
  return (
    <div className="min-h-screen flex flex-col">
      <header className="h-14 flex items-center justify-between border-b border-border bg-surface px-6">
        <Link to="/" className="text-xl font-bold text-text">
          FairFold
        </Link>
        <nav className="flex items-center gap-4">
          <Link to="/login" className="text-sm text-text-secondary hover:text-text">
            Log in
          </Link>
          <Link to="/register" className="text-sm text-text-secondary hover:text-text">
            Register
          </Link>
        </nav>
      </header>
      <main className="flex-1 mx-auto w-full max-w-[1100px] px-6 py-6">
        <Outlet />
      </main>
    </div>
  );
}
