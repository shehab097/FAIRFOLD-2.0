import { useEffect, useState, type FormEvent } from 'react';
import { useCompany, useUpdateCompany } from '../queries';
import { COMPANY_SIZES } from '../../jobs/types';
import { Banner } from '../../../components/ui/Banner';
import { Button } from '../../../components/ui/Button';
import { Checkbox } from '../../../components/ui/Checkbox';
import { Input } from '../../../components/ui/Input';
import { Select } from '../../../components/ui/Select';
import { Skeleton } from '../../../components/ui/Skeleton';
import { useToast } from '../../../components/ui/Toast';
import { getApiErrorMessage } from '../../../lib/errors';

export function CompanyPage() {
  const { data: company, isLoading } = useCompany();
  const update = useUpdateCompany();
  const { toast } = useToast();

  const [name, setName] = useState('');
  const [industry, setIndustry] = useState('');
  const [size, setSize] = useState('');
  const [showName, setShowName] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (company) {
      setName(company.name ?? '');
      setIndustry(company.industry ?? '');
      setSize(company.size ?? '');
      setShowName(company.showNamePublicly);
    }
  }, [company]);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    try {
      await update.mutateAsync({ name, industry, size, showNamePublicly: showName });
      toast('Company profile saved.', 'success');
    } catch (err) {
      setError(getApiErrorMessage(err));
    }
  }

  if (isLoading) {
    return <Skeleton className="h-64 w-full" />;
  }

  return (
    <div>
      <h1 className="text-[28px] font-bold leading-9">Company profile</h1>
      <p className="mb-6 text-text-secondary">Required before you can activate a job.</p>

      {error && (
        <div className="mb-4">
          <Banner tone="error">{error}</Banner>
        </div>
      )}

      <form onSubmit={onSubmit} className="max-w-xl space-y-4 border border-border bg-surface p-6">
        <div>
          <label htmlFor="name" className="mb-1 block text-xs font-semibold uppercase tracking-[0.06em] text-text-secondary">
            Company name
          </label>
          <Input id="name" value={name} onChange={(e) => setName(e.target.value)} required />
        </div>
        <div>
          <label htmlFor="industry" className="mb-1 block text-xs font-semibold uppercase tracking-[0.06em] text-text-secondary">
            Industry
          </label>
          <Input id="industry" value={industry} onChange={(e) => setIndustry(e.target.value)} required />
        </div>
        <div>
          <label htmlFor="size" className="mb-1 block text-xs font-semibold uppercase tracking-[0.06em] text-text-secondary">
            Size
          </label>
          <Select id="size" value={size} onChange={(e) => setSize(e.target.value)} required>
            <option value="">Select size…</option>
            {COMPANY_SIZES.map((s) => (
              <option key={s} value={s}>
                {s}
              </option>
            ))}
          </Select>
        </div>
        <Checkbox checked={showName} onChange={setShowName} label="Show company name publicly on job posts" />
        <Button type="submit" loading={update.isPending}>
          Save
        </Button>
      </form>
    </div>
  );
}
