import { useState } from 'react';
import { ShieldCheck, ShieldOff } from 'lucide-react';
import { useAuth } from '../../../app/auth/AuthContext';
import { disableMfa, enableMfa, setupMfa, type MfaSetup } from '../../../api/account';
import { Badge } from '../../../components/ui/Badge';
import { Banner } from '../../../components/ui/Banner';
import { Button } from '../../../components/ui/Button';
import { Card } from '../../../components/ui/Card';
import { CodeInput } from '../../../components/ui/CodeInput';
import { Dialog } from '../../../components/ui/Dialog';
import { Input } from '../../../components/ui/Input';
import { useToast } from '../../../components/ui/Toast';
import { getApiErrorMessage } from '../../../lib/errors';

export function AccountMfaCard() {
  const { user, updateUser } = useAuth();
  const { toast } = useToast();
  const [setup, setSetup] = useState<MfaSetup | null>(null);
  const [disableOpen, setDisableOpen] = useState(false);
  const [code, setCode] = useState('');
  const [password, setPassword] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (!user) return null;

  async function startSetup() {
    setError(null);
    setBusy(true);
    try {
      setSetup(await setupMfa());
      setCode('');
    } catch (err) {
      setError(getApiErrorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  async function confirmSetup() {
    setError(null);
    setBusy(true);
    try {
      await enableMfa(code);
      updateUser({ ...user!, mfaEnabled: true });
      setSetup(null);
      setCode('');
      toast('Two-step login is on.', 'success');
    } catch (err) {
      setError(getApiErrorMessage(err));
      setCode('');
    } finally {
      setBusy(false);
    }
  }

  async function confirmDisable() {
    setError(null);
    setBusy(true);
    try {
      await disableMfa(password, code);
      updateUser({ ...user!, mfaEnabled: false });
      setDisableOpen(false);
      setPassword('');
      setCode('');
      toast('Two-step login is off.', 'success');
    } catch (err) {
      setError(getApiErrorMessage(err));
      setCode('');
    } finally {
      setBusy(false);
    }
  }

  return (
    <Card>
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h2 className="flex items-center gap-2 text-base font-semibold">
            Two-step login (MFA)
            {user.mfaEnabled ? (
              <Badge tone="success" icon={ShieldCheck}>
                On
              </Badge>
            ) : (
              <Badge tone="neutral" icon={ShieldOff}>
                Off
              </Badge>
            )}
          </h2>
          <p className="mt-1 text-sm text-text-secondary">
            Protect your account with a 6-digit authenticator code after your password.
          </p>
        </div>
        {user.mfaEnabled ? (
          <Button
            variant="secondary"
            onClick={() => {
              setError(null);
              setCode('');
              setPassword('');
              setDisableOpen(true);
            }}
          >
            Turn off
          </Button>
        ) : (
          <Button loading={busy} onClick={startSetup}>
            Set up MFA
          </Button>
        )}
      </div>
      {error && !setup && !disableOpen && (
        <div className="mt-3">
          <Banner tone="error">{error}</Banner>
        </div>
      )}

      <Dialog open={Boolean(setup)} title="Set up two-step login" onClose={() => setSetup(null)}>
        {setup && (
          <div className="space-y-4">
            <p className="text-sm text-text-secondary">
              Scan this QR code with your authenticator app, then enter the 6-digit code to confirm.
            </p>
            <div className="flex flex-col items-center gap-3 rounded-xl border border-border bg-bg p-4">
              <img src={setup.qrDataUri} alt="MFA QR code" className="h-48 w-48" />
              <div className="w-full text-center">
                <div className="text-xs font-semibold uppercase tracking-[0.06em] text-text-secondary">
                  Manual secret
                </div>
                <code className="mt-1 block break-all text-xs text-text">{setup.secret}</code>
              </div>
            </div>
            <CodeInput value={code} onChange={setCode} disabled={busy} autoFocus />
            {error && <Banner tone="error">{error}</Banner>}
            <div className="flex justify-end gap-2">
              <Button variant="secondary" onClick={() => setSetup(null)}>
                Cancel
              </Button>
              <Button loading={busy} disabled={code.length !== 6} onClick={confirmSetup}>
                Turn on MFA
              </Button>
            </div>
          </div>
        )}
      </Dialog>

      <Dialog
        open={disableOpen}
        title="Turn off two-step login"
        onClose={() => setDisableOpen(false)}
      >
        <div className="space-y-4">
          <p className="text-sm text-text-secondary">
            Enter your password and a current authenticator code.
          </p>
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
          <CodeInput value={code} onChange={setCode} disabled={busy} />
          {error && <Banner tone="error">{error}</Banner>}
          <div className="flex justify-end gap-2">
            <Button variant="secondary" onClick={() => setDisableOpen(false)}>
              Cancel
            </Button>
            <Button
              variant="danger"
              loading={busy}
              disabled={!password || code.length !== 6}
              onClick={confirmDisable}
            >
              Turn off MFA
            </Button>
          </div>
        </div>
      </Dialog>
    </Card>
  );
}
