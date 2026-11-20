import { useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../../app/auth/AuthContext';
import { Banner } from '../../../components/ui/Banner';
import { Button } from '../../../components/ui/Button';
import { Input } from '../../../components/ui/Input';
import { getApiErrorCode, getApiErrorMessage } from '../../../lib/errors';

export function LoginPage() {
  const { signIn } = useAuth();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [errorCode, setErrorCode] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setErrorCode(null);
    setLoading(true);
    try {
      await signIn(email, password);
    } catch (err) {
      setErrorCode(getApiErrorCode(err));
      setError(getApiErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }

  return (
    <div>
      <h1 className="text-[28px] font-bold leading-9">Log in</h1>
      <p className="mb-6 text-text-secondary">Welcome back.</p>

      {error && (
        <div className="mb-4">
          <Banner tone={errorCode === 'ACCOUNT_LOCKED' ? 'warning' : 'error'}>{error}</Banner>
        </div>
      )}

      <form onSubmit={onSubmit} className="space-y-4" noValidate>
        <div>
          <label htmlFor="email" className="mb-1 block text-xs font-semibold uppercase tracking-[0.06em] text-text-secondary">
            Email
          </label>
          <Input
            id="email"
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            autoComplete="email"
            required
          />
        </div>
        <div>
          <label htmlFor="password" className="mb-1 block text-xs font-semibold uppercase tracking-[0.06em] text-text-secondary">
            Password
          </label>
          <Input
            id="password"
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            autoComplete="current-password"
            required
          />
        </div>
        <Button type="submit" loading={loading} className="w-full">
          Log in
        </Button>
      </form>

      <div className="mt-4 flex justify-between text-sm">
        <Link to="/forgot-password" className="text-primary-text hover:underline">
          Forgot password?
        </Link>
        <Link to="/register" className="text-primary-text hover:underline">
          Register
        </Link>
      </div>
    </div>
  );
}
