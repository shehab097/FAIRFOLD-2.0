import { useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../../app/auth/AuthContext';
import { Banner } from '../../../components/ui/Banner';
import { Button } from '../../../components/ui/Button';
import { Checkbox } from '../../../components/ui/Checkbox';
import { Input } from '../../../components/ui/Input';
import { Select } from '../../../components/ui/Select';
import { getApiErrorMessage } from '../../../lib/errors';

export function RegisterPage() {
  const { register } = useAuth();
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [role, setRole] = useState<'candidate' | 'employer'>('candidate');
  const [consent, setConsent] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      await register({ email, password, firstName, lastName, role, consent });
    } catch (err) {
      setError(getApiErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }

  return (
    <div>
      <h1 className="text-[28px] font-bold leading-9">Create an account</h1>
      <p className="mb-6 text-text-secondary">As a candidate or an employer.</p>

      {error && (
        <div className="mb-4">
          <Banner tone="error">{error}</Banner>
        </div>
      )}

      <form onSubmit={onSubmit} className="space-y-4" noValidate>
        <div className="grid grid-cols-2 gap-4">
          <div>
            <label htmlFor="firstName" className="mb-1 block text-xs font-semibold uppercase tracking-[0.06em] text-text-secondary">
              First name
            </label>
            <Input id="firstName" value={firstName} onChange={(e) => setFirstName(e.target.value)} autoComplete="given-name" required />
          </div>
          <div>
            <label htmlFor="lastName" className="mb-1 block text-xs font-semibold uppercase tracking-[0.06em] text-text-secondary">
              Last name
            </label>
            <Input id="lastName" value={lastName} onChange={(e) => setLastName(e.target.value)} autoComplete="family-name" required />
          </div>
        </div>

        <div>
          <label htmlFor="email" className="mb-1 block text-xs font-semibold uppercase tracking-[0.06em] text-text-secondary">
            Email
          </label>
          <Input id="email" type="email" value={email} onChange={(e) => setEmail(e.target.value)} autoComplete="email" required />
        </div>

        <div>
          <label htmlFor="password" className="mb-1 block text-xs font-semibold uppercase tracking-[0.06em] text-text-secondary">
            Password
          </label>
          <Input id="password" type="password" value={password} onChange={(e) => setPassword(e.target.value)} minLength={10} autoComplete="new-password" required />
          <p className="mt-1 text-xs text-text-subtle">Minimum 10 characters.</p>
        </div>

        <div>
          <label htmlFor="role" className="mb-1 block text-xs font-semibold uppercase tracking-[0.06em] text-text-secondary">
            I am a…
          </label>
          <Select id="role" value={role} onChange={(e) => setRole(e.target.value as 'candidate' | 'employer')}>
            <option value="candidate">Candidate</option>
            <option value="employer">Employer</option>
          </Select>
        </div>

        <Checkbox
          checked={consent}
          onChange={setConsent}
          label="I agree to the terms and consent to the processing of my data."
        />

        <Button type="submit" loading={loading} disabled={!consent} className="w-full">
          Create account
        </Button>
      </form>

      <p className="mt-4 text-sm text-text-secondary">
        Already have an account?{' '}
        <Link to="/login" className="text-primary-text hover:underline">
          Log in
        </Link>
      </p>
    </div>
  );
}
