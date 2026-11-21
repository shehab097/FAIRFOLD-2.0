import { useEffect, useState, type ReactNode } from 'react';
import { Check, MapPin, Phone, Plus, UserCheck } from 'lucide-react';
import { useAuth } from '../../../app/auth/AuthContext';
import { useProfile, useSkills, useAddSkill, useUpdateSkill, useDeleteSkill, useUpdateProfile } from '../queries';
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
import { useToast } from '../../../components/ui/Toast';
import { getApiErrorMessage } from '../../../lib/errors';

function SummaryTile({ label, value }: { label: string; value: ReactNode }) {
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
              <div className="absolute z-10 mt-1 w-full border border-border bg-raised">
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
  const { user } = useAuth();
  const { data: profile, isLoading } = useProfile();
  const { data: skills } = useSkills();
  const updateProfile = useUpdateProfile();
  const deleteSkill = useDeleteSkill();
  const { toast } = useToast();

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
      render: (s) =>
        s.verified ? (
          <Badge status="verified" />
        ) : (
          <Badge tone="neutral">Self-reported</Badge>
        ),
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
      <div className="border-b border-border pb-4">
        <p className="text-xs font-semibold uppercase tracking-[0.06em] text-primary-text">Candidate Workspace</p>
        <h1 className="mt-1 text-[28px] font-bold leading-9">Profile</h1>
        <p className="text-text-secondary">Your complete candidate profile, personal details and verified skills.</p>
      </div>

      {/* Complete Profile View Summary */}
      <section className="border border-border border-t-2 border-t-primary bg-surface">
        <div className="flex flex-wrap items-center justify-between gap-4 border-b border-border bg-raised px-6 py-4">
          <div className="flex items-center gap-4">
            <div className="flex h-12 w-12 shrink-0 items-center justify-center border border-border-strong bg-primary-subtle text-base font-bold text-primary-text">
              {initials}
            </div>
            <div>
              <div className="flex flex-wrap items-center gap-2.5">
                <h2 className="text-xl font-bold text-text">
                  {user?.firstName} {user?.lastName}
                </h2>
                {profile?.title && (
                  <span className="border border-border bg-surface px-2 py-0.5 text-xs font-medium text-primary-text">
                    {profile.title}
                  </span>
                )}
              </div>
              <div className="mt-1 flex flex-wrap items-center gap-3 text-xs text-text-secondary">
                {user?.email && <span>{user.email}</span>}
                {profile?.location && (
                  <span className="inline-flex items-center gap-1">
                    <Icon icon={MapPin} size={13} />
                    {profile.location}
                  </span>
                )}
                {profile?.phone && (
                  <span className="inline-flex items-center gap-1">
                    <Icon icon={Phone} size={13} />
                    {profile.phone}
                  </span>
                )}
              </div>
            </div>
          </div>
          <Badge tone={profile?.title && profile?.location ? 'success' : 'neutral'} icon={profile?.title && profile?.location ? Check : UserCheck}>
            {profile?.title && profile?.location ? 'Profile saved' : 'Incomplete'}
          </Badge>
        </div>

        <div className="space-y-4 p-6">
          <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
            <SummaryTile label="Full name" value={`${user?.firstName ?? ''} ${user?.lastName ?? ''}`.trim()} />
            <SummaryTile label="Email" value={user?.email} />
            <SummaryTile label="Title" value={profile?.title} />
            <SummaryTile label="Location" value={profile?.location} />
            <SummaryTile label="Phone" value={profile?.phone} />
            <SummaryTile label="Remote preference" value={profile?.remotePreference} />
            <SummaryTile
              label="Experience"
              value={
                profile?.totalExperienceYears !== null && profile?.totalExperienceYears !== undefined
                  ? `${profile.totalExperienceYears} years`
                  : null
              }
            />
            <SummaryTile label="Education" value={profile?.educationLevel?.replace('_', ' ')} />
          </div>
          <div className="border border-border bg-bg p-4">
            <div className="mb-1.5 text-xs font-semibold uppercase tracking-[0.06em] text-text-secondary">Bio</div>
            {profile?.bio ? <Markdown source={profile.bio} /> : <span className="text-sm text-text-subtle">No bio written yet.</span>}
          </div>
        </div>
      </section>

      {/* Edit Basics */}
      <section className="border border-border bg-surface">
        <div className="border-b border-border bg-raised px-6 py-3.5">
          <h2 className="text-base font-semibold">Basics</h2>
        </div>
        <div className="p-6">
          <div className="grid gap-4 sm:grid-cols-2">
            <div>
              <label className="mb-1 block text-xs font-semibold uppercase tracking-[0.06em] text-text-secondary">
                Title
              </label>
              <Input value={title} onChange={(e) => setTitle(e.target.value)} placeholder="e.g. Backend Engineer" />
            </div>
            <div>
              <label className="mb-1 block text-xs font-semibold uppercase tracking-[0.06em] text-text-secondary">
                Location
              </label>
              <Input value={location} onChange={(e) => setLocation(e.target.value)} placeholder="City" />
            </div>
            <div>
              <label className="mb-1 block text-xs font-semibold uppercase tracking-[0.06em] text-text-secondary">
                Phone
              </label>
              <Input value={phone} onChange={(e) => setPhone(e.target.value)} placeholder="+880…" />
            </div>
            <div>
              <label className="mb-1 block text-xs font-semibold uppercase tracking-[0.06em] text-text-secondary">
                Remote preference
              </label>
              <Select value={remotePreference} onChange={(e) => setRemotePreference(e.target.value)}>
                {REMOTE_PREFERENCES.map((r) => (
                  <option key={r} value={r}>
                    {r}
                  </option>
                ))}
              </Select>
            </div>
            <div>
              <label className="mb-1 block text-xs font-semibold uppercase tracking-[0.06em] text-text-secondary">
                Experience (years)
              </label>
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
              <label className="mb-1 block text-xs font-semibold uppercase tracking-[0.06em] text-text-secondary">
                Education
              </label>
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
            <label className="mb-1 block text-xs font-semibold uppercase tracking-[0.06em] text-text-secondary">
              Bio
            </label>
            <textarea
              value={bio}
              onChange={(e) => setBio(e.target.value)}
              className="min-h-[96px] w-full border border-border-strong bg-bg px-3 py-2 text-sm text-text placeholder:text-text-subtle"
              placeholder="A short summary"
            />
          </div>
          <div className="mt-4 flex justify-end">
            <Button loading={updateProfile.isPending} onClick={saveBasics}>
              Save
            </Button>
          </div>
        </div>
      </section>

      {/* Skills */}
      <section className="border border-border bg-surface">
        <div className="flex items-center justify-between border-b border-border bg-raised px-6 py-3.5">
          <h2 className="text-base font-semibold">Skills ({skills?.length ?? 0})</h2>
          <Button onClick={() => setEditor({ open: true, skill: null })}>
            <Icon icon={Plus} size={16} />
            Add skill
          </Button>
        </div>
        <div className="p-6">
          {skills && skills.length === 0 ? (
            <div className="border border-border bg-bg p-6 text-center text-sm text-text-subtle">
              No skills yet. Add your first skill.
            </div>
          ) : (
            <Table columns={columns} rows={skills ?? []} rowKey={(s) => s.id} />
          )}
        </div>
      </section>

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
