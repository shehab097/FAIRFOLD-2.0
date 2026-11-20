import { useEffect, useState, type FormEvent } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useCreateJob, useEmployerJob, useUpdateJob } from '../queries';
import { SkillPicker, type SelectedSkill } from '../components/SkillPicker';
import { EXPERIENCE_LEVELS, type ExperienceLevel, type JobInput } from '../../jobs/types';
import { Banner } from '../../../components/ui/Banner';
import { Button } from '../../../components/ui/Button';
import { Checkbox } from '../../../components/ui/Checkbox';
import { Input } from '../../../components/ui/Input';
import { Select } from '../../../components/ui/Select';
import { Skeleton } from '../../../components/ui/Skeleton';
import { Textarea } from '../../../components/ui/Textarea';
import { getApiErrorMessage, getFieldErrors } from '../../../lib/errors';

function Field({ label, error, children }: { label: string; error?: string; children: React.ReactNode }) {
  return (
    <div>
      <label className="mb-1 block text-xs font-semibold uppercase tracking-[0.06em] text-text-secondary">{label}</label>
      {children}
      {error && <p className="mt-1 text-sm text-accent">{error}</p>}
    </div>
  );
}

export function JobEditorPage() {
  const { jobId } = useParams<{ jobId: string }>();
  const isEdit = Boolean(jobId);
  const navigate = useNavigate();

  const { data: job, isLoading: loadingJob } = useEmployerJob(jobId);
  const createJob = useCreateJob();
  const updateJob = useUpdateJob(jobId ?? '');

  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [requirements, setRequirements] = useState('');
  const [level, setLevel] = useState<ExperienceLevel>('mid');
  const [city, setCity] = useState('');
  const [country, setCountry] = useState('');
  const [remote, setRemote] = useState(false);
  const [requiredSkills, setRequiredSkills] = useState<SelectedSkill[]>([]);
  const [niceToHaveSkills, setNiceToHaveSkills] = useState<SelectedSkill[]>([]);
  const [hfMinYears, setHfMinYears] = useState('');
  const [hfCountry, setHfCountry] = useState('');
  const [cutoffScore, setCutoffScore] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    if (job) {
      setTitle(job.title);
      setDescription(job.description);
      setRequirements(job.requirements ?? '');
      setLevel(job.experienceLevel);
      setCity(job.city ?? '');
      setCountry(job.country);
      setRemote(job.remote);
      setRequiredSkills(job.requiredSkills.map((s) => ({ id: s.id, name: s.name, mustHave: s.mustHave })));
      setNiceToHaveSkills(job.niceToHaveSkills.map((s) => ({ id: s.id, name: s.name, mustHave: false })));
      setHfMinYears(job.hfMinYears?.toString() ?? '');
      setHfCountry(job.hfCountry ?? '');
      setCutoffScore(job.cutoffScore?.toString() ?? '');
    }
  }, [job]);

  function buildInput(): JobInput {
    return {
      title,
      description,
      requirements,
      experienceLevel: level,
      city,
      country,
      remote,
      requiredSkillIds: requiredSkills.map((s) => s.id),
      niceToHaveSkillIds: niceToHaveSkills.map((s) => s.id),
      mustHaveSkillIds: requiredSkills.filter((s) => s.mustHave).map((s) => s.id),
      hfMinYears: hfMinYears === '' ? null : Number(hfMinYears),
      hfCountry,
      cutoffScore: cutoffScore === '' ? null : Number(cutoffScore),
    };
  }

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setFieldErrors({});
    try {
      if (isEdit) {
        await updateJob.mutateAsync(buildInput());
        navigate(`/employer/jobs/${jobId}`);
      } else {
        const created = await createJob.mutateAsync(buildInput());
        navigate(`/employer/jobs/${created.id}`);
      }
    } catch (err) {
      setError(getApiErrorMessage(err));
      setFieldErrors(getFieldErrors(err));
    }
  }

  if (isEdit && loadingJob) {
    return <Skeleton className="h-96 w-full" />;
  }

  return (
    <div>
      <h1 className="text-[28px] font-bold leading-9">{isEdit ? 'Edit job' : 'New job'}</h1>
      <p className="mb-6 text-text-secondary">
        {isEdit ? 'Only draft jobs can be edited.' : 'Jobs are created as drafts.'}
      </p>

      {error && (
        <div className="mb-4">
          <Banner tone="error">{error}</Banner>
        </div>
      )}

      <form onSubmit={onSubmit} className="space-y-6">
        <section className="space-y-4 border border-border bg-surface p-6">
          <h2 className="text-xl font-semibold">Basics</h2>
          <Field label="Title" error={fieldErrors.title}>
            <Input value={title} onChange={(e) => setTitle(e.target.value)} maxLength={200} required />
          </Field>
          <Field label="Description" error={fieldErrors.description}>
            <Textarea rows={5} value={description} onChange={(e) => setDescription(e.target.value)} required />
          </Field>
          <Field label="Requirements" error={fieldErrors.requirements}>
            <Textarea rows={4} value={requirements} onChange={(e) => setRequirements(e.target.value)} />
          </Field>
        </section>

        <section className="space-y-4 border border-border bg-surface p-6">
          <h2 className="text-xl font-semibold">Skills</h2>
          <div>
            <h3 className="mb-1 text-xs font-semibold uppercase tracking-[0.06em] text-text-secondary">Required</h3>
            <SkillPicker value={requiredSkills} onChange={setRequiredSkills} allowMustHave />
          </div>
          <div>
            <h3 className="mb-1 text-xs font-semibold uppercase tracking-[0.06em] text-text-secondary">Nice to have</h3>
            <SkillPicker value={niceToHaveSkills} onChange={setNiceToHaveSkills} />
          </div>
        </section>

        <section className="space-y-4 border border-border bg-surface p-6">
          <h2 className="text-xl font-semibold">Level and location</h2>
          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Experience level" error={fieldErrors.experienceLevel}>
              <Select value={level} onChange={(e) => setLevel(e.target.value as ExperienceLevel)}>
                {EXPERIENCE_LEVELS.map((l) => (
                  <option key={l} value={l}>
                    {l}
                  </option>
                ))}
              </Select>
            </Field>
            <Field label="City">
              <Input value={city} onChange={(e) => setCity(e.target.value)} />
            </Field>
            <Field label="Country" error={fieldErrors.country}>
              <Input value={country} onChange={(e) => setCountry(e.target.value)} required />
            </Field>
            <div className="flex items-end pb-2">
              <Checkbox checked={remote} onChange={setRemote} label="Remote" />
            </div>
          </div>
        </section>

        <section className="space-y-4 border border-border bg-surface p-6">
          <h2 className="text-xl font-semibold">Hard filters and cut-off</h2>
          <p className="text-sm text-text-secondary">Hard filters exclude applicants automatically (applied later).</p>
          <div className="grid gap-4 sm:grid-cols-3">
            <Field label="Minimum years">
              <Input type="number" min={0} value={hfMinYears} onChange={(e) => setHfMinYears(e.target.value)} />
            </Field>
            <Field label="Required country">
              <Input value={hfCountry} onChange={(e) => setHfCountry(e.target.value)} />
            </Field>
            <Field label="Cut-off score (0–100)">
              <Input type="number" min={0} max={100} value={cutoffScore} onChange={(e) => setCutoffScore(e.target.value)} />
            </Field>
          </div>
        </section>

        <div className="flex gap-3">
          <Button type="submit" loading={createJob.isPending || updateJob.isPending}>
            {isEdit ? 'Save' : 'Create draft'}
          </Button>
          <Button type="button" variant="secondary" onClick={() => navigate(-1)}>
            Cancel
          </Button>
        </div>
      </form>
    </div>
  );
}
