import { useState } from 'react';
import { changePassword } from '../../../api/account';
import { useAuth } from '../../../app/auth/AuthContext';
import { Button } from '../../../components/ui/Button';
import { Card } from '../../../components/ui/Card';
import { Input } from '../../../components/ui/Input';
import { useToast } from '../../../components/ui/Toast';
import { getApiErrorMessage } from '../../../lib/errors';

export function AccountPage() {
  const { user } = useAuth();
  const { toast } = useToast();
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [busy, setBusy] = useState(false);

  async function submit() {
    if (newPassword.length < 10) {
      toast('New password must be at least 10 characters.', 'error');
      return;
    }
    if (newPassword !== confirm) {
      toast('Passwords do not match.', 'error');
      return;
    }
    setBusy(true);
    try {
      await changePassword(currentPassword, newPassword);
      toast('Password changed.', 'success');
      setCurrentPassword('');
      setNewPassword('');
      setConfirm('');
    } catch (err) {
      toast(getApiErrorMessage(err), 'error');
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-[28px] font-bold leading-9">Account settings</h1>
        <p className="text-text-secondary">Your account details.</p>
      </div>

      <Card>
        <h2 className="mb-4 text-xl font-semibold">Profile</h2>
        <div className="space-y-3 text-sm">
          <div>
            <div className="text-xs font-semibold uppercase tracking-[0.06em] text-text-secondary">Name</div>
            <div className="text-text">
              {user?.firstName} {user?.lastName}
            </div>
          </div>
          <div>
            <div className="text-xs font-semibold uppercase tracking-[0.06em] text-text-secondary">Email</div>
            <div className="text-text">{user?.email}</div>
          </div>
        </div>
      </Card>

      <Card>
        <h2 className="mb-4 text-xl font-semibold">Change password</h2>
        <div className="max-w-sm space-y-4">
          <div>
            <label className="mb-1 block text-xs font-semibold uppercase tracking-[0.06em] text-text-secondary">
              Current password
            </label>
            <Input
              type="password"
              value={currentPassword}
              onChange={(e) => setCurrentPassword(e.target.value)}
              autoComplete="current-password"
            />
          </div>
          <div>
            <label className="mb-1 block text-xs font-semibold uppercase tracking-[0.06em] text-text-secondary">
              New password
            </label>
            <Input
              type="password"
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              autoComplete="new-password"
            />
          </div>
          <div>
            <label className="mb-1 block text-xs font-semibold uppercase tracking-[0.06em] text-text-secondary">
              Confirm new password
            </label>
            <Input
              type="password"
              value={confirm}
              onChange={(e) => setConfirm(e.target.value)}
              autoComplete="new-password"
            />
          </div>
          <Button loading={busy} onClick={submit}>
            Change password
          </Button>
        </div>
      </Card>
    </div>
  );
}
