import { useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { Eye, EyeOff, Lock, Mail } from 'lucide-react';
import { useAuth } from '../../../app/auth/AuthContext';
import { Banner } from '../../../components/ui/Banner';
import { Button } from '../../../components/ui/Button';
import { CodeInput } from '../../../components/ui/CodeInput';
import { Icon } from '../../../components/ui/Icon';
import { Input } from '../../../components/ui/Input';
import { getApiErrorCode, getApiErrorDetails, getApiErrorMessage } from '../../../lib/errors';

export function LoginPage() {
  const { signIn, verifyMfa } = useAuth();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [mfaToken, setMfaToken] = useState<string | null>(null);
  const [code, setCode] = useState('');
  const [triesLeft, setTriesLeft] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [errorCode, setErrorCode] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setErrorCode(null);
    setLoading(true);
    try {
      const result = await signIn(email, password);
      if ('mfaRequired' in result) {
        setMfaToken(result.mfaToken);
        setCode('');
      }
    } catch (err) {
      setErrorCode(getApiErrorCode(err));
      setError(getApiErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }

  async function onVerify(e: FormEvent) {
    e.preventDefault();
    if (!mfaToken) return;
    setError(null);
    setErrorCode(null);
    setTriesLeft(null);
    setLoading(true);
    try {
      await verifyMfa(mfaToken, code);
    } catch (err) {
      setErrorCode(getApiErrorCode(err));
      setError(getApiErrorMessage(err));
      const details = getApiErrorDetails(err);
      if (typeof details.triesLeft === 'number') {
        setTriesLeft(details.triesLeft);
      }
      setCode('');
    } finally {
      setLoading(false);
    }
  }

  if (mfaToken) {
    return (
      <div>
        <h1 className="text-[28px] font-bold leading-tight">Two-step login</h1>
        <p className="mt-1 text-sm text-text-secondary">
          Enter the 6-digit code from your authenticator app for {email}.
        </p>
        {error && (
          <div className="mt-6">
            <Banner tone={errorCode === 'ACCOUNT_LOCKED' ? 'warning' : 'error'}>{error}</Banner>
          </div>
        )}
        <form onSubmit={onVerify} className="mt-8 space-y-5">
          <CodeInput value={code} onChange={setCode} disabled={loading} autoFocus />
          {triesLeft !== null && (
            <p className="text-sm text-text-secondary">{triesLeft} tries left.</p>
          )}
          <Button type="submit" loading={loading} disabled={code.length !== 6} className="w-full">
            Verify and log in
          </Button>
          <button
            type="button"
            className="w-full text-center text-sm font-medium text-primary-text hover:underline"
            onClick={() => {
              setMfaToken(null);
              setCode('');
              setError(null);
              setTriesLeft(null);
            }}
          >
            Use a different account
          </button>
        </form>
      </div>
    );
  }

  return (
    <div>
      <h1 className="text-[28px] font-bold leading-tight">Log in</h1>
      <p className="mt-1 text-sm text-text-secondary">Welcome back to FairFold.</p>

      {error && (
        <div className="mt-6">
          <Banner tone={errorCode === 'ACCOUNT_LOCKED' ? 'warning' : 'error'}>{error}</Banner>
        </div>
      )}

      <form onSubmit={onSubmit} className="mt-8 space-y-5" noValidate>
        <div>
          <label htmlFor="email" className="mb-1.5 block text-sm font-medium text-text">
            Email
          </label>
          <div className="relative">
            <Icon
              icon={Mail}
              size={16}
              className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-text-subtle"
            />
            <Input
              id="email"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="you@example.com"
              autoComplete="email"
              disabled={loading}
              className="pl-9"
              autoFocus
              required
            />
          </div>
        </div>

        <div>
          <div className="mb-1.5 flex items-center justify-between gap-3">
            <label htmlFor="password" className="block text-sm font-medium text-text">
              Password
            </label>
            <Link to="/forgot-password" className="text-xs font-medium text-primary-text hover:underline">
              Forgot password?
            </Link>
          </div>
          <div className="relative">
            <Icon
              icon={Lock}
              size={16}
              className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-text-subtle"
            />
            <Input
              id="password"
              type={showPassword ? 'text' : 'password'}
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="••••••••"
              autoComplete="current-password"
              disabled={loading}
              className="pl-9 pr-11"
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
        </Button>
      </form>

      <p className="mt-8 text-center text-sm text-text-secondary">
        New to FairFold?{' '}
        <Link to="/register" className="font-semibold text-primary-text hover:underline">
          Create an account
        </Link>
      </p>
    </div>
  );
}
