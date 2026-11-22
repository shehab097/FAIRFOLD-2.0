import { useEffect, useRef, useState, type ReactNode } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import {
  useApplicants,
  useEmployerJob,
  useJobAction,
  usePullBackApplication,
  useRetryScreening,
  useScreeningEstimate,
  useScreeningProgress,
  useStartScreening,
} from '../queries';
import type { JobAction } from '../../../api/employerJobs';
import { useAuth } from '../../../app/auth/AuthContext';
import { Badge } from '../../../components/ui/Badge';
import { Banner } from '../../../components/ui/Banner';
import { Button } from '../../../components/ui/Button';
import { Checkbox } from '../../../components/ui/Checkbox';
import { Dialog } from '../../../components/ui/Dialog';
import { Markdown } from '../../../components/ui/Markdown';
import { ProgressBar } from '../../../components/ui/ProgressBar';
import { ScoreMeter } from '../../../components/ui/ScoreMeter';
import { Select } from '../../../components/ui/Select';
import { Skeleton } from '../../../components/ui/Skeleton';
import { Table, type Column } from '../../../components/ui/Table';
import { Tabs } from '../../../components/ui/Tabs';
import { useToast } from '../../../components/ui/Toast';
import { getApiErrorMessage } from '../../../lib/errors';
import { formatDate } from '../../../lib/format';
import { formatSalary } from '../../../lib/salary';
import { useQueryClient } from '@tanstack/react-query';
import { TriangleAlert } from 'lucide-react';
import type {
  ApplicantFilters,
  EmployerApplicantListItem,
  EmployerJobDetail,
  JobStatus,
} from '../../jobs/types';

const ACTIONS: Record<JobStatus, { label: string; value: JobAction }[]> = {
  draft: [{ label: 'Activate', value: 'activate' }],
  active: [
    { label: 'Pause', value: 'pause' },
    { label: 'Close', value: 'close' },
  ],
  paused: [
    { label: 'Resume', value: 'resume' },
    { label: 'Close', value: 'close' },
  ],
  closed: [],
};

const STATUS_OPTIONS = [
  { value: '', label: 'All statuses' },
  { value: 'applied', label: 'Applied' },
  { value: 'screening', label: 'Screening' },
  { value: 'screened', label: 'Screened' },
  { value: 'not_matched', label: 'Not matched' },
  { value: 'shortlisted', label: 'Shortlisted' },
  { value: 'interview', label: 'Interview' },
  { value: 'hired', label: 'Hired' },
  { value: 'rejected', label: 'Rejected' },
];

const GATE_OPTIONS = [
  { value: '', label: 'All gates' },
  { value: 'not_required', label: 'Not required' },
  { value: 'pending', label: 'Pending' },
  { value: 'passed', label: 'Passed' },
  { value: 'failed', label: 'Failed' },
];

const BIAS_OPTIONS = [
  { value: '', label: 'All bias states' },
  { value: 'clear', label: 'Clear' },
  { value: 'flagged', label: 'Flagged' },
];

function ModeBadge({ mode }: { mode: string | null }) {
  if (!mode) return <span className="text-text-subtle">—</span>;
  if (mode === 'rule_based') {
    return (
      <Badge tone="warning" icon={TriangleAlert}>
        Rule-based
      </Badge>
    );
  }
  return <Badge tone="neutral">AI</Badge>;
}

function Fact({ label, children }: { label: string; children: ReactNode }) {
  return (
    <div className="flex justify-between gap-4 py-1.5 text-sm">
      <dt className="text-text-secondary">{label}</dt>
      <dd className="text-right font-medium text-text">{children}</dd>
    </div>
  );
}

function Chips({ title, skills, muted }: { title: string; skills: EmployerJobDetail['requiredSkills']; muted?: boolean }) {
  return (
    <div>
      <h3 className="mb-1.5 text-xs text-text-secondary">{title}</h3>
      <div className="flex flex-wrap gap-1.5">
        {skills.length === 0 && <span className="text-sm text-text-subtle">None</span>}
        {skills.map((s) => (
          <span key={s.id} className={`rounded-md bg-raised px-2 py-0.5 text-xs ${muted ? 'text-text-secondary' : 'text-text'}`}>
            {s.name}
            {s.mustHave && <span className="ml-1 text-accent">(must)</span>}
          </span>
        ))}
      </div>
    </div>
  );
}

function Overview({ job, applicantCount }: { job: EmployerJobDetail; applicantCount: number }) {
  const hasFilters = job.hfMinYears !== null || job.hfCountry || job.cutoffScore !== null;
  return (
    <div className="grid gap-8 lg:grid-cols-[minmax(0,1fr)_280px]">
      <article className="divide-y divide-border rounded-xl border border-border bg-surface p-6 shadow-sm sm:p-8">
        <section className="pb-6">
          <h2 className="mb-3 text-lg font-semibold">Description</h2>
          <Markdown source={job.description} />
        </section>
        {job.requirements && (
          <section className="pt-6">
            <h2 className="mb-3 text-lg font-semibold">Requirements</h2>
            <Markdown source={job.requirements} />
          </section>
        )}
        {job.screeningQuestions.length > 0 && (
          <section className="pt-6">
            <h2 className="mb-3 text-lg font-semibold">Screening questions</h2>
            <ol className="ml-5 list-decimal space-y-1.5 text-sm text-text">
              {job.screeningQuestions.map((question) => (
                <li key={question}>{question}</li>
              ))}
            </ol>
          </section>
        )}
      </article>

      <aside className="lg:sticky lg:top-20 lg:self-start">
        <div className="divide-y divide-border rounded-xl border border-border bg-surface p-5 shadow-sm">
          <dl className="pb-4">
            <Fact label="Status"><Badge status={job.status} /></Fact>
            <Fact label="Applicants">{applicantCount}</Fact>
            <Fact label="Level"><span className="capitalize">{job.experienceLevel}</span></Fact>
            <Fact label="Location">{[job.city, job.country].filter(Boolean).join(', ')}</Fact>
            <Fact label="Salary">{formatSalary(job)}</Fact>
            <Fact label="Work mode">{job.remote ? 'Remote' : 'On-site'}</Fact>
            <Fact label="Published">{job.publishedAt ? formatDate(job.publishedAt) : '—'}</Fact>
          </dl>
          <div className="space-y-3 py-4">
            <Chips title="Required skills" skills={job.requiredSkills} />
            {job.niceToHaveSkills.length > 0 && <Chips title="Nice to have" skills={job.niceToHaveSkills} muted />}
          </div>
          {hasFilters && (
            <dl className="pt-4">
              {job.hfMinYears !== null && <Fact label="Minimum years">{job.hfMinYears}</Fact>}
              {job.hfCountry && <Fact label="Required country">{job.hfCountry}</Fact>}
              {job.cutoffScore !== null && <Fact label="Cut-off score">{job.cutoffScore}</Fact>}
            </dl>
          )}
        </div>
      </aside>
    </div>
  );
}

function ApplicantsTab({ job, isHr }: { job: EmployerJobDetail; isHr: boolean }) {
  const jobId = job.id;
  const qc = useQueryClient();
  const { toast } = useToast();
  const [filters, setFilters] = useState<ApplicantFilters>({
    includeNotMatched: false,
    page: 0,
    size: 10,
  });
  const [screenOpen, setScreenOpen] = useState(false);
  const { data: applicants, isLoading } = useApplicants(jobId, filters);
  const { data: progress } = useScreeningProgress(jobId);
  const { data: estimate, isLoading: estimating } = useScreeningEstimate(jobId, screenOpen);
  const startScreening = useStartScreening();
  const retryScreening = useRetryScreening();
  const pullBack = usePullBackApplication();
  const lastRunStatus = useRef<string | null>(null);

  useEffect(() => {
    if (!progress) return;
    if (progress.status === 'finished' && lastRunStatus.current !== 'finished') {
      qc.invalidateQueries({ queryKey: ['employer', 'job', jobId, 'applications'] });
      qc.invalidateQueries({ queryKey: ['employer', 'dashboard'] });
    }
    lastRunStatus.current = progress.status;
  }, [progress, jobId, qc]);

  const totalPages = applicants?.totalPages ?? 0;
  const page = filters.page ?? 0;

  function updateFilter(patch: Partial<ApplicantFilters>) {
    setFilters((current) => ({
      ...current,
      ...patch,
      page: 0,
      ...(patch.status === 'not_matched' ? { includeNotMatched: true } : {}),
    }));
  }

  async function confirmScreen() {
    try {
      await startScreening.mutateAsync(jobId);
      setScreenOpen(false);
      toast('Screening started.', 'success');
      qc.invalidateQueries({ queryKey: ['employer', 'job', jobId, 'screening'] });
    } catch (err) {
      toast(getApiErrorMessage(err), 'error');
    }
  }

  async function retryFailed() {
    try {
      await retryScreening.mutateAsync(jobId);
      toast('Failed items queued again.', 'success');
    } catch (err) {
      toast(getApiErrorMessage(err), 'error');
    }
  }

  async function pullBackCandidate(application: EmployerApplicantListItem) {
    try {
      await pullBack.mutateAsync(application.id);
      toast('Candidate pulled back into screening.', 'success');
    } catch (err) {
      toast(getApiErrorMessage(err), 'error');
    }
  }

  const columns: Column<EmployerApplicantListItem>[] = [
    {
      header: 'Rank',
      className: 'w-16',
      render: (a) => (
        <span className="tabular-nums text-text-secondary">
          {(applicants?.items.indexOf(a) ?? 0) + page * (filters.size ?? 10) + 1}
        </span>
      ),
    },
    {
      header: 'Alias',
      render: (a) => (
        <Link
          to={`/employer/jobs/${jobId}/applications/${a.id}`}
          className="inline-flex items-center rounded-full border border-border bg-raised px-2.5 py-0.5 text-xs font-semibold text-text hover:border-primary-text hover:text-primary-text"
        >
          {a.alias}
        </Link>
      ),
    },
    {
      header: 'Score',
      render: (a) => <ScoreMeter score={a.score} cutoff={job.cutoffScore} />,
    },
    {
      header: 'Status',
      render: (a) => (
        <div className="space-y-1">
          <Badge status={a.status} />
          {a.notMatchedRule && (
            <div className="max-w-[220px] text-xs text-text-secondary">{a.notMatchedRule}</div>
          )}
        </div>
      ),
    },
    {
      header: 'Bias',
      render: (a) => (a.biasStatus ? <Badge status={a.biasStatus} /> : <span className="text-text-subtle">—</span>),
    },
    {
      header: 'Gate',
      render: (a) => <Badge status={a.gateStatus} />,
    },
    { header: 'Mode', render: (a) => <ModeBadge mode={a.scoringMode} /> },
    { header: 'Applied', render: (a) => <span className="text-text-secondary">{formatDate(a.appliedAt)}</span> },
    {
      header: '',
      render: (a) => (
        <div className="flex justify-end gap-1">
          <Link to={`/employer/jobs/${jobId}/applications/${a.id}`}>
            <Button variant="ghost" className="h-8 px-2 text-xs">
              Review
            </Button>
          </Link>
          {a.status === 'not_matched' && (
            <Button
              variant="secondary"
              className="h-8 px-2 text-xs"
              loading={pullBack.isPending}
              onClick={() => pullBackCandidate(a)}
            >
              Pull back
            </Button>
          )}
        </div>
      ),
    },
  ];

  return (
    <div className="space-y-5">
      <Banner tone="info">Scores are advisory, you decide. Identities stay hidden until you shortlist.</Banner>

      <div className="flex flex-col gap-3 rounded-xl border border-border bg-surface p-4 shadow-sm sm:flex-row sm:items-end sm:justify-between">
        <div className="grid flex-1 gap-3 sm:grid-cols-4">
          <div>
            <label className="mb-1 block text-xs text-text-secondary">Status</label>
            <Select
              value={filters.status ?? ''}
              onChange={(e) => updateFilter({ status: e.target.value || undefined })}
            >
              {STATUS_OPTIONS.map((o) => (
                <option key={o.value} value={o.value}>
                  {o.label}
                </option>
              ))}
            </Select>
          </div>
          <div>
            <label className="mb-1 block text-xs text-text-secondary">Gate</label>
            <Select
              value={filters.gate ?? ''}
              onChange={(e) => updateFilter({ gate: e.target.value || undefined })}
            >
              {GATE_OPTIONS.map((o) => (
                <option key={o.value} value={o.value}>
                  {o.label}
                </option>
              ))}
            </Select>
          </div>
          <div>
            <label className="mb-1 block text-xs text-text-secondary">Bias</label>
            <Select
              value={filters.bias ?? ''}
              onChange={(e) => updateFilter({ bias: e.target.value || undefined })}
            >
              {BIAS_OPTIONS.map((o) => (
                <option key={o.value} value={o.value}>
                  {o.label}
                </option>
              ))}
            </Select>
          </div>
          <div className="flex items-end pb-2">
            <Checkbox
              checked={Boolean(filters.includeNotMatched)}
              onChange={(checked) => updateFilter({ includeNotMatched: checked })}
              label="Show not matched"
            />
          </div>
        </div>
        {isHr && (
          <Button onClick={() => setScreenOpen(true)}>Screen All</Button>
        )}
      </div>

      {progress?.runId && progress.status && (
        <div className="rounded-xl border border-border bg-surface p-4 shadow-sm">
          <div className="mb-3 flex flex-wrap items-center justify-between gap-3">
            <div className="flex items-center gap-2">
              <Badge status={progress.status} />
              <span className="text-sm text-text-secondary">
                {progress.mode === 'rule_based' ? 'Rule-based' : 'AI'} screening
              </span>
            </div>
            {progress.failed > 0 && (
              <div className="flex items-center gap-2">
                <span className="text-sm text-accent">{progress.failed} failed</span>
                {isHr && (
                  <Button variant="secondary" loading={retryScreening.isPending} onClick={retryFailed}>
                    Retry failed
                  </Button>
                )}
              </div>
            )}
          </div>
          <ProgressBar
            value={progress.done}
            max={progress.total}
            label={`Screening progress · ${progress.done} of ${progress.total} done`}
            tone={progress.failed > 0 ? 'danger' : 'primary'}
          />
        </div>
      )}

      {isLoading ? (
        <Skeleton className="h-48 w-full" />
      ) : (
        <Table
          columns={columns}
          rows={applicants?.items ?? []}
          rowKey={(a) => a.id}
          empty={
            <div className="rounded-xl border border-border bg-surface p-8 text-center text-sm text-text-secondary">
              No applicants match these filters.
            </div>
          }
        />
      )}

      {totalPages > 1 && (
        <div className="flex items-center justify-end gap-2">
          <Button
            variant="secondary"
            disabled={page <= 0}
            onClick={() => setFilters((current) => ({ ...current, page: page - 1 }))}
          >
            Previous
          </Button>
          <span className="text-sm text-text-secondary">
            Page {page + 1} of {totalPages}
          </span>
          <Button
            variant="secondary"
            disabled={page + 1 >= totalPages}
            onClick={() => setFilters((current) => ({ ...current, page: page + 1 }))}
          >
            Next
          </Button>
        </div>
      )}

      <Dialog open={screenOpen} title="Screen All" onClose={() => setScreenOpen(false)}>
        <div className="space-y-4">
          <p className="text-sm text-text-secondary">
            Every new applicant with a ready resume will be scored. Scores are advisory and identities
            stay hidden until you shortlist.
          </p>
          {estimating && <Skeleton className="h-20 w-full" />}
          {estimate && (
            <div className="divide-y divide-border rounded-xl border border-border bg-bg px-4">
              <Fact label="Applicants">{estimate.count}</Fact>
              <Fact label="Mode">
                {estimate.mode === 'rule_based' ? (
                  <Badge tone="warning" icon={TriangleAlert}>Rule-based</Badge>
                ) : (
                  <Badge tone="neutral">AI</Badge>
                )}
              </Fact>
              <Fact label="Estimated cost">
                <span className="tabular-nums">${Number(estimate.estimatedCostUsd).toFixed(4)}</span>
              </Fact>
            </div>
          )}
          <div className="flex justify-end gap-3">
            <Button variant="secondary" onClick={() => setScreenOpen(false)}>
              Cancel
            </Button>
            <Button
              loading={startScreening.isPending}
              disabled={!estimate || estimate.count === 0}
              onClick={confirmScreen}
            >
              Confirm and screen {estimate ? `(${estimate.count})` : ''}
            </Button>
          </div>
        </div>
      </Dialog>
    </div>
  );
}

export function JobWorkspacePage() {
  const { jobId } = useParams<{ jobId: string }>();
  const { data: job, isLoading, isError } = useEmployerJob(jobId);
  const { data: applicantPage } = useApplicants(jobId, { page: 0, size: 1, includeNotMatched: true });
  const jobAction = useJobAction();
  const navigate = useNavigate();
  const { user } = useAuth();
  const { toast } = useToast();
  const [tab, setTab] = useState('overview');
  const [busy, setBusy] = useState<string | null>(null);

  const isHr = user?.role === 'EMPLOYER_HR';

  async function run(action: JobAction) {
    setBusy(action);
    try {
      await jobAction.mutateAsync({ id: jobId as string, action });
      toast('Job updated.', 'success');
    } catch (err) {
      toast(getApiErrorMessage(err), 'error');
    } finally {
      setBusy(null);
    }
  }

  async function copyLink() {
    await navigator.clipboard.writeText(`${window.location.origin}/jobs/${jobId}`);
    toast('Public link copied.');
  }

  if (isLoading) return <Skeleton className="h-64 w-full" />;
  if (isError || !job) return <Banner tone="error">Job not found.</Banner>;

  return (
    <div>
      <Link to="/employer/jobs" className="text-sm text-text-secondary hover:text-primary-text">
        ← All jobs
      </Link>
      <div className="mb-4 mt-2 flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
        <div className="min-w-0">
          <div className="flex flex-wrap items-center gap-3">
            <h1 className="break-words text-[28px] font-bold leading-9">{job.title}</h1>
            <Badge status={job.status} />
          </div>
          <p className="text-sm text-text-secondary">
            {[job.city, job.country].filter(Boolean).join(', ')} · {job.remote ? 'Remote' : 'On-site'} ·{' '}
            <span className="capitalize">{job.experienceLevel}</span>
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          {job.status === 'active' && (
            <Button variant="secondary" onClick={copyLink}>
              Copy public link
            </Button>
          )}
          {isHr && job.status === 'draft' && (
            <Button variant="secondary" onClick={() => navigate(`/employer/jobs/${job.id}/edit`)}>
              Edit
            </Button>
          )}
          {isHr &&
            ACTIONS[job.status].map((a) => (
              <Button
                key={a.value}
                variant={a.value === 'close' ? 'danger' : 'secondary'}
                loading={busy === a.value}
                disabled={busy !== null}
                onClick={() => run(a.value)}
              >
                {a.label}
              </Button>
            ))}
        </div>
      </div>

      <Tabs
        tabs={[
          { key: 'overview', label: 'Overview' },
          { key: 'applicants', label: 'Applicants' },
        ]}
        active={tab}
        onChange={setTab}
      />

      <div className="mt-6">
        {tab === 'overview' ? (
          <Overview job={job} applicantCount={applicantPage?.totalItems ?? 0} />
        ) : (
          <ApplicantsTab job={job} isHr={isHr} />
        )}
      </div>
    </div>
  );
}
