import { useEffect, useState, type FormEvent } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { getInviteInfo, type InviteInfo } from '../../../api/auth';
import { useAuth } from '../../../app/auth/AuthContext';
import { Banner } from '../../../components/ui/Banner';
import { Button } from '../../../components/ui/Button';
import { Checkbox } from '../../../components/ui/Checkbox';
import { Input } from '../../../components/ui/Input';
import { Skeleton } from '../../../components/ui/Skeleton';
import { getApiErrorMessage } from '../../../lib/errors';

export function AcceptInvitePage() {
  const [params] = useSearchParams();
  const token = params.get('token') ?? '';
  const navigate = useNavigate();
  const { acceptInvite } = useAuth();

  const [info, setInfo] = useState<InviteInfo | null>(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [password, setPassword] = useState('');
  const [consent, setConsent] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    let active = true;
    if (!token) {
      setLoadError('This invite link is invalid or has expired.');
      setLoading(false);
      return;
    }
    getInviteInfo(token)
      .then((data) => {
        if (active) setInfo(data);
      })
      .catch((err) => {
        if (active) setLoadError(getApiErrorMessage(err));
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [token]);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setBusy(true);
    try {
      await acceptInvite({ token, firstName, lastName, password, consent });
      navigate('/', { replace: true });
    } catch (err) {
      setError(getApiErrorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  if (loading) return <Skeleton className="h-72 w-full rounded-xl" />;
  if (loadError || !info) {
    return (
      <div className="space-y-4">
        <Banner tone="error">{loadError ?? 'This invite link is invalid or has expired.'}</Banner>
        <Link to="/login" className="text-sm text-primary-text hover:underline">
          Back to sign in
        </Link>
      </div>
    );
  }

  return (
    <div>
      <h1 className="text-[28px] font-bold leading-9">Join {info.companyName ?? 'the team'}</h1>
      <p className="mb-6 text-text-secondary">
        You were invited as <span className="font-semibold text-text">{info.role.replace('_', ' ')}</span>{' '}
        for {info.email}.
      </p>

      {error && (
        <div className="mb-4">
          <Banner tone="error">{error}</Banner>
        </div>
      )}

      <form onSubmit={onSubmit} className="space-y-4" noValidate>
        <div className="grid grid-cols-2 gap-4">
          <label className="block">
            <span className="mb-1 block text-xs font-semibold uppercase tracking-[0.06em] text-text-secondary">
              First name
            </span>
            <Input value={firstName} onChange={(e) => setFirstName(e.target.value)} required />
          </label>
          <label className="block">
            <span className="mb-1 block text-xs font-semibold uppercase tracking-[0.06em] text-text-secondary">
              Last name
            </span>
            <Input value={lastName} onChange={(e) => setLastName(e.target.value)} required />
          </label>
        </div>
        <label className="block">
          <span className="mb-1 block text-xs font-semibold uppercase tracking-[0.06em] text-text-secondary">
            Password
          </span>
          <Input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            minLength={10}
            autoComplete="new-password"
            required
          />
          <span className="mt-1 block text-xs text-text-subtle">Minimum 10 characters.</span>
        </label>
        <Checkbox
          checked={consent}
          onChange={setConsent}
          label="I agree to the terms and privacy policy"
        />
        <Button type="submit" loading={busy} disabled={!consent}>
          Accept invite and sign in
        </Button>
      </form>
    </div>
  );
}
