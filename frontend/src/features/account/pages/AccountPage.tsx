import { Link } from 'react-router-dom';
import { Check, FileText, KeyRound, LogOut, Mail, Monitor, Moon, Shield, Sun } from 'lucide-react';
import { useAuth } from '../../../app/auth/AuthContext';
import { useTheme } from '../../../app/theme/ThemeContext';
import { useProfile, useResume, useSkills } from '../../candidate/queries';
import { useCompany } from '../../employer/queries';
import { ProfilePhotoCard } from '../components/ProfilePhotoCard';
import { AccountDataCard } from '../components/AccountDataCard';
import { Badge } from '../../../components/ui/Badge';
import { Button } from '../../../components/ui/Button';
import { Icon } from '../../../components/ui/Icon';
import { Markdown } from '../../../components/ui/Markdown';
import { UserAvatar } from '../../../components/ui/UserAvatar';
import { DetailGrid, DetailItem, DetailSection } from '../../../components/ui/DetailList';

function CandidateSnapshot() {
  const { data: profile } = useProfile();
  const { data: skills } = useSkills();
  const { data: resume } = useResume();

  return (
    <div className="divide-y divide-border rounded-xl border border-border bg-surface p-6 shadow-sm sm:p-8">
      <div className="flex flex-wrap items-center justify-between gap-3 pb-5">
        <h2 className="text-base font-semibold">Candidate profile</h2>
        <div className="flex flex-wrap gap-2">
          <Link to="/candidate/resume">
            <Button variant="secondary" className="h-9 px-3 text-xs">
              <Icon icon={FileText} size={14} />
              Resume & preview
            </Button>
          </Link>
          <Link to="/candidate/profile">
            <Button variant="secondary" className="h-9 px-3 text-xs">
              Edit profile
            </Button>
          </Link>
        </div>
      </div>

      <div className="space-y-6 py-6">
        <DetailSection title="Personal details">
          <DetailGrid>
            <DetailItem label="Location" value={profile?.location} />
            <DetailItem label="Phone" value={profile?.phone} />
            <DetailItem label="Remote preference" value={profile?.remotePreference} />
          </DetailGrid>
        </DetailSection>
        <DetailSection title="Professional details">
          <DetailGrid>
            <DetailItem label="Professional title" value={profile?.title} />
            <DetailItem
              label="Total experience"
              value={profile?.totalExperienceYears !== null && profile?.totalExperienceYears !== undefined ? `${profile.totalExperienceYears} years` : null}
            />
            <DetailItem label="Education level" value={profile?.educationLevel?.replace('_', ' ')} />
            <DetailItem
              label="Current resume"
              value={
                resume ? (
                  <Link to="/candidate/resume" className="text-primary-text hover:underline">
                    {resume.filename} ({resume.status})
                  </Link>
                ) : null
              }
            />
          </DetailGrid>
        </DetailSection>
      </div>

      <div className="space-y-4 pt-6">
        <div>
          <div className="mb-1 text-xs text-text-secondary">Bio</div>
          {profile?.bio ? <Markdown source={profile.bio} /> : <span className="text-sm text-text-subtle">No bio added yet.</span>}
        </div>
        <div>
          <div className="mb-2 text-xs text-text-secondary">Skills</div>
          {skills && skills.length > 0 ? (
            <div className="flex flex-wrap gap-2">
              {skills.map((s) => (
                <span key={s.id} className="inline-flex items-center gap-1.5 rounded-sm bg-raised px-2.5 py-1 text-xs text-text">
                  <span className="font-medium">{s.name}</span>
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
    </div>
  );
}

function EmployerCompanySnapshot() {
  const { data: company } = useCompany();
  if (!company) return null;

  return (
    <div className="divide-y divide-border rounded-xl border border-border bg-surface p-6 shadow-sm sm:p-8">
      <div className="flex flex-wrap items-center justify-between gap-3 pb-5">
        <h2 className="text-base font-semibold">Company profile</h2>
        <Link to="/employer/company">
          <Button variant="secondary" className="h-9 px-3 text-xs">
            Manage company profile
          </Button>
        </Link>
      </div>

      <div className="space-y-6 py-6">
        <DetailSection title="Company facts">
          <DetailGrid>
            <DetailItem label="Company name" value={company.name} />
            <DetailItem label="Industry" value={company.industry} />
            <DetailItem label="Company size" value={company.size ? `${company.size} employees` : null} />
            <DetailItem label="Founded year" value={company.foundedYear} />
            <DetailItem label="Headquarters" value={company.headquarters} />
            <DetailItem
              label="Public name visibility"
              value={company.showNamePublicly ? 'Shown publicly on job posts' : 'Hidden (Confidential company)'}
            />
            <DetailItem label="Tagline" value={company.tagline} />
          </DetailGrid>
        </DetailSection>
        <DetailSection title="Contact and links">
          <DetailGrid>
            <DetailItem
              label="Website"
              value={
                company.website ? (
                  <a href={company.website} target="_blank" rel="noopener noreferrer" className="text-primary-text hover:underline">
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
            <DetailItem label="Contact email" value={company.contactEmail} />
            <DetailItem label="Phone" value={company.phone} />
            <DetailItem label="Logo URL" value={company.logoUrl} />
          </DetailGrid>
        </DetailSection>
      </div>

      <div className="grid gap-8 pt-6 lg:grid-cols-2">
        <div>
          <div className="mb-1.5 text-xs text-text-secondary">About the company</div>
          {company.about ? <Markdown source={company.about} /> : <span className="text-sm text-text-subtle">Not specified</span>}
        </div>
        <div>
          <div className="mb-1.5 text-xs text-text-secondary">Benefits and perks</div>
          {company.benefits ? <Markdown source={company.benefits} /> : <span className="text-sm text-text-subtle">Not specified</span>}
        </div>
      </div>
    </div>
  );
}

export function AccountPage() {
  const { user, signOut } = useAuth();
  const { theme, setTheme } = useTheme();
  const displayName = `${user?.firstName ?? ''} ${user?.lastName ?? ''}`.trim();

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h1 className="text-[28px] font-bold leading-9">Account profile</h1>
          <p className="text-sm text-text-secondary">Your account details, role profile and session controls.</p>
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

      <ProfilePhotoCard />

      <div className="divide-y divide-border rounded-xl border border-border bg-surface p-6 shadow-sm sm:p-8">
        <div className="flex items-center gap-4 pb-6">
          <UserAvatar name={displayName} avatarUrl={user?.avatarUrl} size="lg" />
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

        <div className="py-6">
          <DetailSection title="Account details">
            <DetailGrid>
              <DetailItem label="First name" value={user?.firstName} />
              <DetailItem label="Last name" value={user?.lastName} />
              <DetailItem label="Email address" value={user?.email} />
              <DetailItem label="Role" value={user?.role.replace('_', ' ')} />
              <DetailItem label="Account ID" value={user?.id} />
              {user?.companyId && <DetailItem label="Company ID" value={user.companyId} />}
            </DetailGrid>
          </DetailSection>
        </div>

        <div className="pt-6">
          <h3 className="text-sm font-semibold text-text">Theme & appearance</h3>
          <p className="mt-0.5 text-xs text-text-secondary">Choose how FairFold looks to you.</p>
          <div className="mt-3 flex flex-wrap gap-2.5">
            <button
              type="button"
              onClick={() => setTheme('light')}
              className={`inline-flex items-center gap-2 rounded-lg border px-3.5 py-2 text-xs font-semibold transition-colors ${
                theme === 'light'
                  ? 'border-primary bg-primary-subtle text-primary-text'
                  : 'border-border bg-bg text-text hover:bg-raised'
              }`}
            >
              <Icon icon={Sun} size={16} />
              Light mode
            </button>
            <button
              type="button"
              onClick={() => setTheme('dark')}
              className={`inline-flex items-center gap-2 rounded-lg border px-3.5 py-2 text-xs font-semibold transition-colors ${
                theme === 'dark'
                  ? 'border-primary bg-primary-subtle text-primary-text'
                  : 'border-border bg-bg text-text hover:bg-raised'
              }`}
            >
              <Icon icon={Moon} size={16} />
              Dark mode
            </button>
            <button
              type="button"
              onClick={() => setTheme('system')}
              className={`inline-flex items-center gap-2 rounded-lg border px-3.5 py-2 text-xs font-semibold transition-colors ${
                theme === 'system'
                  ? 'border-primary bg-primary-subtle text-primary-text'
                  : 'border-border bg-bg text-text hover:bg-raised'
              }`}
            >
              <Icon icon={Monitor} size={16} />
              System default
            </button>
          </div>
        </div>
      </div>

      <AccountDataCard />

      {user?.role === 'CANDIDATE' && <CandidateSnapshot />}
      {user?.role === 'EMPLOYER_HR' && <EmployerCompanySnapshot />}
    </div>
  );
}
