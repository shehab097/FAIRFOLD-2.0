import { useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { forgotPassword } from '../../../api/auth';
import { Banner } from '../../../components/ui/Banner';
import { Button } from '../../../components/ui/Button';
import { Input } from '../../../components/ui/Input';

export function ForgotPasswordPage() {
  const [email, setEmail] = useState('');
  const [sent, setSent] = useState(false);
  const [loading, setLoading] = useState(false);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setLoading(true);
    try {
      await forgotPassword(email);
      setSent(true);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div>
      <h1 className="text-[28px] font-bold leading-9">Reset password</h1>
      <p className="mb-6 text-text-secondary">Enter your email and we will send a link.</p>

      {sent ? (
        <Banner tone="info">If an account exists for that email, we sent a reset link.</Banner>
      ) : (
        <form onSubmit={onSubmit} className="space-y-4" noValidate>
          <div>
            <label htmlFor="email" className="mb-1 block text-xs font-semibold uppercase tracking-[0.06em] text-text-secondary">
              Email
            </label>
            <Input id="email" type="email" value={email} onChange={(e) => setEmail(e.target.value)} autoComplete="email" required />
          </div>
          <Button type="submit" loading={loading} className="w-full">
            Send reset link
          </Button>
        </form>
      )}

      <p className="mt-4 text-sm text-text-secondary">
        <Link to="/login" className="text-primary-text hover:underline">
          Back to log in
        </Link>
      </p>
    </div>
  );
}
