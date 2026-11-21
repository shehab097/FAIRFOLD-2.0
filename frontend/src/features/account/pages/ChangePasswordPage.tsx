import { useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { changePassword } from '../../../api/account';
import { Banner } from '../../../components/ui/Banner';
import { Button } from '../../../components/ui/Button';
import { Input } from '../../../components/ui/Input';
import { useToast } from '../../../components/ui/Toast';
import { getApiErrorMessage } from '../../../lib/errors';

export function ChangePasswordPage() {
  const { toast } = useToast();
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    if (newPassword.length < 10) {
      const msg = 'New password must be at least 10 characters.';
      setError(msg);
      toast(msg, 'error');
      return;
    }
    if (newPassword !== confirm) {
      const msg = 'Passwords do not match.';
      setError(msg);
      toast(msg, 'error');
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
      const msg = getApiErrorMessage(err);
      setError(msg);
      toast(msg, 'error');
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="max-w-lg space-y-6">
      <div>
        <Link to="/account" className="text-sm text-text-secondary hover:text-primary-text">
          ← Account profile
        </Link>
        <h1 className="mt-2 text-[28px] font-bold leading-9">Change password</h1>
        <p className="text-sm text-text-secondary">Update your account password. Use at least 10 characters.</p>
      </div>

      {error && <Banner tone="error">{error}</Banner>}

      <form onSubmit={onSubmit} className="space-y-4 rounded-xl border border-border bg-surface p-6 shadow-sm">
        <div>
          <label htmlFor="current-password" className="mb-1 block text-xs text-text-secondary">
            Current password
          </label>
          <Input
            id="current-password"
            type="password"
            value={currentPassword}
            onChange={(e) => setCurrentPassword(e.target.value)}
            autoComplete="current-password"
            required
          />
        </div>
        <div>
          <label htmlFor="new-password" className="mb-1 block text-xs text-text-secondary">
            New password
          </label>
          <Input
            id="new-password"
            type="password"
            value={newPassword}
            onChange={(e) => setNewPassword(e.target.value)}
            autoComplete="new-password"
            minLength={10}
            required
          />
          <p className="mt-1 text-xs text-text-subtle">Minimum 10 characters.</p>
        </div>
        <div>
          <label htmlFor="confirm-password" className="mb-1 block text-xs text-text-secondary">
            Confirm new password
          </label>
          <Input
            id="confirm-password"
            type="password"
            value={confirm}
            onChange={(e) => setConfirm(e.target.value)}
            autoComplete="new-password"
            minLength={10}
            required
          />
        </div>
        <div className="flex items-center gap-3 pt-2">
          <Button type="submit" loading={busy}>
            Change password
          </Button>
          <Link to="/account">
            <Button type="button" variant="secondary">
              Cancel
            </Button>
          </Link>
        </div>
      </form>
    </div>
  );
}
