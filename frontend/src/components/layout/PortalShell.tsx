import { useState } from 'react';
import { Bell, KeyRound, LogOut, Menu, User, X } from 'lucide-react';
import { Link, NavLink, Outlet } from 'react-router-dom';
import { useAuth } from '../../app/auth/AuthContext';
import { useUnreadCount } from '../../features/notifications/queries';
import { cn } from '../../lib/cn';
import { ACCOUNT_NAV_ITEMS, SIDEBAR_ITEMS, type NavItem } from '../../lib/nav';
import { Icon } from '../ui/Icon';
import { Logo } from '../ui/Logo';

function NavGroup({ title, items, onNavigate }: { title: string; items: NavItem[]; onNavigate?: () => void }) {
  return (
    <div className="mb-5">
      <div className="mb-1.5 px-6 text-xs font-semibold uppercase tracking-[0.06em] text-text-subtle">
        {title}
      </div>
      {items.map((item) => (
        <NavLink
          key={item.to}
          to={item.to}
          end={item.end}
          onClick={onNavigate}
          className={({ isActive }) =>
            cn(
              'flex h-10 items-center gap-3 border-l-[3px] border-l-transparent px-6 text-sm font-medium text-text-secondary hover:bg-raised hover:text-text',
              isActive && 'border-l-primary bg-primary-subtle text-primary-text',
            )
          }
        >
          {item.icon && <Icon icon={item.icon} size={18} />}
          <span>{item.label}</span>
        </NavLink>
      ))}
    </div>
  );
}

export function PortalShell() {
  const { user, signOut } = useAuth();
  const [drawerOpen, setDrawerOpen] = useState(false);
  const items = user ? SIDEBAR_ITEMS[user.role].filter((i) => i.enabled) : [];
  const { data: unread } = useUnreadCount({
    refetchInterval: 60_000,
    enabled: Boolean(user),
  });

  const initials = `${user?.firstName?.[0] ?? ''}${user?.lastName?.[0] ?? ''}`.toUpperCase() || 'U';
  const roleLabel = user?.role.replace('_', ' ') ?? '';

  const sidebarContent = (onNavigate?: () => void) => (
    <>
      <div className="flex h-14 items-center justify-between border-b border-border px-6">
        <Logo subtitle={roleLabel} />
        {onNavigate && (
          <button
            type="button"
            onClick={onNavigate}
            aria-label="Close navigation"
            className="p-1 text-text-secondary hover:text-text lg:hidden"
          >
            <Icon icon={X} size={20} />
          </button>
        )}
      </div>
      <nav className="flex-1 py-5">
        <NavGroup title="Workspace" items={items} onNavigate={onNavigate} />
        <NavGroup title="Account" items={ACCOUNT_NAV_ITEMS} onNavigate={onNavigate} />
      </nav>
      <div className="border-t border-border bg-bg p-4">
        <div className="flex items-center gap-3">
          <div className="flex h-8 w-8 shrink-0 items-center justify-center border border-border-strong bg-primary-subtle text-xs font-bold text-primary-text">
            {initials}
          </div>
          <div className="min-w-0 flex-1">
            <div className="truncate text-sm font-semibold text-text">
              {user?.firstName} {user?.lastName}
            </div>
            <div className="truncate text-xs text-text-secondary">{user?.email}</div>
          </div>
        </div>
      </div>
    </>
  );

  return (
    <div className="flex min-h-screen bg-bg">
      <aside className="hidden w-[248px] shrink-0 flex-col border-r border-border bg-surface lg:flex">
        {sidebarContent()}
      </aside>

      {drawerOpen && (
        <div className="fixed inset-0 z-40 flex lg:hidden">
          <div className="fixed inset-0 bg-scrim" onClick={() => setDrawerOpen(false)} />
          <aside className="relative z-50 flex w-[248px] flex-col border-r border-border-strong bg-surface">
            {sidebarContent(() => setDrawerOpen(false))}
          </aside>
        </div>
      )}

      <div className="flex min-w-0 flex-1 flex-col">
        <header className="flex h-14 items-center justify-between border-b border-border border-t-2 border-t-primary bg-surface px-4 sm:px-6">
          <div className="flex items-center gap-3 lg:hidden">
            <button
              type="button"
              onClick={() => setDrawerOpen(true)}
              aria-label="Open navigation"
              className="border border-border bg-raised p-1.5 text-text-secondary hover:text-text"
            >
              <Icon icon={Menu} size={18} />
            </button>
            <Logo />
          </div>
          <div className="ml-auto flex items-center gap-2 sm:gap-3">
            <Link
              to="/notifications"
              aria-label="Notifications"
              className="relative flex h-9 w-9 items-center justify-center border border-border bg-bg text-text-secondary hover:bg-raised hover:text-text"
            >
              <Icon icon={Bell} size={18} />
              {Boolean(unread?.count) && (
                <span className="absolute -right-1 -top-1 flex h-4 min-w-4 items-center justify-center bg-primary px-1 text-[10px] font-bold text-white">
                  {unread?.count}
                </span>
              )}
            </Link>
            <Link
              to="/account/password"
              title="Change password"
              aria-label="Change password"
              className="flex h-9 items-center gap-1.5 border border-border bg-bg px-2.5 text-xs font-medium text-text-secondary hover:bg-raised hover:text-text"
            >
              <Icon icon={KeyRound} size={15} />
              <span className="hidden sm:inline">Password</span>
            </Link>
            <Link
              to="/account"
              aria-label="Account settings"
              className="flex h-9 items-center gap-2 border border-border bg-bg px-2.5 text-xs font-medium text-text hover:bg-raised"
            >
              <span className="flex h-5 w-5 items-center justify-center bg-primary-subtle text-[10px] font-bold text-primary-text">
                {initials}
              </span>
              <span className="hidden sm:inline">{user?.firstName ?? 'Account'}</span>
              <Icon icon={User} size={14} className="text-text-secondary sm:hidden" />
            </Link>
            <button
              type="button"
              onClick={() => signOut()}
              className="flex h-9 items-center gap-1.5 border border-border bg-bg px-2.5 text-xs font-medium text-text-secondary hover:border-danger hover:bg-danger-subtle hover:text-text"
            >
              <Icon icon={LogOut} size={15} />
              <span className="hidden sm:inline">Sign out</span>
            </button>
          </div>
        </header>
        <main className="mx-auto w-full max-w-[1280px] flex-1 px-4 py-6 sm:px-6">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
