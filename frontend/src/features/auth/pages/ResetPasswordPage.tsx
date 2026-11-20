import { useState, type FormEvent } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { resetPassword } from '../../../api/auth';
import { Banner } from '../../../components/ui/Banner';
import { Button } from '../../../components/ui/Button';
import { Input } from '../../../components/ui/Input';
import { getApiErrorMessage } from '../../../lib/errors';

export function ResetPasswordPage() {
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const token = params.get('token') ?? '';

  const [newPassword, setNewPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    if (newPassword !== confirm) {
      setError('Passwords do not match.');
      return;
    }
    setLoading(true);
    try {
      await resetPassword(token, newPassword);
      navigate('/login', { replace: true });
    } catch (err) {
      setError(getApiErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }

  if (!token) {
    return (
      <div>
        <h1 className="text-[28px] font-bold leading-9">Reset password</h1>
        <div className="mt-6">
          <Banner tone="error">This reset link is invalid or has expired.</Banner>
        </div>
      </div>
    );
  }

  return (
    <div>
      <h1 className="text-[28px] font-bold leading-9">Choose a new password</h1>
      <p className="mb-6 text-text-secondary">At least 10 characters.</p>

      {error && (
        <div className="mb-4">
          <Banner tone="error">{error}</Banner>
        </div>
      )}

      <form onSubmit={onSubmit} className="space-y-4" noValidate>
        <div>
          <label htmlFor="newPassword" className="mb-1 block text-xs font-semibold uppercase tracking-[0.06em] text-text-secondary">
            New password
          </label>
          <Input id="newPassword" type="password" value={newPassword} onChange={(e) => setNewPassword(e.target.value)} minLength={10} autoComplete="new-password" required />
        </div>
        <div>
          <label htmlFor="confirm" className="mb-1 block text-xs font-semibold uppercase tracking-[0.06em] text-text-secondary">
            Confirm password
          </label>
          <Input id="confirm" type="password" value={confirm} onChange={(e) => setConfirm(e.target.value)} minLength={10} autoComplete="new-password" required />
        </div>
        <Button type="submit" loading={loading} className="w-full">
          Reset password
        </Button>
      </form>

      <p className="mt-4 text-sm text-text-secondary">
        <Link to="/login" className="text-primary-text hover:underline">
          Back to log in
        </Link>
      </p>
    </div>
  );
}
