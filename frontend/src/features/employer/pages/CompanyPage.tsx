import { useEffect, useRef, useState, type FormEvent, type ReactNode } from 'react';
import { Link } from 'react-router-dom';
import { Building2, Check, Eye, EyeOff, Globe, KeyRound, LogOut, Mail, MapPin, Pencil, Phone, Trash2, Upload } from 'lucide-react';
import { useAuth } from '../../../app/auth/AuthContext';
import { useCompany, useDeleteCompanyLogo, useUpdateCompany, useUploadCompanyLogo } from '../queries';
import { COMPANY_SIZES, type CompanyProfile } from '../../jobs/types';
import { Badge } from '../../../components/ui/Badge';
import { Banner } from '../../../components/ui/Banner';
import { Button } from '../../../components/ui/Button';
import { Checkbox } from '../../../components/ui/Checkbox';
import { Icon } from '../../../components/ui/Icon';
import { Input } from '../../../components/ui/Input';
import { LocationFields } from '../../../components/ui/LocationFields';
import { Markdown } from '../../../components/ui/Markdown';
import { MarkdownEditor } from '../../../components/ui/MarkdownEditor';
import { Select } from '../../../components/ui/Select';
import { Skeleton } from '../../../components/ui/Skeleton';
import { Tabs } from '../../../components/ui/Tabs';
import { useToast } from '../../../components/ui/Toast';
import { getApiErrorMessage, getFieldErrors } from '../../../lib/errors';
import { emptyLocation, formatLocation, parseLocation, type LocationValue } from '../../../lib/locations';

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
];

const CONTACT: typeof BASICS = [
  { key: 'website', label: 'Website', type: 'url', placeholder: 'https://example.com' },
  { key: 'linkedinUrl', label: 'LinkedIn', type: 'url', placeholder: 'https://linkedin.com/company/…' },
  { key: 'contactEmail', label: 'Contact email', type: 'email', placeholder: 'talent@example.com' },
  { key: 'phone', label: 'Phone', type: 'tel', placeholder: '+880…' },
];

function Field({ label, error, children, wrap = true }: { label: string; error?: string; children: ReactNode; wrap?: boolean }) {
  const Tag = wrap ? 'label' : 'div';
  return (
    <Tag className="block">
      <span className="mb-1 block text-xs text-text-secondary">{label}</span>
      {children}
      {error && <span className="mt-1 block text-sm text-accent">{error}</span>}
    </Tag>
  );
}

function DetailItem({ label, value }: { label: string; value: ReactNode }) {
  const empty = value === null || value === undefined || value === '';
  return (
    <div>
      <dt className="text-xs text-text-secondary">{label}</dt>
      <dd className="mt-0.5 break-words text-sm font-medium text-text">
        {empty ? <span className="font-normal text-text-subtle">Not specified</span> : value}
      </dd>
    </div>
  );
}

function CompanyProfileView({
  company,
  onEdit,
  onUploadLogoClick,
  isUploadingLogo,
}: {
  company: CompanyProfile;
  onEdit: () => void;
  onUploadLogoClick: () => void;
  isUploadingLogo: boolean;
}) {
  const initials = (company.name ?? 'CO')
    .split(/\s+/)
    .map((w) => w[0])
    .join('')
    .slice(0, 2)
    .toUpperCase();

  return (
    <div className="divide-y divide-border rounded-xl border border-border bg-surface p-6 shadow-sm sm:p-8">
      <div className="flex flex-col justify-between gap-4 pb-6 sm:flex-row sm:items-center">
        <div className="flex items-start gap-4">
          <div className="relative flex h-16 w-16 shrink-0 items-center justify-center overflow-hidden rounded-xl border border-border bg-surface p-1 shadow-sm">
            {company.logoUrl ? (
              <img
                src={company.logoUrl}
                alt={company.name ?? 'Company logo'}
                className="h-full w-full object-contain"
              />
            ) : (
              <div className="flex h-full w-full items-center justify-center bg-primary-subtle text-xl font-bold text-primary-text">
                {initials}
              </div>
            )}
          </div>
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
            <p className="mt-1 text-sm text-text-secondary">{company.tagline || 'No tagline added'}</p>
            <div className="mt-2 flex flex-wrap items-center gap-3 text-xs text-text-secondary">
              {company.industry && (
                <span className="inline-flex items-center gap-1">
                  <Icon icon={Building2} size={13} />
                  {company.industry}
                </span>
              )}
              {company.size && <span>· {company.size} employees</span>}
              {company.headquarters && (
                <span className="inline-flex items-center gap-1">
                  · <Icon icon={MapPin} size={13} />
                  {company.headquarters}
                </span>
              )}
            </div>
          </div>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          <Button variant="secondary" onClick={onUploadLogoClick} loading={isUploadingLogo}>
            <Icon icon={Upload} size={15} />
            {company.logoUrl ? 'Change logo' : 'Upload logo'}
          </Button>
          <Button variant="secondary" onClick={onEdit}>
            <Icon icon={Pencil} size={15} />
            Edit profile
          </Button>
        </div>
      </div>

      <dl className="grid gap-y-5 gap-x-8 py-6 sm:grid-cols-2 lg:grid-cols-3">
        <DetailItem label="Company name" value={company.name} />
        <DetailItem label="Industry" value={company.industry} />
        <DetailItem label="Company size" value={company.size ? `${company.size} employees` : null} />
        <DetailItem label="Tagline" value={company.tagline} />
        <DetailItem label="Founded year" value={company.foundedYear} />
        <DetailItem label="Headquarters" value={company.headquarters} />
        <DetailItem
          label="Public visibility"
          value={company.showNamePublicly ? 'Shown publicly on job posts' : 'Hidden (Confidential company)'}
        />
        <DetailItem
          label="Website"
          value={
            company.website ? (
              <a href={company.website} target="_blank" rel="noopener noreferrer" className="inline-flex items-center gap-1 text-primary-text hover:underline">
                <Icon icon={Globe} size={13} />
                {company.website}
              </a>
            ) : null
          }
        />
        <DetailItem
          label="LinkedIn"
          value={
            company.linkedinUrl ? (
              <a href={company.linkedinUrl} target="_blank" rel="noopener noreferrer" className="text-primary-text hover:underline">
                {company.linkedinUrl}
              </a>
            ) : null
          }
        />
        <DetailItem
          label="Contact email"
          value={
            company.contactEmail ? (
              <a href={`mailto:${company.contactEmail}`} className="inline-flex items-center gap-1 text-primary-text hover:underline">
                <Icon icon={Mail} size={13} />
                {company.contactEmail}
              </a>
            ) : null
          }
        />
        <DetailItem
          label="Phone"
          value={
            company.phone ? (
              <span className="inline-flex items-center gap-1">
                <Icon icon={Phone} size={13} />
                {company.phone}
              </span>
            ) : null
          }
        />
        <DetailItem
          label="Logo"
          value={
            company.logoUrl ? (
              <a href={company.logoUrl} target="_blank" rel="noopener noreferrer" className="inline-flex items-center gap-1.5 text-primary-text hover:underline">
                <img src={company.logoUrl} alt="" className="h-4 w-4 rounded object-contain" />
                View logo image
              </a>
            ) : null
          }
        />
      </dl>

      <div className="grid gap-8 pt-6 lg:grid-cols-2">
        <div>
          <h3 className="mb-2 text-sm font-semibold text-text">About the company</h3>
          {company.about ? (
            <Markdown source={company.about} />
          ) : (
            <p className="text-sm text-text-subtle">No company overview written yet.</p>
          )}
        </div>
        <div>
          <h3 className="mb-2 text-sm font-semibold text-text">Benefits and perks</h3>
          {company.benefits ? (
            <Markdown source={company.benefits} />
          ) : (
            <p className="text-sm text-text-subtle">No benefits or perks listed yet.</p>
          )}
        </div>
      </div>
    </div>
  );
}

export function CompanyPage() {
  const { signOut } = useAuth();
  const { data: company, isLoading } = useCompany();
  const update = useUpdateCompany();
  const uploadLogo = useUploadCompanyLogo();
  const deleteLogo = useDeleteCompanyLogo();
  const { toast } = useToast();
  const fileInputRef = useRef<HTMLInputElement>(null);
  const overviewFileInputRef = useRef<HTMLInputElement>(null);
  const [form, setForm] = useState<Form>(EMPTY);
  const [headquarters, setHeadquarters] = useState<LocationValue>(emptyLocation);
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
      setHeadquarters(parseLocation(company.headquarters));
    }
  }, [company]);

  const set = (key: keyof Form) => (value: string | boolean) => setForm((f) => ({ ...f, [key]: value }));

  async function handleUploadLogo(file: File) {
    if (!file) return;
    if (file.size > 2 * 1024 * 1024) {
      toast('Logo file must be at most 2 MB.', 'error');
      return;
    }
    try {
      const updated = await uploadLogo.mutateAsync(file);
      setForm((f) => ({ ...f, logoUrl: updated.logoUrl ?? '' }));
      toast('Company logo uploaded.', 'success');
    } catch (err) {
      toast(getApiErrorMessage(err) || 'Failed to upload company logo.', 'error');
    }
  }

  async function handleRemoveLogo() {
    try {
      await deleteLogo.mutateAsync();
      setForm((f) => ({ ...f, logoUrl: '' }));
      toast('Company logo removed.', 'success');
    } catch (err) {
      toast(getApiErrorMessage(err) || 'Failed to remove company logo.', 'error');
    }
  }

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setFieldErrors({});
    try {
      await update.mutateAsync({
        ...form,
        headquarters: formatLocation(headquarters),
        foundedYear: form.foundedYear === '' ? null : Number(form.foundedYear),
      });
      toast('Company profile saved.', 'success');
      setTab('view');
    } catch (err) {
      setError(getApiErrorMessage(err));
      setFieldErrors(getFieldErrors(err));
    }
  }

  if (isLoading) return <Skeleton className="h-64 w-full" />;

  const isComplete = Boolean(company?.name && company?.industry && company?.size);
  const initials = (form.name || company?.name || 'CO')
    .split(/\s+/)
    .map((w) => w[0])
    .join('')
    .slice(0, 2)
    .toUpperCase();

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
      <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <div className="flex flex-wrap items-center gap-3">
            <h1 className="text-[28px] font-bold leading-9">Company profile</h1>
            {isComplete ? (
              <Badge tone="success" icon={Check}>
                Complete
              </Badge>
            ) : (
              <Badge status="paused" />
            )}
          </div>
          <p className="text-sm text-text-secondary">
            Name, industry and size are required before you can activate a job.
          </p>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          <Link to="/account/password">
            <Button variant="secondary">
              <Icon icon={KeyRound} size={16} />
              Change password
            </Button>
          </Link>
          <Button variant="secondary" onClick={() => signOut()}>
            <Icon icon={LogOut} size={16} />
            Sign out
          </Button>
        </div>
      </div>

      <Tabs
        tabs={[
          { key: 'view', label: 'Overview' },
          { key: 'edit', label: 'Edit profile' },
        ]}
        active={tab}
        onChange={(k) => setTab(k as 'view' | 'edit')}
      />

      <input
        type="file"
        ref={overviewFileInputRef}
        onChange={(e) => {
          const file = e.target.files?.[0];
          if (file) handleUploadLogo(file);
          e.target.value = '';
        }}
        accept="image/png,image/jpeg,image/webp,image/gif,image/svg+xml"
        className="hidden"
      />

      {tab === 'view' && company && (
        <CompanyProfileView
          company={company}
          onEdit={() => setTab('edit')}
          onUploadLogoClick={() => overviewFileInputRef.current?.click()}
          isUploadingLogo={uploadLogo.isPending}
        />
      )}

      {tab === 'edit' && (
        <>
          {error && <Banner tone="error">{error}</Banner>}

          <form onSubmit={onSubmit} className="divide-y divide-border rounded-xl border border-border bg-surface p-6 shadow-sm sm:p-8">
            {/* Company Logo section */}
            <div className="space-y-4 pb-6">
              <div>
                <h2 className="text-base font-semibold">Company logo</h2>
                <p className="text-xs text-text-secondary">
                  Upload an image for your company. PNG, JPEG, WebP, GIF, or SVG (max 2 MB).
                </p>
              </div>

              <div className="flex flex-col gap-4 sm:flex-row sm:items-center">
                <div className="flex h-20 w-20 shrink-0 items-center justify-center overflow-hidden rounded-xl border border-border bg-surface p-1 shadow-sm">
                  {form.logoUrl ? (
                    <img
                      src={form.logoUrl}
                      alt={form.name || 'Company logo'}
                      className="h-full w-full object-contain"
                    />
                  ) : (
                    <div className="flex h-full w-full items-center justify-center bg-primary-subtle text-xl font-bold text-primary-text">
                      {initials}
                    </div>
                  )}
                </div>

                <div className="flex flex-col gap-2">
                  <input
                    type="file"
                    ref={fileInputRef}
                    onChange={(e) => {
                      const file = e.target.files?.[0];
                      if (file) handleUploadLogo(file);
                      e.target.value = '';
                    }}
                    accept="image/png,image/jpeg,image/webp,image/gif,image/svg+xml"
                    className="hidden"
                  />
                  <div className="flex flex-wrap items-center gap-2">
                    <Button
                      type="button"
                      variant="secondary"
                      onClick={() => fileInputRef.current?.click()}
                      loading={uploadLogo.isPending}
                    >
                      <Icon icon={Upload} size={15} />
                      {form.logoUrl ? 'Change logo' : 'Upload logo'}
                    </Button>

                    {form.logoUrl && (
                      <Button
                        type="button"
                        variant="ghost"
                        onClick={handleRemoveLogo}
                        loading={deleteLogo.isPending}
                        className="text-text-secondary hover:text-accent"
                      >
                        <Icon icon={Trash2} size={15} />
                        Remove
                      </Button>
                    )}
                  </div>
                  <span className="text-xs text-text-subtle">
                    Recommended square aspect ratio, minimum 128×128 px.
                  </span>
                </div>
              </div>

              <div className="pt-2">
                <Field label="Or specify an external logo URL" error={fieldErrors.logoUrl}>
                  <Input
                    type="url"
                    value={form.logoUrl}
                    onChange={(e) => set('logoUrl')(e.target.value)}
                    placeholder="https://example.com/logo.png"
                    error={Boolean(fieldErrors.logoUrl)}
                  />
                </Field>
              </div>
            </div>

            <div className="space-y-4 py-6">
              <h2 className="text-base font-semibold">Basics</h2>
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
              <LocationFields value={headquarters} onChange={setHeadquarters} />
              <Checkbox
                checked={form.showNamePublicly}
                onChange={set('showNamePublicly')}
                label="Show company name publicly on job posts"
              />
            </div>

            <div className="space-y-4 py-6">
              <h2 className="text-base font-semibold">Contact and links</h2>
              <div className="grid gap-4 sm:grid-cols-2">{inputs(CONTACT)}</div>
            </div>

            <div className="space-y-4 py-6">
              <h2 className="text-base font-semibold">About and benefits</h2>
              <Field label="About the company (Markdown)" error={fieldErrors.about} wrap={false}>
                <MarkdownEditor value={form.about} onChange={set('about')} rows={8} />
              </Field>
              <Field label="Benefits and perks (Markdown)" error={fieldErrors.benefits} wrap={false}>
                <MarkdownEditor value={form.benefits} onChange={set('benefits')} rows={5} />
              </Field>
            </div>

            <div className="flex items-center gap-3 pt-6">
              <Button type="submit" loading={update.isPending}>
                Save changes
              </Button>
              <Button type="button" variant="secondary" onClick={() => setTab('view')}>
                Cancel
              </Button>
            </div>
          </form>
        </>
      )}
    </div>
  );
}
