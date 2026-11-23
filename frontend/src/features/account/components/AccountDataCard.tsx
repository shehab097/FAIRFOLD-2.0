import { useState } from 'react';
import { Download, Trash2 } from 'lucide-react';
import { useAuth } from '../../../app/auth/AuthContext';
import { deleteAccount, exportAccountData } from '../../../api/account';
import { Banner } from '../../../components/ui/Banner';
import { Button } from '../../../components/ui/Button';
import { Card } from '../../../components/ui/Card';
import { Icon } from '../../../components/ui/Icon';
import { Input } from '../../../components/ui/Input';
import { useToast } from '../../../components/ui/Toast';
import { getApiErrorMessage } from '../../../lib/errors';

export function AccountDataCard() {
  const { signOut } = useAuth();
  const { toast } = useToast();
  const [exporting, setExporting] = useState(false);
  const [password, setPassword] = useState('');
  const [confirmation, setConfirmation] = useState('');
  const [deleting, setDeleting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function download() {
    setExporting(true);
    setError(null);
    try {
      const { blob, filename } = await exportAccountData();
      const url = URL.createObjectURL(blob);
      const anchor = document.createElement('a');
      anchor.href = url;
      anchor.download = filename;
      document.body.appendChild(anchor);
      anchor.click();
      anchor.remove();
      URL.revokeObjectURL(url);
      toast('Your data export downloaded.', 'success');
    } catch (err) {
      setError(getApiErrorMessage(err));
    } finally {
      setExporting(false);
    }
  }

  async function remove() {
    setError(null);
    if (confirmation !== 'DELETE') {
      setError('Type DELETE exactly to confirm.');
      return;
    }
    setDeleting(true);
    try {
      await deleteAccount(password, confirmation);
      await signOut();
    } catch (err) {
      setError(getApiErrorMessage(err));
      setDeleting(false);
    }
  }

  return (
    <div className="space-y-6">
      <Card>
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div>
            <h2 className="text-base font-semibold">Your data</h2>
            <p className="mt-1 text-sm text-text-secondary">
              Download a JSON copy of your profile, skills, resumes, applications, practice history,
              messages and notifications.
            </p>
          </div>
          <Button variant="secondary" loading={exporting} onClick={download}>
            <Icon icon={Download} size={16} /> Download my data
          </Button>
        </div>
      </Card>

      <Card className="border-danger">
        <h2 className="text-base font-semibold text-danger">Delete my account</h2>
        <p className="mt-1 text-sm text-text-secondary">
          This permanently removes your account, profile and files. Audit rows stay with your
          identity cleared. The last admin and the last HR of a company cannot delete themselves.
        </p>
        <div className="mt-4 grid gap-3 sm:grid-cols-2">
          <label className="block">
            <span className="mb-1 block text-xs font-semibold uppercase tracking-[0.06em] text-text-secondary">
              Password
            </span>
            <Input
              type="password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              autoComplete="current-password"
            />
          </label>
          <label className="block">
            <span className="mb-1 block text-xs font-semibold uppercase tracking-[0.06em] text-text-secondary">
              Type DELETE to confirm
            </span>
            <Input
              value={confirmation}
              onChange={(event) => setConfirmation(event.target.value)}
              placeholder="DELETE"
            />
          </label>
        </div>
        {error && (
          <div className="mt-3">
            <Banner tone="error">{error}</Banner>
          </div>
        )}
        <div className="mt-4">
          <Button
            variant="danger"
            loading={deleting}
            disabled={!password || confirmation !== 'DELETE'}
            onClick={remove}
          >
            <Icon icon={Trash2} size={16} /> Delete my account
          </Button>
        </div>
      </Card>
    </div>
  );
}
