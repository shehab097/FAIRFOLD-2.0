import { useEffect, useState, type FormEvent, type ReactNode } from 'react';
import { useCompany, useUpdateCompany } from '../queries';
import { COMPANY_SIZES, type CompanyProfile } from '../../jobs/types';
import { Banner } from '../../../components/ui/Banner';
import { Button } from '../../../components/ui/Button';
import { Checkbox } from '../../../components/ui/Checkbox';
import { Input } from '../../../components/ui/Input';
import { MarkdownEditor } from '../../../components/ui/MarkdownEditor';
import { Select } from '../../../components/ui/Select';
import { Skeleton } from '../../../components/ui/Skeleton';
import { useToast } from '../../../components/ui/Toast';
import { getApiErrorMessage, getFieldErrors } from '../../../lib/errors';

type TextKey = Exclude<keyof CompanyProfile, 'showNamePublicly' | 'foundedYear'>;
type Form = Record<TextKey | 'foundedYear', string> & { showNamePublicly: boolean };

const EMPTY: Form = {
  name: '', industry: '', size: '', showNamePublicly: false, tagline: '', about: '', website: '', logoUrl: '',
  linkedinUrl: '', foundedYear: '', headquarters: '', contactEmail: '', phone: '', benefits: '',
};

const BASICS: { key: keyof Form; label: string; type?: string; required?: boolean; placeholder?: string }[] = [
  { key: 'name', label: 'Company name', required: true },
  { key: 'industry', label: 'Industry', required: true },
  { key: 'tagline', label: 'Tagline', placeholder: 'One line about what you do' },
  { key: 'foundedYear', label: 'Founded', type: 'number', placeholder: '2015' },
  { key: 'headquarters', label: 'Headquarters', placeholder: 'Dhaka, Bangladesh' },
];

const CONTACT: typeof BASICS = [
  { key: 'website', label: 'Website', type: 'url', placeholder: 'https://example.com' },
  { key: 'linkedinUrl', label: 'LinkedIn', type: 'url', placeholder: 'https://linkedin.com/company/…' },
  { key: 'logoUrl', label: 'Logo URL', type: 'url', placeholder: 'https://…/logo.png' },
  { key: 'contactEmail', label: 'Contact email', type: 'email' },
  { key: 'phone', label: 'Phone', type: 'tel' },
];

// `wrap=false` for composite controls (a <label> would forward clicks to the first toolbar button).
function Field({ label, error, children, wrap = true }: { label: string; error?: string; children: ReactNode; wrap?: boolean }) {
  const Tag = wrap ? 'label' : 'div';
  return (
    <Tag className="block">
      <span className="mb-1 block text-xs font-semibold uppercase tracking-[0.06em] text-text-secondary">{label}</span>
      {children}
      {error && <span className="mt-1 block text-sm text-accent">{error}</span>}
    </Tag>
  );
}

export function CompanyPage() {
  const { data: company, isLoading } = useCompany();
  const update = useUpdateCompany();
  const { toast } = useToast();
  const [form, setForm] = useState<Form>(EMPTY);
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    if (company) {
      const next = { ...EMPTY };
      for (const k of Object.keys(EMPTY) as (keyof Form)[]) {
        const v = company[k];
        if (v !== null && v !== undefined) (next as Record<string, unknown>)[k] = k === 'showNamePublicly' ? v : String(v);
      }
      setForm(next);
    }
  }, [company]);

  const set = (key: keyof Form) => (value: string | boolean) => setForm((f) => ({ ...f, [key]: value }));

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setFieldErrors({});
    try {
      await update.mutateAsync({ ...form, foundedYear: form.foundedYear === '' ? null : Number(form.foundedYear) });
      toast('Company profile saved.', 'success');
    } catch (err) {
      setError(getApiErrorMessage(err));
      setFieldErrors(getFieldErrors(err));
    }
  }

  if (isLoading) return <Skeleton className="h-64 w-full" />;

  const inputs = (fields: typeof BASICS) =>
    fields.map((f) => (
      <Field key={f.key} label={f.label} error={fieldErrors[f.key]}>
        <Input
          type={f.type ?? 'text'}
          value={form[f.key] as string}
          onChange={(e) => set(f.key)(e.target.value)}
          required={f.required}
          placeholder={f.placeholder}
        />
      </Field>
    ));

  return (
    <div>
      <h1 className="text-[28px] font-bold leading-9">Company profile</h1>
      <p className="mb-6 text-text-secondary">Name, industry and size are required before you can activate a job.</p>

      {error && (
        <div className="mb-4">
          <Banner tone="error">{error}</Banner>
        </div>
      )}

      <form onSubmit={onSubmit} className="max-w-4xl space-y-6">
        <section className="space-y-4 border border-border bg-surface p-6">
          <h2 className="text-xl font-semibold">Basics</h2>
          <div className="grid gap-4 sm:grid-cols-2">
            {inputs(BASICS.slice(0, 2))}
            <Field label="Size" error={fieldErrors.size}>
              <Select value={form.size} onChange={(e) => set('size')(e.target.value)} required>
                <option value="">Select size…</option>
                {COMPANY_SIZES.map((s) => (
                  <option key={s} value={s}>
                    {s}
                  </option>
                ))}
              </Select>
            </Field>
            {inputs(BASICS.slice(2))}
          </div>
          <Checkbox checked={form.showNamePublicly} onChange={set('showNamePublicly')} label="Show company name publicly on job posts" />
        </section>

        <section className="space-y-4 border border-border bg-surface p-6">
          <h2 className="text-xl font-semibold">Contact and links</h2>
          <div className="grid gap-4 sm:grid-cols-2">{inputs(CONTACT)}</div>
        </section>

        <section className="space-y-4 border border-border bg-surface p-6">
          <h2 className="text-xl font-semibold">About and benefits</h2>
          <Field label="About the company (Markdown)" error={fieldErrors.about} wrap={false}>
            <MarkdownEditor value={form.about} onChange={set('about')} rows={10} />
          </Field>
          <Field label="Benefits and perks (Markdown)" error={fieldErrors.benefits} wrap={false}>
            <MarkdownEditor value={form.benefits} onChange={set('benefits')} rows={6} />
          </Field>
        </section>

        <Button type="submit" loading={update.isPending}>
          Save
        </Button>
      </form>
    </div>
  );
}
