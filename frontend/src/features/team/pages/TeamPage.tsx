import { useState, type FormEvent } from 'react';
import {
  useCancelTeamInvite,
  useChangeTeamRole,
  useInviteTeamMember,
  useRemoveTeamMember,
  useTeam,
} from '../queries';
import { useAuth } from '../../../app/auth/AuthContext';
import type { TeamInvite, TeamMember, TeamRole } from '../types';
import { TEAM_ROLES, teamRoleLabel } from '../types';
import { Badge } from '../../../components/ui/Badge';
import { Banner } from '../../../components/ui/Banner';
import { Button } from '../../../components/ui/Button';
import { Dialog } from '../../../components/ui/Dialog';
import { Input } from '../../../components/ui/Input';
import { Select } from '../../../components/ui/Select';
import { Skeleton } from '../../../components/ui/Skeleton';
import { Table, type Column } from '../../../components/ui/Table';
import { useToast } from '../../../components/ui/Toast';
import { getApiErrorMessage } from '../../../lib/errors';
import { formatDate } from '../../../lib/format';

export function TeamPage() {
  const { user } = useAuth();
  const { data, isLoading, isError, refetch } = useTeam();
  const invite = useInviteTeamMember();
  const cancelInvite = useCancelTeamInvite();
  const changeRole = useChangeTeamRole();
  const removeMember = useRemoveTeamMember();
  const { toast } = useToast();

  const [inviteOpen, setInviteOpen] = useState(false);
  const [email, setEmail] = useState('');
  const [inviteRole, setInviteRole] = useState<TeamRole>('interviewer');
  const [inviteError, setInviteError] = useState<string | null>(null);
  const [roleTarget, setRoleTarget] = useState<TeamMember | null>(null);
  const [newRole, setNewRole] = useState<TeamRole>('interviewer');
  const [removeTarget, setRemoveTarget] = useState<TeamMember | null>(null);
  const [busy, setBusy] = useState(false);

  async function sendInvite(e: FormEvent) {
    e.preventDefault();
    setInviteError(null);
    setBusy(true);
    try {
      await invite.mutateAsync({ email, role: inviteRole });
      toast('Invite sent.', 'success');
      setInviteOpen(false);
      setEmail('');
    } catch (err) {
      setInviteError(getApiErrorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  async function confirmRole() {
    if (!roleTarget) return;
    setBusy(true);
    try {
      await changeRole.mutateAsync({ id: roleTarget.id, role: newRole });
      toast('Role updated.', 'success');
      setRoleTarget(null);
    } catch (err) {
      toast(getApiErrorMessage(err), 'error');
    } finally {
      setBusy(false);
    }
  }

  async function confirmRemove() {
    if (!removeTarget) return;
    setBusy(true);
    try {
      await removeMember.mutateAsync(removeTarget.id);
      toast('Member removed.', 'success');
      setRemoveTarget(null);
    } catch (err) {
      toast(getApiErrorMessage(err), 'error');
    } finally {
      setBusy(false);
    }
  }

  async function doCancelInvite(row: TeamInvite) {
    try {
      await cancelInvite.mutateAsync(row.id);
      toast('Invite cancelled.', 'success');
    } catch (err) {
      toast(getApiErrorMessage(err), 'error');
    }
  }

  const memberColumns: Column<TeamMember>[] = [
    { header: 'Name', render: (m) => <span className="font-medium text-text">{m.name}</span> },
    { header: 'Email', render: (m) => m.email },
    { header: 'Role', render: (m) => teamRoleLabel(m.role) },
    {
      header: 'Status',
      render: () => (
        <Badge tone="success">Active</Badge>
      ),
    },
    {
      header: '',
      render: (m) => (
        <div className="flex justify-end gap-1">
          <Button
            variant="ghost"
            className="h-8 px-2 text-xs"
            onClick={() => {
              setRoleTarget(m);
              setNewRole(m.role);
            }}
          >
            Change role
          </Button>
          <Button
            variant="danger"
            className="h-8 px-2 text-xs"
            disabled={m.id === user?.id}
            title={m.id === user?.id ? 'You cannot remove yourself.' : undefined}
            onClick={() => setRemoveTarget(m)}
          >
            Remove
          </Button>
        </div>
      ),
    },
  ];

  const inviteColumns: Column<TeamInvite>[] = [
    { header: 'Email', render: (i) => i.email },
    { header: 'Role', render: (i) => teamRoleLabel(i.role) },
    { header: 'Expires', render: (i) => formatDate(i.expiresAt) },
    { header: 'Status', render: () => <Badge tone="warning">Pending</Badge> },
    {
      header: '',
      render: (i) => (
        <div className="flex justify-end">
          <Button variant="ghost" className="h-8 px-2 text-xs" onClick={() => doCancelInvite(i)}>
            Cancel invite
          </Button>
        </div>
      ),
    },
  ];

  return (
    <div className="space-y-8">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h1 className="text-[28px] font-bold leading-9">Team</h1>
          <p className="text-text-secondary">
            Invite managers, HR and interviewers. The last HR cannot be removed or demoted.
          </p>
        </div>
        <Button onClick={() => setInviteOpen(true)}>Invite member</Button>
      </div>

      {isLoading && <Skeleton className="h-48 w-full rounded-xl" />}
      {isError && (
        <Banner tone="error">
          Could not load the team.{' '}
          <button type="button" className="underline" onClick={() => refetch()}>
            Retry
          </button>
        </Banner>
      )}

      {!isLoading && !isError && (
        <>
          <section>
            <h2 className="mb-3 text-lg font-semibold">Members</h2>
            <Table
              columns={memberColumns}
              rows={data?.members ?? []}
              rowKey={(m) => m.id}
              empty={<div className="rounded-xl border border-border bg-surface p-6 text-sm text-text-secondary">No members.</div>}
            />
          </section>

          <section>
            <h2 className="mb-3 text-lg font-semibold">Pending invites</h2>
            <Table
              columns={inviteColumns}
              rows={data?.invites ?? []}
              rowKey={(i) => i.id}
              empty={<div className="rounded-xl border border-border bg-surface p-6 text-sm text-text-secondary">No pending invites.</div>}
            />
          </section>
        </>
      )}

      <Dialog open={inviteOpen} title="Invite a team member" onClose={() => setInviteOpen(false)}>
        <form className="space-y-4" onSubmit={sendInvite}>
          <label className="block">
            <span className="mb-1 block text-xs text-text-secondary">Email</span>
            <Input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
            />
          </label>
          <label className="block">
            <span className="mb-1 block text-xs text-text-secondary">Role</span>
            <Select value={inviteRole} onChange={(e) => setInviteRole(e.target.value as TeamRole)}>
              {TEAM_ROLES.map((role) => (
                <option key={role.value} value={role.value}>
                  {role.label}
                </option>
              ))}
            </Select>
          </label>
          {inviteError && <p className="text-sm text-accent">{inviteError}</p>}
          <p className="text-xs text-text-secondary">
            A console email prints the accept-invite link (valid for 7 days).
          </p>
          <div className="flex justify-end gap-2">
            <Button variant="secondary" type="button" onClick={() => setInviteOpen(false)}>
              Cancel
            </Button>
            <Button type="submit" loading={busy}>
              Send invite
            </Button>
          </div>
        </form>
      </Dialog>

      <Dialog
        open={Boolean(roleTarget)}
        title={`Change role for ${roleTarget?.name ?? ''}`}
        onClose={() => setRoleTarget(null)}
      >
        <div className="space-y-4">
          <label className="block">
            <span className="mb-1 block text-xs text-text-secondary">Role</span>
            <Select value={newRole} onChange={(e) => setNewRole(e.target.value as TeamRole)}>
              {TEAM_ROLES.map((role) => (
                <option key={role.value} value={role.value}>
                  {role.label}
                </option>
              ))}
            </Select>
          </label>
          <div className="flex justify-end gap-2">
            <Button variant="secondary" onClick={() => setRoleTarget(null)}>
              Cancel
            </Button>
            <Button loading={busy} onClick={confirmRole}>
              Save role
            </Button>
          </div>
        </div>
      </Dialog>

      <Dialog
        open={Boolean(removeTarget)}
        title={`Remove ${removeTarget?.name ?? ''}`}
        onClose={() => setRemoveTarget(null)}
      >
        <p className="text-sm text-text">
          They will be signed out and cannot sign in again. This cannot be undone from the team page.
        </p>
        <div className="mt-5 flex justify-end gap-2">
          <Button variant="secondary" onClick={() => setRemoveTarget(null)}>
            Cancel
          </Button>
          <Button variant="danger" loading={busy} onClick={confirmRemove}>
            Remove member
          </Button>
        </div>
      </Dialog>
    </div>
  );
}
