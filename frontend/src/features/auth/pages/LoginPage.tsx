import { useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { ArrowRight, Eye, EyeOff, Lock, ShieldCheck } from 'lucide-react';
import { useAuth } from '../../../app/auth/AuthContext';
import { Banner } from '../../../components/ui/Banner';
import { Button } from '../../../components/ui/Button';
import { Icon } from '../../../components/ui/Icon';
import { Input } from '../../../components/ui/Input';
import { getApiErrorCode, getApiErrorMessage } from '../../../lib/errors';

export function LoginPage() {
  const { signIn } = useAuth();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
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
      <Link to="/" className="inline-flex items-center gap-1.5 text-xs text-text-secondary hover:text-primary-text">
        ← Back to home
      </Link>

      <div className="mt-5 inline-flex items-center gap-2 rounded-full bg-primary-subtle px-3 py-1 text-xs font-semibold uppercase tracking-[0.08em] text-primary-text">
        <Icon icon={ShieldCheck} size={14} />
        Secure sign-in
      </div>
      <h1 className="mt-4 text-[30px] font-bold leading-tight">Welcome back.</h1>
      <p className="mt-1.5 text-sm text-text-secondary">
        Log in to continue your fair hiring journey.
      </p>

      {error && (
        <div className="mt-5">
          <Banner tone={errorCode === 'ACCOUNT_LOCKED' ? 'warning' : 'error'}>{error}</Banner>
        </div>
      )}

      <form onSubmit={onSubmit} className="mt-6 space-y-4" noValidate>
        <div>
          <label htmlFor="email" className="mb-1 block text-xs font-semibold uppercase tracking-[0.06em] text-text-secondary">
            Email address
          </label>
          <Input
            id="email"
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="you@example.com"
            autoComplete="email"
            disabled={loading}
            required
          />
        </div>

        <div>
          <div className="mb-1 flex items-center justify-between gap-3">
            <label htmlFor="password" className="block text-xs font-semibold uppercase tracking-[0.06em] text-text-secondary">
              Password
            </label>
            <Link to="/forgot-password" className="text-xs font-medium text-primary-text hover:underline">
              Forgot password?
            </Link>
          </div>
          <div className="relative">
            <Input
              id="password"
              type={showPassword ? 'text' : 'password'}
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="Enter your password"
              autoComplete="current-password"
              disabled={loading}
              className="pr-11"
              required
            />
            <button
              type="button"
              onClick={() => setShowPassword((value) => !value)}
              aria-label={showPassword ? 'Hide password' : 'Show password'}
              className="absolute right-1.5 top-1/2 flex h-8 w-8 -translate-y-1/2 items-center justify-center rounded-md text-text-secondary transition-colors hover:bg-raised hover:text-text"
            >
              <Icon icon={showPassword ? EyeOff : Eye} size={16} />
            </button>
          </div>
        </div>

        <Button type="submit" loading={loading} className="w-full">
          Log in
          <Icon icon={ArrowRight} size={16} />
        </Button>
      </form>

      <div className="my-5 flex items-center gap-3">
        <div className="h-px flex-1 bg-border" />
        <span className="text-[11px] font-semibold uppercase tracking-[0.08em] text-text-subtle">
          New to FairFold?
        </span>
        <div className="h-px flex-1 bg-border" />
      </div>

      <Link to="/register">
        <Button variant="secondary" className="w-full">
          Create an account
        </Button>
      </Link>

      <p className="mt-5 flex items-start gap-2 text-xs leading-5 text-text-secondary">
        <Icon icon={Lock} size={14} className="mt-0.5 shrink-0 text-primary-text" />
        Your identity stays private during screening. Only your experience and evidence are reviewed.
      </p>
    </div>
  );
}
