import { useEffect, useState } from 'react';
import { KeyRound, Search, Unlock } from 'lucide-react';
import {
  useAdminUsers,
  useChangeUserRole,
  useResetUserMfa,
  useSendPasswordReset,
  useUnlockUser,
} from '../adminQueries';
import type { AdminUser } from '../adminTypes';
import { Badge } from '../../../components/ui/Badge';
import { Banner } from '../../../components/ui/Banner';
import { Button } from '../../../components/ui/Button';
import { Dialog } from '../../../components/ui/Dialog';
import { Icon } from '../../../components/ui/Icon';
import { Input } from '../../../components/ui/Input';
import { Select } from '../../../components/ui/Select';
import { Skeleton } from '../../../components/ui/Skeleton';
import { Table, type Column } from '../../../components/ui/Table';
import { useToast } from '../../../components/ui/Toast';
import { getApiErrorMessage } from '../../../lib/errors';
import { formatDate } from '../../../lib/format';

const ROLES = ['EMPLOYER_HR', 'EMPLOYER_MANAGER', 'INTERVIEWER'];
const ROLE_LABELS: Record<string, string> = {
  CANDIDATE: 'Candidate',
  EMPLOYER_HR: 'Employer HR',
  EMPLOYER_MANAGER: 'Employer manager',
  INTERVIEWER: 'Interviewer',
  ADMIN: 'Admin',
};

function roleLabel(role: string): string {
  return ROLE_LABELS[role] ?? role.replace(/_/g, ' ');
}

function status(user: AdminUser): { label: string; tone: 'success' | 'warning' | 'failure' | 'neutral' } {
  if (user.disabled) return { label: 'Disabled', tone: 'failure' };
  if (user.locked) return { label: 'Locked', tone: 'warning' };
  return { label: 'Active', tone: 'success' };
}

export function AdminUsersPage() {
  const { toast } = useToast();
  const [qInput, setQInput] = useState('');
  const [q, setQ] = useState('');
  const [role, setRole] = useState('');
  const [locked, setLocked] = useState('');
  const [page, setPage] = useState(0);
  const [roleTarget, setRoleTarget] = useState<AdminUser | null>(null);
  const [nextRole, setNextRole] = useState('EMPLOYER_MANAGER');
  const [resetTarget, setResetTarget] = useState<AdminUser | null>(null);
  const [mfaTarget, setMfaTarget] = useState<AdminUser | null>(null);
  const [dialogError, setDialogError] = useState<string | null>(null);

  const filters = {
    q: q || undefined,
    role: role || undefined,
    locked: locked === '' ? undefined : locked === 'true',
    page,
  };
  const { data, isLoading, isError, refetch } = useAdminUsers(filters);
  const changeRole = useChangeUserRole();
  const unlock = useUnlockUser();
  const reset = useSendPasswordReset();
  const resetMfa = useResetUserMfa();

  useEffect(() => {
    if (roleTarget) {
      setNextRole(
        ROLES.includes(roleTarget.role) ? roleTarget.role : 'EMPLOYER_MANAGER',
      );
      setDialogError(null);
    }
  }, [roleTarget]);

  async function submitRole() {
    if (!roleTarget) return;
    setDialogError(null);
    try {
      await changeRole.mutateAsync({ id: roleTarget.id, role: nextRole });
      toast('Role updated.', 'success');
      setRoleTarget(null);
    } catch (error) {
      setDialogError(getApiErrorMessage(error));
    }
  }

  async function submitReset() {
    if (!resetTarget) return;
    setDialogError(null);
    try {
      await reset.mutateAsync(resetTarget.id);
      toast('Password reset link sent.', 'success');
      setResetTarget(null);
    } catch (error) {
      setDialogError(getApiErrorMessage(error));
    }
  }

  async function submitMfaReset() {
    if (!mfaTarget) return;
    setDialogError(null);
    try {
      await resetMfa.mutateAsync(mfaTarget.id);
      toast('MFA reset.', 'success');
      setMfaTarget(null);
    } catch (error) {
      setDialogError(getApiErrorMessage(error));
    }
  }

  async function doUnlock(user: AdminUser) {
    try {
      await unlock.mutateAsync(user.id);
      toast(`${user.name} unlocked.`, 'success');
    } catch (error) {
      toast(getApiErrorMessage(error), 'error');
    }
  }

  const columns: Column<AdminUser>[] = [
    {
      header: 'Name',
      render: (user) => (
        <div>
          <div className="font-medium text-text">{user.name}</div>
          <div className="text-xs text-text-secondary">{user.email}</div>
        </div>
      ),
    },
    { header: 'Role', render: (user) => roleLabel(user.role) },
    {
      header: 'Status',
      render: (user) => {
        const value = status(user);
        return <Badge tone={value.tone}>{value.label}</Badge>;
      },
    },
    {
      header: 'MFA',
      render: (user) => (
        <span className="text-sm text-text-secondary">{user.mfaEnabled ? 'On' : 'Off'}</span>
      ),
    },
    {
      header: 'Created',
      render: (user) => <span className="text-sm text-text-secondary">{formatDate(user.createdAt)}</span>,
    },
    {
      header: 'Actions',
      render: (user) => (
        <div className="flex flex-wrap gap-2">
          {ROLES.includes(user.role) && (
            <Button
              variant="secondary"
              className="h-8 px-2 text-xs"
              onClick={() => setRoleTarget(user)}
            >
              Change role
            </Button>
          )}
          {user.locked && (
            <Button
              variant="secondary"
              className="h-8 px-2 text-xs"
              loading={unlock.isPending}
              onClick={() => doUnlock(user)}
            >
              <Icon icon={Unlock} size={13} /> Unlock
            </Button>
          )}
          <Button
            variant="secondary"
            className="h-8 px-2 text-xs"
            onClick={() => {
              setDialogError(null);
              setResetTarget(user);
            }}
          >
            <Icon icon={KeyRound} size={13} /> Reset password
          </Button>
          {user.mfaEnabled && (
            <Button
              variant="secondary"
              className="h-8 px-2 text-xs"
              onClick={() => {
                setDialogError(null);
                setMfaTarget(user);
              }}
            >
              Reset MFA
            </Button>
          )}
        </div>
      ),
    },
  ];

  return (
    <div className="space-y-5">
      <div>
        <h1 className="text-[28px] font-bold leading-9">Users</h1>
        <p className="text-sm text-text-secondary">
          Search accounts, change employer team roles, unlock locked accounts and send password resets.
        </p>
      </div>

      <div className="flex flex-wrap items-end gap-3">
        <form
          className="flex min-w-[260px] flex-1 items-end gap-2"
          onSubmit={(event) => {
            event.preventDefault();
            setPage(0);
            setQ(qInput.trim());
          }}
        >
          <label className="block flex-1">
            <span className="mb-1 block text-xs font-semibold uppercase tracking-[0.06em] text-text-secondary">
              Search
            </span>
            <Input
              value={qInput}
              onChange={(event) => setQInput(event.target.value)}
              placeholder="Name or email"
            />
          </label>
          <Button type="submit" variant="secondary">
            <Icon icon={Search} size={16} /> Search
          </Button>
        </form>
        <label className="block w-[180px]">
          <span className="mb-1 block text-xs font-semibold uppercase tracking-[0.06em] text-text-secondary">
            Role
          </span>
          <Select
            value={role}
            onChange={(event) => {
              setRole(event.target.value);
              setPage(0);
            }}
          >
            <option value="">All roles</option>
            {Object.keys(ROLE_LABELS).map((value) => (
              <option key={value} value={value}>
                {ROLE_LABELS[value]}
              </option>
            ))}
          </Select>
        </label>
        <label className="block w-[160px]">
          <span className="mb-1 block text-xs font-semibold uppercase tracking-[0.06em] text-text-secondary">
            Lock status
          </span>
          <Select
            value={locked}
            onChange={(event) => {
              setLocked(event.target.value);
              setPage(0);
            }}
          >
            <option value="">Any</option>
            <option value="true">Locked</option>
            <option value="false">Not locked</option>
          </Select>
        </label>
      </div>

      {isError && <Banner tone="error">Could not load users. <button className="underline" onClick={() => refetch()}>Retry</button></Banner>}

      {isLoading || !data ? (
        <Skeleton className="h-64 w-full rounded-xl" />
      ) : (
        <>
          <Table columns={columns} rows={data.items} rowKey={(user) => user.id} />
          <div className="flex flex-wrap items-center justify-between gap-2 text-sm text-text-secondary">
            <span>
              {data.totalItems.toLocaleString()} user{data.totalItems === 1 ? '' : 's'}
            </span>
            <div className="flex items-center gap-2">
              <Button
                variant="secondary"
                disabled={page === 0}
                onClick={() => setPage((value) => Math.max(0, value - 1))}
              >
                Previous
              </Button>
              <span>
                Page {data.totalPages === 0 ? 0 : page + 1} of {data.totalPages}
              </span>
              <Button
                variant="secondary"
                disabled={page + 1 >= data.totalPages}
                onClick={() => setPage((value) => value + 1)}
              >
                Next
              </Button>
            </div>
          </div>
        </>
      )}

      <Dialog open={Boolean(roleTarget)} title="Change role" onClose={() => setRoleTarget(null)}>
        {roleTarget && (
          <div className="space-y-4">
            <p className="text-sm text-text-secondary">
              Change <span className="font-semibold text-text">{roleTarget.name}</span> from{' '}
              {roleLabel(roleTarget.role)}.
            </p>
            <label className="block">
              <span className="mb-1 block text-xs font-semibold uppercase tracking-[0.06em] text-text-secondary">
                New role
              </span>
              <Select value={nextRole} onChange={(event) => setNextRole(event.target.value)}>
                {ROLES.map((value) => (
                  <option key={value} value={value}>
                    {roleLabel(value)}
                  </option>
                ))}
              </Select>
            </label>
            {dialogError && <Banner tone="warning">{dialogError}</Banner>}
            <div className="flex justify-end gap-2">
              <Button variant="secondary" onClick={() => setRoleTarget(null)}>
                Cancel
              </Button>
              <Button loading={changeRole.isPending} onClick={submitRole}>
                Confirm change
              </Button>
            </div>
          </div>
        )}
      </Dialog>

      <Dialog open={Boolean(mfaTarget)} title="Reset MFA" onClose={() => setMfaTarget(null)}>
        {mfaTarget && (
          <div className="space-y-4">
            <p className="text-sm text-text-secondary">
              Clear the authenticator setup for{' '}
              <span className="font-semibold text-text">{mfaTarget.name}</span>? They can set it up
              again later.
            </p>
            {dialogError && <Banner tone="warning">{dialogError}</Banner>}
            <div className="flex justify-end gap-2">
              <Button variant="secondary" onClick={() => setMfaTarget(null)}>
                Cancel
              </Button>
              <Button variant="danger" loading={resetMfa.isPending} onClick={submitMfaReset}>
                Reset MFA
              </Button>
            </div>
          </div>
        )}
      </Dialog>

      <Dialog
        open={Boolean(resetTarget)}
        title="Send password reset"
        onClose={() => setResetTarget(null)}
      >
        {resetTarget && (
          <div className="space-y-4">
            <p className="text-sm text-text-secondary">
              Send a one-hour password reset link to{' '}
              <span className="font-semibold text-text">{resetTarget.email}</span>?
            </p>
            {dialogError && <Banner tone="warning">{dialogError}</Banner>}
            <div className="flex justify-end gap-2">
              <Button variant="secondary" onClick={() => setResetTarget(null)}>
                Cancel
              </Button>
              <Button loading={reset.isPending} onClick={submitReset}>
                Send link
              </Button>
            </div>
          </div>
        )}
      </Dialog>
    </div>
  );
}
