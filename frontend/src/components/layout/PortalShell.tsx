import { useState } from 'react';
import { Bell, Menu, X } from 'lucide-react';
import { Link, NavLink, Outlet } from 'react-router-dom';
import { useAuth } from '../../app/auth/AuthContext';
import { useUnreadCount } from '../../features/notifications/queries';
import { cn } from '../../lib/cn';
import { SIDEBAR_ITEMS } from '../../lib/nav';
import { Icon } from '../ui/Icon';
import { Logo } from '../ui/Logo';
import { ThemeToggle } from '../ui/ThemeToggle';
import { UserAvatar } from '../ui/UserAvatar';

export function PortalShell() {
  const { user } = useAuth();
  const [drawerOpen, setDrawerOpen] = useState(false);
  const items = user ? SIDEBAR_ITEMS[user.role].filter((i) => i.enabled) : [];
  const { data: unread } = useUnreadCount({
    refetchInterval: 60_000,
    enabled: Boolean(user),
  });

  const displayName = `${user?.firstName ?? ''} ${user?.lastName ?? ''}`.trim();
  const roleLabel = user?.role.replace('_', ' ') ?? '';

  const sidebarContent = (onNavigate?: () => void) => (
    <>
      <div className="flex h-14 shrink-0 items-center justify-between px-6">
        <Logo subtitle={roleLabel} />
        {onNavigate && (
          <button
            type="button"
            onClick={onNavigate}
            aria-label="Close navigation"
            className="rounded-lg p-1 text-text-secondary hover:bg-raised hover:text-text lg:hidden"
          >
            <Icon icon={X} size={20} />
          </button>
        )}
      </div>

      <nav className="flex-1 space-y-1 overflow-y-auto px-3 py-4">
        {items.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            end={item.end}
            onClick={onNavigate}
            className={({ isActive }) =>
              cn(
                'flex h-10 items-center gap-3 rounded-lg px-3 text-sm font-medium text-text-secondary transition-colors hover:bg-raised hover:text-text',
                isActive && 'bg-primary-subtle font-semibold text-primary-text',
              )
            }
          >
            {item.icon && <Icon icon={item.icon} size={18} />}
            <span>{item.label}</span>
          </NavLink>
        ))}
      </nav>

      <div className="shrink-0 border-t border-border p-3">
        <Link
          to="/account"
          onClick={onNavigate}
          className="flex items-center gap-3 rounded-xl p-2.5 transition-colors hover:bg-raised"
        >
          <UserAvatar name={displayName} avatarUrl={user?.avatarUrl} size="sm" />
          <div className="min-w-0 flex-1">
            <div className="truncate text-sm font-medium text-text">
              {user?.firstName} {user?.lastName}
            </div>
            <div className="truncate text-xs text-text-secondary">Account profile</div>
          </div>
        </Link>
      </div>
    </>
  );

  return (
    <div className="flex min-h-screen bg-bg">
      <aside className="hidden w-[240px] shrink-0 flex-col border-r border-border bg-surface lg:sticky lg:top-0 lg:flex lg:h-screen">
        {sidebarContent()}
      </aside>

      {drawerOpen && (
        <div className="fixed inset-0 z-40 flex lg:hidden">
          <div className="fixed inset-0 bg-scrim" onClick={() => setDrawerOpen(false)} />
          <aside className="relative z-50 flex h-full w-[240px] flex-col border-r border-border bg-surface shadow-xl">
            {sidebarContent(() => setDrawerOpen(false))}
          </aside>
        </div>
      )}

      <div className="flex min-w-0 flex-1 flex-col">
        <header className="sticky top-0 z-20 flex h-14 shrink-0 items-center justify-between border-b border-border bg-surface px-4 sm:px-6">
          <div className="flex items-center gap-3 lg:hidden">
            <button
              type="button"
              onClick={() => setDrawerOpen(true)}
              aria-label="Open navigation"
              className="rounded-lg p-1.5 text-text-secondary hover:bg-raised hover:text-text"
            >
              <Icon icon={Menu} size={20} />
            </button>
            <Logo />
          </div>
          <div className="ml-auto flex items-center gap-2">
            <ThemeToggle />
            <Link
              to="/notifications"
              aria-label="Notifications"
              className="relative flex h-9 w-9 items-center justify-center rounded-lg text-text-secondary transition-colors hover:bg-raised hover:text-text"
            >
              <Icon icon={Bell} size={18} />
              {Boolean(unread?.count) && (
                <span className="absolute right-1 top-1 flex h-4 min-w-4 items-center justify-center rounded-full bg-primary px-1 text-[10px] font-bold text-white">
                  {unread?.count}
                </span>
              )}
            </Link>
            <Link
              to="/account"
              aria-label="Account profile"
              className="flex h-9 items-center gap-2 rounded-lg px-2.5 text-sm font-medium text-text transition-colors hover:bg-raised"
            >
              <UserAvatar name={displayName} avatarUrl={user?.avatarUrl} size="xs" />
              <span className="hidden sm:inline">{user?.firstName}</span>
            </Link>
          </div>
        </header>
        <main className="mx-auto w-full max-w-[1200px] flex-1 px-4 py-6 sm:px-8">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
