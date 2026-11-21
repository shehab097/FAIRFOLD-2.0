import type { ReactNode } from 'react';
import { Link } from 'react-router-dom';
import { Building2, Check, KeyRound, Mail, Shield, UserCheck } from 'lucide-react';
import { useAuth } from '../../../app/auth/AuthContext';
import { useProfile, useResume, useSkills } from '../../candidate/queries';
import { useCompany } from '../../employer/queries';
import { Badge } from '../../../components/ui/Badge';
import { Button } from '../../../components/ui/Button';
import { Icon } from '../../../components/ui/Icon';
import { Markdown } from '../../../components/ui/Markdown';

function FieldTile({ label, value }: { label: string; value: ReactNode }) {
  const empty = value === null || value === undefined || value === '';
  return (
    <div className="border border-border bg-bg p-3.5">
      <div className="text-xs font-semibold uppercase tracking-[0.06em] text-text-secondary">{label}</div>
      <div className="mt-1 break-words text-sm font-medium text-text">
        {empty ? <span className="font-normal text-text-subtle">Not specified</span> : value}
      </div>
    </div>
  );
}

function CandidateSnapshot() {
  const { data: profile } = useProfile();
  const { data: skills } = useSkills();
  const { data: resume } = useResume();

  return (
    <section className="border border-border bg-surface">
      <div className="flex flex-wrap items-center justify-between gap-3 border-b border-border bg-raised px-6 py-3.5">
        <div className="flex items-center gap-2">
          <Icon icon={UserCheck} size={18} className="text-primary-text" />
          <h2 className="text-base font-semibold">Candidate profile details</h2>
        </div>
        <Link to="/candidate/profile">
          <Button variant="secondary" className="h-9 px-3 text-xs">
            Edit candidate profile
          </Button>
        </Link>
      </div>
      <div className="space-y-6 p-6">
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
          <FieldTile label="Professional title" value={profile?.title} />
          <FieldTile label="Location" value={profile?.location} />
          <FieldTile label="Phone" value={profile?.phone} />
          <FieldTile label="Remote preference" value={profile?.remotePreference} />
          <FieldTile
            label="Total experience"
            value={profile?.totalExperienceYears !== null && profile?.totalExperienceYears !== undefined ? `${profile.totalExperienceYears} years` : null}
          />
          <FieldTile label="Education level" value={profile?.educationLevel?.replace('_', ' ')} />
          <FieldTile label="Current resume" value={resume ? `${resume.filename} (${resume.status})` : null} />
          <FieldTile label="Skills recorded" value={skills ? `${skills.length} skills` : '0 skills'} />
        </div>
        <div>
          <div className="mb-2 text-xs font-semibold uppercase tracking-[0.06em] text-text-secondary">Bio</div>
          <div className="border border-border bg-bg p-4 text-sm">
            {profile?.bio ? <Markdown source={profile.bio} /> : <span className="text-text-subtle">No bio added yet.</span>}
          </div>
        </div>
        <div>
          <div className="mb-2 text-xs font-semibold uppercase tracking-[0.06em] text-text-secondary">Skills</div>
          {skills && skills.length > 0 ? (
            <div className="flex flex-wrap gap-2">
              {skills.map((s) => (
                <span key={s.id} className="inline-flex items-center gap-1.5 border border-border bg-raised px-2.5 py-1 text-xs text-text">
                  <span className="font-semibold">{s.name}</span>
                  <span className="text-text-secondary">· {s.level}</span>
                  {s.years !== null && <span className="text-text-subtle">({s.years}y)</span>}
                  {s.verified && <Icon icon={Check} size={13} className="text-primary-text" />}
                </span>
              ))}
            </div>
          ) : (
            <p className="text-sm text-text-subtle">No skills listed yet.</p>
          )}
        </div>
      </div>
    </section>
  );
}

function EmployerCompanySnapshot() {
  const { data: company } = useCompany();
  if (!company) return null;

  return (
    <section className="border border-border bg-surface">
      <div className="flex flex-wrap items-center justify-between gap-3 border-b border-border bg-raised px-6 py-3.5">
        <div className="flex items-center gap-2">
          <Icon icon={Building2} size={18} className="text-primary-text" />
          <h2 className="text-base font-semibold">Company profile details (all 13 fields)</h2>
        </div>
        <Link to="/employer/company">
          <Button variant="secondary" className="h-9 px-3 text-xs">
            Manage company profile
          </Button>
        </Link>
      </div>
      <div className="space-y-6 p-6">
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
          <FieldTile label="Company name" value={company.name} />
          <FieldTile label="Industry" value={company.industry} />
          <FieldTile label="Company size" value={company.size ? `${company.size} employees` : null} />
          <FieldTile label="Tagline" value={company.tagline} />
          <FieldTile label="Founded year" value={company.foundedYear} />
          <FieldTile label="Headquarters" value={company.headquarters} />
          <FieldTile
            label="Public name visibility"
            value={company.showNamePublicly ? 'Shown publicly on job posts' : 'Hidden (Confidential company)'}
          />
          <FieldTile
            label="Website"
            value={
              company.website ? (
                <a href={company.website} target="_blank" rel="noopener noreferrer" className="text-primary-text underline">
                  {company.website}
                </a>
              ) : null
            }
          />
          <FieldTile
            label="LinkedIn"
            value={
              company.linkedinUrl ? (
                <a href={company.linkedinUrl} target="_blank" rel="noopener noreferrer" className="text-primary-text underline">
                  {company.linkedinUrl}
                </a>
              ) : null
            }
          />
          <FieldTile label="Logo URL" value={company.logoUrl} />
          <FieldTile label="Contact email" value={company.contactEmail} />
          <FieldTile label="Phone" value={company.phone} />
        </div>
        <div className="grid gap-4 lg:grid-cols-2">
          <div className="border border-border bg-bg p-4">
            <div className="mb-2 text-xs font-semibold uppercase tracking-[0.06em] text-text-secondary">About the company</div>
            {company.about ? <Markdown source={company.about} /> : <span className="text-sm text-text-subtle">Not specified</span>}
          </div>
          <div className="border border-border bg-bg p-4">
            <div className="mb-2 text-xs font-semibold uppercase tracking-[0.06em] text-text-secondary">Benefits and perks</div>
            {company.benefits ? <Markdown source={company.benefits} /> : <span className="text-sm text-text-subtle">Not specified</span>}
          </div>
        </div>
      </div>
    </section>
  );
}

export function AccountPage() {
  const { user } = useAuth();
  const initials = `${user?.firstName?.[0] ?? ''}${user?.lastName?.[0] ?? ''}`.toUpperCase() || 'U';

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-3 border-b border-border pb-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <p className="text-xs font-semibold uppercase tracking-[0.06em] text-primary-text">Account & Identity</p>
          <h1 className="mt-1 text-[28px] font-bold leading-9">Account profile</h1>
          <p className="text-text-secondary">Complete overview of your signed-in account and role profile data.</p>
        </div>
        <Link to="/account/password">
          <Button variant="secondary">
            <Icon icon={KeyRound} size={16} />
            Change password
          </Button>
        </Link>
      </div>

      <section className="border border-border border-t-2 border-t-primary bg-surface">
        <div className="flex flex-wrap items-center justify-between gap-4 border-b border-border bg-raised px-6 py-4">
          <div className="flex items-center gap-4">
            <div className="flex h-12 w-12 shrink-0 items-center justify-center border border-border-strong bg-primary-subtle text-base font-bold text-primary-text">
              {initials}
            </div>
            <div>
              <div className="flex flex-wrap items-center gap-2.5">
                <h2 className="text-xl font-semibold text-text">
                  {user?.firstName} {user?.lastName}
                </h2>
                <Badge tone="success" icon={Shield}>
                  {user?.role.replace('_', ' ')}
                </Badge>
              </div>
              <p className="mt-0.5 flex items-center gap-1.5 text-sm text-text-secondary">
                <Icon icon={Mail} size={14} />
                {user?.email}
              </p>
            </div>
          </div>
        </div>

        <div className="grid gap-3 p-6 sm:grid-cols-2 lg:grid-cols-3">
          <FieldTile label="Full name" value={`${user?.firstName ?? ''} ${user?.lastName ?? ''}`.trim()} />
          <FieldTile label="First name" value={user?.firstName} />
          <FieldTile label="Last name" value={user?.lastName} />
          <FieldTile label="Email address" value={user?.email} />
          <FieldTile label="Role" value={user?.role} />
          <FieldTile label="Account ID" value={user?.id} />
          {user?.companyId && <FieldTile label="Company ID" value={user.companyId} />}
        </div>
      </section>

      {user?.role === 'CANDIDATE' && <CandidateSnapshot />}
      {user?.role === 'EMPLOYER_HR' && <EmployerCompanySnapshot />}

      <section className="flex flex-col justify-between gap-4 border border-border bg-surface p-6 sm:flex-row sm:items-center">
        <div>
          <div className="flex items-center gap-2">
            <Icon icon={KeyRound} size={18} className="text-primary-text" />
            <h2 className="text-base font-semibold">Password & security</h2>
          </div>
          <p className="mt-1 text-sm text-text-secondary">
            Manage your password on a dedicated security page.
          </p>
        </div>
        <Link to="/account/password">
          <Button variant="secondary">Go to change password →</Button>
        </Link>
      </section>
    </div>
  );
}
