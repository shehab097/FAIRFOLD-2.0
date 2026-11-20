import { Bell, LogOut } from 'lucide-react';
import { Link, NavLink, Outlet } from 'react-router-dom';
import { useAuth } from '../../app/auth/AuthContext';
import { cn } from '../../lib/cn';
import { SIDEBAR_ITEMS } from '../../lib/nav';
import { Icon } from '../ui/Icon';

export function PortalShell() {
  const { user, signOut } = useAuth();
  const items = user ? SIDEBAR_ITEMS[user.role].filter((i) => i.enabled) : [];

  return (
    <div className="flex min-h-screen">
      <aside className="hidden w-[248px] shrink-0 flex-col border-r border-border bg-surface lg:flex">
        <div className="flex h-14 items-center border-b border-border px-6">
          <Link to="/" className="text-xl font-bold text-text">
            FairFold
          </Link>
        </div>
        <nav className="flex-1 py-4">
          {items.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) =>
                cn(
                  'flex h-10 items-center border-l-[3px] border-l-transparent px-6 text-sm text-text-secondary hover:bg-raised',
                  isActive && 'border-l-primary bg-primary-subtle text-primary-text',
                )
              }
            >
              {item.label}
            </NavLink>
          ))}
        </nav>
      </aside>

      <div className="flex flex-1 flex-col">
        <header className="flex h-14 items-center justify-between border-b border-border bg-surface px-6">
          <Link to="/" className="text-xl font-bold text-text lg:hidden">
            FairFold
          </Link>
          <div className="ml-auto flex items-center gap-4">
            <button aria-label="Notifications" className="text-text-secondary hover:text-text">
              <Icon icon={Bell} size={20} />
            </button>
            <button
              onClick={() => signOut()}
              className="flex items-center gap-1 text-sm text-text-secondary hover:text-text"
            >
              <Icon icon={LogOut} size={16} />
              Sign out
            </button>
          </div>
        </header>
        <main className="mx-auto w-full max-w-[1280px] flex-1 px-6 py-6">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
