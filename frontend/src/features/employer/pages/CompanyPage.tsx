import { useEffect, useState, type FormEvent, type ReactNode } from 'react';
import { Building2, Check, Eye, EyeOff, Globe, Mail, MapPin, Pencil, Phone } from 'lucide-react';
import { useCompany, useUpdateCompany } from '../queries';
import { COMPANY_SIZES, type CompanyProfile } from '../../jobs/types';
import { Badge } from '../../../components/ui/Badge';
import { Banner } from '../../../components/ui/Banner';
import { Button } from '../../../components/ui/Button';
import { Checkbox } from '../../../components/ui/Checkbox';
import { Icon } from '../../../components/ui/Icon';
import { Input } from '../../../components/ui/Input';
import { Markdown } from '../../../components/ui/Markdown';
import { MarkdownEditor } from '../../../components/ui/MarkdownEditor';
import { Select } from '../../../components/ui/Select';
import { Skeleton } from '../../../components/ui/Skeleton';
import { Tabs } from '../../../components/ui/Tabs';
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
  { key: 'foundedYear', label: 'Founded year', type: 'number', placeholder: '2015' },
  { key: 'headquarters', label: 'Headquarters', placeholder: 'Dhaka, Bangladesh' },
];

const CONTACT: typeof BASICS = [
  { key: 'website', label: 'Website', type: 'url', placeholder: 'https://example.com' },
  { key: 'linkedinUrl', label: 'LinkedIn', type: 'url', placeholder: 'https://linkedin.com/company/…' },
  { key: 'logoUrl', label: 'Logo URL', type: 'url', placeholder: 'https://…/logo.png' },
  { key: 'contactEmail', label: 'Contact email', type: 'email', placeholder: 'talent@example.com' },
  { key: 'phone', label: 'Phone', type: 'tel', placeholder: '+880…' },
];

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

function DetailTile({ label, value }: { label: string; value: ReactNode }) {
  const empty = value === null || value === undefined || value === '';
  return (
    <div className="border border-border bg-bg p-3.5">
      <dt className="text-xs font-semibold uppercase tracking-[0.06em] text-text-secondary">{label}</dt>
      <dd className="mt-1 break-words text-sm font-medium text-text">
        {empty ? <span className="font-normal text-text-subtle">Not specified</span> : value}
      </dd>
    </div>
  );
}

function CompanyProfileView({ company, onEdit }: { company: CompanyProfile; onEdit: () => void }) {
  const initials = (company.name ?? 'CO')
    .split(/\s+/)
    .map((w) => w[0])
    .join('')
    .slice(0, 2)
    .toUpperCase();

  return (
    <div className="space-y-6">
      <section className="border border-border border-t-2 border-t-primary bg-surface">
        <div className="flex flex-col justify-between gap-4 border-b border-border bg-raised p-6 sm:flex-row sm:items-center">
          <div className="flex items-start gap-4">
            {company.logoUrl ? (
              <img
                src={company.logoUrl}
                alt={company.name ?? 'Company logo'}
                className="h-14 w-14 shrink-0 border border-border-strong bg-bg object-contain"
              />
            ) : (
              <div className="flex h-14 w-14 shrink-0 items-center justify-center border border-border-strong bg-primary-subtle text-lg font-bold text-primary-text">
                {initials}
              </div>
            )}
            <div className="min-w-0">
              <div className="flex flex-wrap items-center gap-2.5">
                <h2 className="text-xl font-bold text-text">{company.name || 'Unnamed company'}</h2>
                <Badge
                  tone={company.showNamePublicly ? 'success' : 'neutral'}
                  icon={company.showNamePublicly ? Eye : EyeOff}
                >
                  {company.showNamePublicly ? 'Public name' : 'Confidential name'}
                </Badge>
              </div>
              {company.tagline ? (
                <p className="mt-1 text-sm text-text-secondary">{company.tagline}</p>
              ) : (
                <p className="mt-1 text-sm text-text-subtle">No tagline added</p>
              )}
              <div className="mt-2 flex flex-wrap items-center gap-3 text-xs text-text-secondary">
                {company.industry && (
                  <span className="inline-flex items-center gap-1 border border-border bg-surface px-2 py-0.5">
                    <Icon icon={Building2} size={13} />
                    {company.industry}
                  </span>
                )}
                {company.size && (
                  <span className="border border-border bg-surface px-2 py-0.5">{company.size} employees</span>
                )}
                {company.headquarters && (
                  <span className="inline-flex items-center gap-1">
                    <Icon icon={MapPin} size={13} />
                    {company.headquarters}
                  </span>
                )}
              </div>
            </div>
          </div>
          <Button variant="secondary" onClick={onEdit}>
            <Icon icon={Pencil} size={15} />
            Edit details
          </Button>
        </div>

        <dl className="grid gap-3 p-6 sm:grid-cols-2 lg:grid-cols-3">
          <DetailTile label="1. Company name" value={company.name} />
          <DetailTile label="2. Industry" value={company.industry} />
          <DetailTile label="3. Company size" value={company.size ? `${company.size} employees` : null} />
          <DetailTile label="4. Tagline" value={company.tagline} />
          <DetailTile label="5. Founded year" value={company.foundedYear} />
          <DetailTile label="6. Headquarters" value={company.headquarters} />
          <DetailTile
            label="7. Public visibility"
            value={company.showNamePublicly ? 'Shown publicly on job posts' : 'Hidden (Confidential company)'}
          />
          <DetailTile
            label="8. Website"
            value={
              company.website ? (
                <a href={company.website} target="_blank" rel="noopener noreferrer" className="inline-flex items-center gap-1 text-primary-text underline">
                  <Icon icon={Globe} size={13} />
                  {company.website}
                </a>
              ) : null
            }
          />
          <DetailTile
            label="9. LinkedIn"
            value={
              company.linkedinUrl ? (
                <a href={company.linkedinUrl} target="_blank" rel="noopener noreferrer" className="text-primary-text underline">
                  {company.linkedinUrl}
                </a>
              ) : null
            }
          />
          <DetailTile
            label="10. Contact email"
            value={
              company.contactEmail ? (
                <a href={`mailto:${company.contactEmail}`} className="inline-flex items-center gap-1 text-primary-text underline">
                  <Icon icon={Mail} size={13} />
                  {company.contactEmail}
                </a>
              ) : null
            }
          />
          <DetailTile
            label="11. Phone"
            value={
              company.phone ? (
                <span className="inline-flex items-center gap-1">
                  <Icon icon={Phone} size={13} />
                  {company.phone}
                </span>
              ) : null
            }
          />
          <DetailTile
            label="12. Logo URL"
            value={
              company.logoUrl ? (
                <a href={company.logoUrl} target="_blank" rel="noopener noreferrer" className="text-primary-text underline">
                  {company.logoUrl}
                </a>
              ) : null
            }
          />
        </dl>
      </section>

      <div className="grid gap-6 lg:grid-cols-2">
        <section className="border border-border bg-surface">
          <div className="border-b border-border bg-raised px-6 py-3.5">
            <h3 className="text-xs font-semibold uppercase tracking-[0.06em] text-text-secondary">
              13. About the company
            </h3>
          </div>
          <div className="p-6">
            {company.about ? (
              <Markdown source={company.about} />
            ) : (
              <p className="text-sm text-text-subtle">No company overview written yet.</p>
            )}
          </div>
        </section>

        <section className="border border-border bg-surface">
          <div className="border-b border-border bg-raised px-6 py-3.5">
            <h3 className="text-xs font-semibold uppercase tracking-[0.06em] text-text-secondary">
              14. Benefits and perks
            </h3>
          </div>
          <div className="p-6">
            {company.benefits ? (
              <Markdown source={company.benefits} />
            ) : (
              <p className="text-sm text-text-subtle">No benefits or perks listed yet.</p>
            )}
          </div>
        </section>
      </div>
    </div>
  );
}

export function CompanyPage() {
  const { data: company, isLoading } = useCompany();
  const update = useUpdateCompany();
  const { toast } = useToast();
  const [form, setForm] = useState<Form>(EMPTY);
  const [tab, setTab] = useState<'view' | 'edit'>('view');
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
      setTab('view');
    } catch (err) {
      setError(getApiErrorMessage(err));
      setFieldErrors(getFieldErrors(err));
    }
  }

  if (isLoading) return <Skeleton className="h-64 w-full" />;

  const isComplete = Boolean(company?.name && company?.industry && company?.size);

  const inputs = (fields: typeof BASICS) =>
    fields.map((f) => (
      <Field key={f.key} label={f.label} error={fieldErrors[f.key]}>
        <Input
          type={f.type ?? 'text'}
          value={form[f.key] as string}
          onChange={(e) => set(f.key)(e.target.value)}
          required={f.required}
          placeholder={f.placeholder}
          error={Boolean(fieldErrors[f.key])}
        />
      </Field>
    ));

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-3 border-b border-border pb-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <p className="text-xs font-semibold uppercase tracking-[0.06em] text-primary-text">Employer Organisation</p>
          <div className="mt-1 flex flex-wrap items-center gap-3">
            <h1 className="text-[28px] font-bold leading-9">Company profile</h1>
            {isComplete ? (
              <Badge tone="success" icon={Check}>
                Complete
              </Badge>
            ) : (
              <Badge status="paused" />
            )}
          </div>
          <p className="text-text-secondary">
            Name, industry and size are required before you can activate a job. All 13 details are displayed below.
          </p>
        </div>
      </div>

      <Tabs
        tabs={[
          { key: 'view', label: 'Profile view' },
          { key: 'edit', label: 'Edit profile' },
        ]}
        active={tab}
        onChange={(k) => setTab(k as 'view' | 'edit')}
      />

      {tab === 'view' && company && <CompanyProfileView company={company} onEdit={() => setTab('edit')} />}

      {tab === 'edit' && (
        <>
          {error && <Banner tone="error">{error}</Banner>}

          <form onSubmit={onSubmit} className="space-y-6">
            <section className="border border-border border-t-2 border-t-primary bg-surface">
              <div className="border-b border-border bg-raised px-6 py-3.5">
                <h2 className="text-base font-semibold">Basics</h2>
              </div>
              <div className="space-y-4 p-6">
                <div className="grid gap-4 sm:grid-cols-2">
                  {inputs(BASICS.slice(0, 2))}
                  <Field label="Size" error={fieldErrors.size}>
                    <Select value={form.size} onChange={(e) => set('size')(e.target.value)} required error={Boolean(fieldErrors.size)}>
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
                <Checkbox
                  checked={form.showNamePublicly}
                  onChange={set('showNamePublicly')}
                  label="Show company name publicly on job posts"
                />
              </div>
            </section>

            <section className="border border-border bg-surface">
              <div className="border-b border-border bg-raised px-6 py-3.5">
                <h2 className="text-base font-semibold">Contact and links</h2>
              </div>
              <div className="grid gap-4 p-6 sm:grid-cols-2">{inputs(CONTACT)}</div>
            </section>

            <section className="border border-border bg-surface">
              <div className="border-b border-border bg-raised px-6 py-3.5">
                <h2 className="text-base font-semibold">About and benefits</h2>
              </div>
              <div className="space-y-4 p-6">
                <Field label="About the company (Markdown)" error={fieldErrors.about} wrap={false}>
                  <MarkdownEditor value={form.about} onChange={set('about')} rows={10} />
                </Field>
                <Field label="Benefits and perks (Markdown)" error={fieldErrors.benefits} wrap={false}>
                  <MarkdownEditor value={form.benefits} onChange={set('benefits')} rows={6} />
                </Field>
              </div>
            </section>

            <div className="flex items-center gap-3">
              <Button type="submit" loading={update.isPending}>
                Save
              </Button>
              <Button type="button" variant="secondary" onClick={() => setTab('view')}>
                View profile
              </Button>
            </div>
          </form>
        </>
      )}
    </div>
  );
}
