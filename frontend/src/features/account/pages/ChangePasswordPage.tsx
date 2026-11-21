import { useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { KeyRound, ShieldCheck } from 'lucide-react';
import { changePassword } from '../../../api/account';
import { Banner } from '../../../components/ui/Banner';
import { Button } from '../../../components/ui/Button';
import { Icon } from '../../../components/ui/Icon';
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
    <div className="space-y-6">
      <div className="flex flex-col gap-3 border-b border-border pb-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <Link to="/account" className="text-xs font-semibold uppercase tracking-[0.06em] text-primary-text hover:underline">
            ← Back to account profile
          </Link>
          <h1 className="mt-1 text-[28px] font-bold leading-9">Change password</h1>
          <p className="text-text-secondary">Update your account password. Use at least 10 characters.</p>
        </div>
      </div>

      {error && <Banner tone="error">{error}</Banner>}

      <div className="grid gap-6 lg:grid-cols-[minmax(0,520px)_1fr]">
        <form onSubmit={onSubmit} className="border border-border border-t-2 border-t-primary bg-surface">
          <div className="flex items-center gap-2 border-b border-border bg-raised px-6 py-3.5">
            <Icon icon={KeyRound} size={18} className="text-primary-text" />
            <h2 className="text-base font-semibold">Password credentials</h2>
          </div>
          <div className="space-y-4 p-6">
            <div>
              <label htmlFor="current-password" className="mb-1 block text-xs font-semibold uppercase tracking-[0.06em] text-text-secondary">
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
              <label htmlFor="new-password" className="mb-1 block text-xs font-semibold uppercase tracking-[0.06em] text-text-secondary">
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
              <label htmlFor="confirm-password" className="mb-1 block text-xs font-semibold uppercase tracking-[0.06em] text-text-secondary">
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
          </div>
        </form>

        <aside className="border border-border bg-surface p-6 lg:self-start">
          <div className="flex items-center gap-2 text-primary-text">
            <Icon icon={ShieldCheck} size={18} />
            <h2 className="text-xs font-semibold uppercase tracking-[0.06em]">Security requirements</h2>
          </div>
          <ul className="mt-3 space-y-2 text-sm text-text-secondary">
            <li className="border-l-2 border-primary pl-3">Must be at least 10 characters long.</li>
            <li className="border-l-2 border-border pl-3">Your current password is verified before any update is saved.</li>
            <li className="border-l-2 border-border pl-3">All password changes are recorded in the security audit log.</li>
          </ul>
        </aside>
      </div>
    </div>
  );
}

