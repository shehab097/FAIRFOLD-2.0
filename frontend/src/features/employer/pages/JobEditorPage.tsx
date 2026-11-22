import { useEffect, useState, type FormEvent } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Plus, Trash2, Wand2 } from 'lucide-react';
import {
  useCreateJob,
  useEmployerJob,
  useSaveScreeningQuestions,
  useSuggestScreeningQuestions,
  useUpdateJob,
} from '../queries';
import { SkillPicker, type SelectedSkill } from '../components/SkillPicker';
import {
  EXPERIENCE_LEVELS,
  SALARY_PERIODS,
  SALARY_TYPES,
  type ExperienceLevel,
  type JobInput,
  type SalaryPeriod,
  type SalaryType,
} from '../../jobs/types';
import { Banner } from '../../../components/ui/Banner';
import { Button } from '../../../components/ui/Button';
import { Checkbox } from '../../../components/ui/Checkbox';
import { Input } from '../../../components/ui/Input';
import { LocationFields } from '../../../components/ui/LocationFields';
import { Select } from '../../../components/ui/Select';
import { Skeleton } from '../../../components/ui/Skeleton';
import { MarkdownEditor } from '../../../components/ui/MarkdownEditor';
import { emptyLocation, type LocationValue } from '../../../lib/locations';
import { CURRENCIES } from '../../../lib/currencies';
import { getApiErrorMessage, getFieldErrors } from '../../../lib/errors';
import { useToast } from '../../../components/ui/Toast';

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
  const suggestQuestions = useSuggestScreeningQuestions();
  const saveQuestions = useSaveScreeningQuestions();
  const { toast } = useToast();

  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [requirements, setRequirements] = useState('');
  const [level, setLevel] = useState<ExperienceLevel>('mid');
  const [location, setLocation] = useState<LocationValue>(emptyLocation);
  const [remote, setRemote] = useState(false);
  const [salaryType, setSalaryType] = useState<SalaryType>('negotiable');
  const [salaryMin, setSalaryMin] = useState('');
  const [salaryMax, setSalaryMax] = useState('');
  const [salaryCurrency, setSalaryCurrency] = useState('USD');
  const [salaryPeriod, setSalaryPeriod] = useState<SalaryPeriod>('year');
  const [salaryNegotiable, setSalaryNegotiable] = useState(true);
  const [requiredSkills, setRequiredSkills] = useState<SelectedSkill[]>([]);
  const [niceToHaveSkills, setNiceToHaveSkills] = useState<SelectedSkill[]>([]);
  const [hfMinYears, setHfMinYears] = useState('');
  const [hfCountry, setHfCountry] = useState('');
  const [cutoffScore, setCutoffScore] = useState('');
  const [questions, setQuestions] = useState<string[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    if (job) {
      setTitle(job.title);
      setDescription(job.description);
      setRequirements(job.requirements ?? '');
      setLevel(job.experienceLevel);
      setLocation({ country: job.country, city: job.city ?? '', address: job.address ?? '' });
      setRemote(job.remote);
      setSalaryType(job.salaryType);
      setSalaryMin(job.salaryMin?.toString() ?? '');
      setSalaryMax(job.salaryMax?.toString() ?? '');
      setSalaryCurrency(job.salaryCurrency || 'USD');
      setSalaryPeriod(job.salaryPeriod);
      setSalaryNegotiable(job.salaryNegotiable);
      setRequiredSkills(job.requiredSkills.map((s) => ({ id: s.id, name: s.name, mustHave: s.mustHave })));
      setNiceToHaveSkills(job.niceToHaveSkills.map((s) => ({ id: s.id, name: s.name, mustHave: false })));
      setHfMinYears(job.hfMinYears?.toString() ?? '');
      setHfCountry(job.hfCountry ?? '');
      setCutoffScore(job.cutoffScore?.toString() ?? '');
      setQuestions(job.screeningQuestions ?? []);
    }
  }, [job]);

  function buildInput(): JobInput {
    return {
      title,
      description,
      requirements,
      experienceLevel: level,
      city: location.city,
      address: location.address,
      country: location.country,
      remote,
      salaryType,
      salaryMin: salaryType === 'negotiable' || salaryMin === '' ? null : Number(salaryMin),
      salaryMax: salaryType === 'range' && salaryMax !== '' ? Number(salaryMax) : null,
      salaryCurrency,
      salaryPeriod,
      salaryNegotiable: salaryType === 'negotiable' ? true : salaryNegotiable,
      requiredSkillIds: requiredSkills.map((s) => s.id),
      niceToHaveSkillIds: niceToHaveSkills.map((s) => s.id),
      mustHaveSkillIds: requiredSkills.filter((s) => s.mustHave).map((s) => s.id),
      hfMinYears: hfMinYears === '' ? null : Number(hfMinYears),
      hfCountry,
      cutoffScore: cutoffScore === '' ? null : Number(cutoffScore),
    };
  }

  async function handleSuggestQuestions() {
    if (!jobId) {
      toast('Create the draft first, then suggest questions.', 'error');
      return;
    }
    try {
      const result = await suggestQuestions.mutateAsync(jobId);
      setQuestions(result.questions);
      toast('Screening questions suggested.', 'success');
    } catch (err) {
      toast(getApiErrorMessage(err), 'error');
    }
  }

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setFieldErrors({});
    try {
      const cleaned = questions.map((q) => q.trim()).filter(Boolean);
      if (isEdit) {
        await updateJob.mutateAsync(buildInput());
        if (job?.status === 'draft') {
          await saveQuestions.mutateAsync({ jobId: jobId as string, questions: cleaned });
        }
        navigate(`/employer/jobs/${jobId}`);
      } else {
        const created = await createJob.mutateAsync(buildInput());
        await saveQuestions.mutateAsync({ jobId: created.id, questions: cleaned });
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
          <Field label="Description (Markdown)" error={fieldErrors.description}>
            <MarkdownEditor rows={12} value={description} onChange={setDescription} required error={Boolean(fieldErrors.description)} />
          </Field>
          <Field label="Requirements (Markdown)" error={fieldErrors.requirements}>
            <MarkdownEditor rows={8} value={requirements} onChange={setRequirements} error={Boolean(fieldErrors.requirements)} />
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
          <Field label="Experience level" error={fieldErrors.experienceLevel}>
            <Select value={level} onChange={(e) => setLevel(e.target.value as ExperienceLevel)}>
              {EXPERIENCE_LEVELS.map((l) => (
                <option key={l} value={l}>
                  {l}
                </option>
              ))}
            </Select>
          </Field>
          <LocationFields
            value={location}
            onChange={setLocation}
            requiredCountry
            countryError={fieldErrors.country}
          />
          <div className="flex items-center">
            <Checkbox checked={remote} onChange={setRemote} label="Remote" />
          </div>
        </section>

        <section className="space-y-4 border border-border bg-surface p-6">
          <h2 className="text-xl font-semibold">Salary</h2>
          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Salary type">
              <Select value={salaryType} onChange={(e) => setSalaryType(e.target.value as SalaryType)}>
                {SALARY_TYPES.map((type) => (
                  <option key={type} value={type}>
                    {type === 'fixed' ? 'Fixed amount' : type === 'range' ? 'Salary range' : 'Negotiable'}
                  </option>
                ))}
              </Select>
            </Field>

            {salaryType !== 'negotiable' && (
              <Field label="Currency">
                <Select value={salaryCurrency} onChange={(e) => setSalaryCurrency(e.target.value)}>
                  {CURRENCIES.map((currency) => (
                    <option key={currency.code} value={currency.code}>
                      {currency.label}
                    </option>
                  ))}
                </Select>
              </Field>
            )}

            {salaryType === 'fixed' && (
              <Field label="Amount">
                <Input
                  type="number"
                  min={0}
                  step="0.01"
                  value={salaryMin}
                  onChange={(e) => setSalaryMin(e.target.value)}
                  required
                  placeholder="e.g. 60000"
                />
              </Field>
            )}

            {salaryType === 'range' && (
              <>
                <Field label="Minimum">
                  <Input
                    type="number"
                    min={0}
                    step="0.01"
                    value={salaryMin}
                    onChange={(e) => setSalaryMin(e.target.value)}
                    required
                    placeholder="e.g. 50000"
                  />
                </Field>
                <Field label="Maximum">
                  <Input
                    type="number"
                    min={0}
                    step="0.01"
                    value={salaryMax}
                    onChange={(e) => setSalaryMax(e.target.value)}
                    required
                    placeholder="e.g. 70000"
                  />
                </Field>
              </>
            )}

            {salaryType !== 'negotiable' && (
              <Field label="Period">
                <Select value={salaryPeriod} onChange={(e) => setSalaryPeriod(e.target.value as SalaryPeriod)}>
                  {SALARY_PERIODS.map((period) => (
                    <option key={period} value={period}>
                      {period === 'year' ? 'Per year' : period === 'month' ? 'Per month' : 'Per hour'}
                    </option>
                  ))}
                </Select>
              </Field>
            )}
          </div>
          {salaryType === 'negotiable' ? (
            <p className="text-sm text-text-secondary">
              No amount is shown. The job will display “Salary negotiable”.
            </p>
          ) : (
            <Checkbox
              checked={salaryNegotiable}
              onChange={setSalaryNegotiable}
              label="Salary is negotiable (uncheck for “not negotiable”)"
            />
          )}
        </section>

        <section className="space-y-4 border border-border bg-surface p-6">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <h2 className="text-xl font-semibold">Screening questions</h2>
              <p className="text-sm text-text-secondary">
                Used later in the interview pack. You can edit the suggested list.
              </p>
            </div>
            <Button
              type="button"
              variant="secondary"
              loading={suggestQuestions.isPending}
              onClick={handleSuggestQuestions}
            >
              <Wand2 size={16} />
              Suggest questions
            </Button>
          </div>
          {questions.length === 0 && (
            <p className="text-sm text-text-subtle">No screening questions yet.</p>
          )}
          <div className="space-y-2">
            {questions.map((question, index) => (
              <div key={index} className="flex items-start gap-2">
                <Input
                  value={question}
                  onChange={(e) =>
                    setQuestions((current) =>
                      current.map((value, i) => (i === index ? e.target.value : value)),
                    )
                  }
                  placeholder={`Question ${index + 1}`}
                />
                <Button
                  type="button"
                  variant="ghost"
                  className="h-10 px-2"
                  aria-label="Remove question"
                  onClick={() => setQuestions((current) => current.filter((_, i) => i !== index))}
                >
                  <Trash2 size={16} />
                </Button>
              </div>
            ))}
          </div>
          <Button
            type="button"
            variant="secondary"
            onClick={() => setQuestions((current) => [...current, ''])}
          >
            <Plus size={16} />
            Add question
          </Button>
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
