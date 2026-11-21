import { useEffect, useState, type ReactNode } from 'react';
import { Link } from 'react-router-dom';
import { Check, FileText, KeyRound, LogOut, MapPin, Pencil, Phone, Plus, UserCheck } from 'lucide-react';
import { useAuth } from '../../../app/auth/AuthContext';
import { useProfile, useResume, useSkills, useAddSkill, useUpdateSkill, useDeleteSkill, useUpdateProfile } from '../queries';
import { suggestSkills } from '../../../api/skills';
import { SKILL_LEVELS, REMOTE_PREFERENCES, EDUCATION_LEVELS, type CandidateSkill } from '../types';
import { Badge } from '../../../components/ui/Badge';
import { Button } from '../../../components/ui/Button';
import { Dialog } from '../../../components/ui/Dialog';
import { Icon } from '../../../components/ui/Icon';
import { Input } from '../../../components/ui/Input';
import { Markdown } from '../../../components/ui/Markdown';
import { Select } from '../../../components/ui/Select';
import { Skeleton } from '../../../components/ui/Skeleton';
import { Table, type Column } from '../../../components/ui/Table';
import { Tabs } from '../../../components/ui/Tabs';
import { useToast } from '../../../components/ui/Toast';
import { getApiErrorMessage } from '../../../lib/errors';

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

function SkillEditor({
  open,
  initial,
  onClose,
}: {
  open: boolean;
  initial: CandidateSkill | null;
  onClose: () => void;
}) {
  const { toast } = useToast();
  const addSkill = useAddSkill();
  const updateSkill = useUpdateSkill();
  const [query, setQuery] = useState('');
  const [options, setOptions] = useState<{ id: string; name: string }[]>([]);
  const [skillId, setSkillId] = useState<string | null>(initial?.skillId ?? null);
  const [skillName, setSkillName] = useState(initial?.name ?? '');
  const [level, setLevel] = useState<string>(initial?.level ?? 'beginner');
  const [years, setYears] = useState(initial?.years?.toString() ?? '');

  useEffect(() => {
    if (!initial) {
      setSkillId(null);
      setSkillName('');
      setLevel('beginner');
      setYears('');
      setQuery('');
      setOptions([]);
      return;
    }
    setSkillId(initial.skillId);
    setSkillName(initial.name);
    setLevel(initial.level);
    setYears(initial.years?.toString() ?? '');
    setQuery('');
    setOptions([]);
  }, [initial, open]);

  useEffect(() => {
    if (initial || query.trim().length < 1) {
      setOptions([]);
      return;
    }
    const timer = window.setTimeout(() => {
      suggestSkills(query).then(setOptions).catch(() => setOptions([]));
    }, 200);
    return () => window.clearTimeout(timer);
  }, [query, initial]);

  function chooseSkill(id: string, name: string) {
    setSkillId(id);
    setSkillName(name);
    setQuery(name);
    setOptions([]);
  }

  async function save() {
    if (!skillId) return;
    const yearsValue = years.trim() === '' ? null : Number(years);
    try {
      if (initial) {
        await updateSkill.mutateAsync({ id: initial.id, level, years: yearsValue });
      } else {
        await addSkill.mutateAsync({ skillId, level, years: yearsValue });
      }
      toast('Skill saved.', 'success');
      onClose();
    } catch (err) {
      toast(getApiErrorMessage(err), 'error');
    }
  }

  const busy = addSkill.isPending || updateSkill.isPending;

  return (
    <Dialog open={open} title={initial ? 'Edit skill' : 'Add skill'} onClose={onClose}>
      <div className="space-y-4">
        {!initial && (
          <div className="relative">
            <Input
              value={query}
              onChange={(e) => {
                setQuery(e.target.value);
                setSkillId(null);
              }}
              placeholder="Search skills"
              aria-label="Search skills"
            />
            {options.length > 0 && (
              <div className="absolute z-10 mt-1 w-full rounded-sm border border-border bg-raised">
                {options.map((o) => (
                  <button
                    key={o.id}
                    type="button"
                    className="block w-full px-3 py-2 text-left text-sm text-text hover:bg-primary-subtle"
                    onClick={() => chooseSkill(o.id, o.name)}
                  >
                    {o.name}
                  </button>
                ))}
              </div>
            )}
          </div>
        )}
        {initial && <div className="text-sm font-semibold text-text">{skillName}</div>}
        <Select value={level} onChange={(e) => setLevel(e.target.value)} aria-label="Level">
          {SKILL_LEVELS.map((l) => (
            <option key={l} value={l}>
              {l}
            </option>
          ))}
        </Select>
        <Input
          type="number"
          min={0}
          max={60}
          step="0.1"
          value={years}
          onChange={(e) => setYears(e.target.value)}
          placeholder="Years of experience"
          aria-label="Years"
        />
        <div className="flex justify-end gap-3">
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button loading={busy} disabled={!skillId} onClick={save}>
            Save
          </Button>
        </div>
      </div>
    </Dialog>
  );
}

export function ProfilePage() {
  const { user, signOut } = useAuth();
  const { data: profile, isLoading } = useProfile();
  const { data: skills } = useSkills();
  const { data: resume } = useResume();
  const updateProfile = useUpdateProfile();
  const deleteSkill = useDeleteSkill();
  const { toast } = useToast();

  const [tab, setTab] = useState<'overview' | 'edit'>('overview');
  const [phone, setPhone] = useState('');
  const [title, setTitle] = useState('');
  const [bio, setBio] = useState('');
  const [location, setLocation] = useState('');
  const [remotePreference, setRemotePreference] = useState('any');
  const [totalExperienceYears, setTotalExperienceYears] = useState('');
  const [educationLevel, setEducationLevel] = useState('');
  const [editor, setEditor] = useState<{ open: boolean; skill: CandidateSkill | null }>({
    open: false,
    skill: null,
  });
  const [toDelete, setToDelete] = useState<CandidateSkill | null>(null);

  useEffect(() => {
    if (!profile) return;
    setPhone(profile.phone ?? '');
    setTitle(profile.title ?? '');
    setBio(profile.bio ?? '');
    setLocation(profile.location ?? '');
    setRemotePreference(profile.remotePreference ?? 'any');
    setTotalExperienceYears(profile.totalExperienceYears?.toString() ?? '');
    setEducationLevel(profile.educationLevel ?? '');
  }, [profile]);

  async function saveBasics() {
    try {
      await updateProfile.mutateAsync({
        phone,
        title,
        bio,
        location,
        remotePreference: remotePreference as (typeof REMOTE_PREFERENCES)[number],
        totalExperienceYears: totalExperienceYears.trim() === '' ? null : Number(totalExperienceYears),
        educationLevel: educationLevel || null,
      });
      toast('Profile saved.', 'success');
      setTab('overview');
    } catch (err) {
      toast(getApiErrorMessage(err), 'error');
    }
  }

  async function confirmDelete() {
    if (!toDelete) return;
    try {
      await deleteSkill.mutateAsync(toDelete.id);
      toast('Skill removed.', 'success');
      setToDelete(null);
    } catch (err) {
      toast(getApiErrorMessage(err), 'error');
    }
  }

  const columns: Column<CandidateSkill>[] = [
    { header: 'Skill', render: (s) => <span className="font-medium">{s.name}</span> },
    { header: 'Level', render: (s) => <span className="capitalize">{s.level}</span> },
    { header: 'Years', render: (s) => (s.years === null ? '—' : String(s.years)) },
    {
      header: 'Status',
      render: (s) => (s.verified ? <Badge status="verified" /> : <Badge tone="neutral">Self-reported</Badge>),
    },
    {
      header: '',
      render: (s) => (
        <div className="flex justify-end gap-1">
          <Button variant="ghost" className="h-8 px-2 text-xs" onClick={() => setEditor({ open: true, skill: s })}>
            Edit
          </Button>
          <Button variant="danger" className="h-8 px-2 text-xs" onClick={() => setToDelete(s)}>
            Remove
          </Button>
        </div>
      ),
    },
  ];

  if (isLoading) return <Skeleton className="h-64 w-full" />;

  const initials = `${user?.firstName?.[0] ?? ''}${user?.lastName?.[0] ?? ''}`.toUpperCase() || 'C';

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h1 className="text-[28px] font-bold leading-9">Profile</h1>
          <p className="text-sm text-text-secondary">Your personal details, experience, skills and account actions.</p>
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
          { key: 'overview', label: 'Overview' },
          { key: 'edit', label: 'Edit details' },
        ]}
        active={tab}
        onChange={(k) => setTab(k as 'overview' | 'edit')}
      />

      {tab === 'overview' && (
        <div className="divide-y divide-border rounded border border-border bg-surface p-6 sm:p-8">
          <div className="flex flex-wrap items-center justify-between gap-4 pb-6">
            <div className="flex items-center gap-4">
              <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-sm bg-primary-subtle text-base font-bold text-primary-text">
                {initials}
              </div>
              <div>
                <div className="flex flex-wrap items-center gap-2.5">
                  <h2 className="text-xl font-bold text-text">
                    {user?.firstName} {user?.lastName}
                  </h2>
                  <Badge tone={profile?.title && profile?.location ? 'success' : 'neutral'} icon={profile?.title && profile?.location ? Check : UserCheck}>
                    {profile?.title && profile?.location ? 'Complete' : 'Incomplete'}
                  </Badge>
                </div>
                <div className="mt-1 flex flex-wrap items-center gap-3 text-sm text-text-secondary">
                  {profile?.title && <span>{profile.title}</span>}
                  {profile?.location && (
                    <span className="inline-flex items-center gap-1">
                      · <Icon icon={MapPin} size={13} />
                      {profile.location}
                    </span>
                  )}
                  {profile?.phone && (
                    <span className="inline-flex items-center gap-1">
                      · <Icon icon={Phone} size={13} />
                      {profile.phone}
                    </span>
                  )}
                </div>
              </div>
            </div>
            <Button variant="secondary" onClick={() => setTab('edit')}>
              <Icon icon={Pencil} size={15} />
              Edit details
            </Button>
          </div>

          <dl className="grid gap-y-5 gap-x-8 py-6 sm:grid-cols-2 lg:grid-cols-4">
            <DetailItem label="Full name" value={`${user?.firstName ?? ''} ${user?.lastName ?? ''}`.trim()} />
            <DetailItem label="Email" value={user?.email} />
            <DetailItem label="Title" value={profile?.title} />
            <DetailItem label="Location" value={profile?.location} />
            <DetailItem label="Phone" value={profile?.phone} />
            <DetailItem label="Remote preference" value={profile?.remotePreference} />
            <DetailItem
              label="Experience"
              value={
                profile?.totalExperienceYears !== null && profile?.totalExperienceYears !== undefined
                  ? `${profile.totalExperienceYears} years`
                  : null
              }
            />
            <DetailItem label="Education" value={profile?.educationLevel?.replace('_', ' ')} />
            <DetailItem
              label="Current resume"
              value={
                resume ? (
                  <Link to="/candidate/resume" className="inline-flex items-center gap-1 text-primary-text hover:underline">
                    <Icon icon={FileText} size={14} />
                    {resume.filename} (Preview)
                  </Link>
                ) : (
                  <Link to="/candidate/resume" className="text-primary-text hover:underline">
                    Upload resume
                  </Link>
                )
              }
            />
          </dl>

          <div className="pt-6">
            <div className="mb-1.5 text-xs text-text-secondary">Bio</div>
            {profile?.bio ? <Markdown source={profile.bio} /> : <span className="text-sm text-text-subtle">No bio written yet.</span>}
          </div>
        </div>
      )}

      {tab === 'edit' && (
        <div className="rounded border border-border bg-surface p-6 sm:p-8">
          <h2 className="mb-4 text-base font-semibold">Edit profile details</h2>
          <div className="grid gap-4 sm:grid-cols-2">
            <div>
              <label className="mb-1 block text-xs text-text-secondary">Title</label>
              <Input value={title} onChange={(e) => setTitle(e.target.value)} placeholder="e.g. Backend Engineer" />
            </div>
            <div>
              <label className="mb-1 block text-xs text-text-secondary">Location</label>
              <Input value={location} onChange={(e) => setLocation(e.target.value)} placeholder="City" />
            </div>
            <div>
              <label className="mb-1 block text-xs text-text-secondary">Phone</label>
              <Input value={phone} onChange={(e) => setPhone(e.target.value)} placeholder="+880…" />
            </div>
            <div>
              <label className="mb-1 block text-xs text-text-secondary">Remote preference</label>
              <Select value={remotePreference} onChange={(e) => setRemotePreference(e.target.value)}>
                {REMOTE_PREFERENCES.map((r) => (
                  <option key={r} value={r}>
                    {r}
                  </option>
                ))}
              </Select>
            </div>
            <div>
              <label className="mb-1 block text-xs text-text-secondary">Experience (years)</label>
              <Input
                type="number"
                min={0}
                max={40}
                step="0.1"
                value={totalExperienceYears}
                onChange={(e) => setTotalExperienceYears(e.target.value)}
              />
            </div>
            <div>
              <label className="mb-1 block text-xs text-text-secondary">Education</label>
              <Select value={educationLevel} onChange={(e) => setEducationLevel(e.target.value)}>
                <option value="">—</option>
                {EDUCATION_LEVELS.map((l) => (
                  <option key={l} value={l}>
                    {l}
                  </option>
                ))}
              </Select>
            </div>
          </div>
          <div className="mt-4">
            <label className="mb-1 block text-xs text-text-secondary">Bio</label>
            <textarea
              value={bio}
              onChange={(e) => setBio(e.target.value)}
              className="min-h-[96px] w-full rounded-sm border border-border bg-bg px-3 py-2 text-sm text-text placeholder:text-text-subtle focus:border-primary focus:outline-none"
              placeholder="A short summary"
            />
          </div>
          <div className="mt-5 flex items-center gap-3">
            <Button loading={updateProfile.isPending} onClick={saveBasics}>
              Save changes
            </Button>
            <Button variant="secondary" onClick={() => setTab('overview')}>
              Cancel
            </Button>
          </div>
        </div>
      )}

      {/* Skills */}
      <div className="space-y-4">
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-semibold">Skills ({skills?.length ?? 0})</h2>
          <Button onClick={() => setEditor({ open: true, skill: null })}>
            <Icon icon={Plus} size={16} />
            Add skill
          </Button>
        </div>
        {skills && skills.length === 0 ? (
          <div className="rounded border border-border bg-surface p-8 text-center text-sm text-text-subtle">
            No skills yet. Add your first skill.
          </div>
        ) : (
          <Table columns={columns} rows={skills ?? []} rowKey={(s) => s.id} />
        )}
      </div>

      <SkillEditor open={editor.open} initial={editor.skill} onClose={() => setEditor({ open: false, skill: null })} />

      <Dialog open={Boolean(toDelete)} title="Remove skill" onClose={() => setToDelete(null)}>
        <p className="text-text">Remove “{toDelete?.name}”?</p>
        <div className="mt-6 flex justify-end gap-3">
          <Button variant="secondary" onClick={() => setToDelete(null)}>
            Cancel
          </Button>
          <Button variant="danger" loading={deleteSkill.isPending} onClick={confirmDelete}>
            Remove
          </Button>
        </div>
      </Dialog>
    </div>
  );
}
