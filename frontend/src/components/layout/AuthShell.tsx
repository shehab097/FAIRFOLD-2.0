import { Link, Outlet } from 'react-router-dom';

export function AuthShell() {
  return (
    <div className="flex min-h-screen">
      <aside className="hidden w-1/2 flex-col justify-between bg-primary-subtle p-10 md:flex">
        <Link to="/" className="text-xl font-bold text-text">
          FairFold
        </Link>
        <ul className="space-y-4">
          <li className="text-lg font-semibold text-text">Anonymised screening</li>
          <li className="text-lg font-semibold text-text">Explained scores</li>
          <li className="text-lg font-semibold text-text">People decide</li>
        </ul>
      </aside>
      <main className="flex w-full items-center justify-center px-6 py-10 md:w-1/2">
        <div className="w-full max-w-sm">
          <Outlet />
        </div>
      </main>
    </div>
  );
}
