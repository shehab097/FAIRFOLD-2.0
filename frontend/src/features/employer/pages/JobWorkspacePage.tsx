import { useState, type ReactNode } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { useApplicants, useEmployerJob, useJobAction } from '../queries';
import type { JobAction } from '../../../api/employerJobs';
import { useAuth } from '../../../app/auth/AuthContext';
import { Badge } from '../../../components/ui/Badge';
import { Banner } from '../../../components/ui/Banner';
import { Button } from '../../../components/ui/Button';
import { Markdown } from '../../../components/ui/Markdown';
import { Skeleton } from '../../../components/ui/Skeleton';
import { Table, type Column } from '../../../components/ui/Table';
import { Tabs } from '../../../components/ui/Tabs';
import { useToast } from '../../../components/ui/Toast';
import { getApiErrorMessage } from '../../../lib/errors';
import { formatDate } from '../../../lib/format';
import { formatSalary } from '../../../lib/salary';
import type { EmployerApplicantListItem, EmployerJobDetail, JobStatus } from '../../jobs/types';

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

function ApplicantsTab({ applicants }: { applicants: EmployerApplicantListItem[] }) {
  const columns: Column<EmployerApplicantListItem>[] = [
    { header: 'Applicant', render: (a) => <span className="font-medium">{a.label}</span> },
    { header: 'Status', render: (a) => <Badge status={a.status} /> },
    { header: 'Applied', render: (a) => formatDate(a.appliedAt) },
  ];
  if (applicants.length === 0) {
    return <div className="rounded border border-border bg-surface p-8 text-center text-text-secondary">No applicants yet.</div>;
  }
  return <Table columns={columns} rows={applicants} rowKey={(a) => a.id} />;
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
          <span key={s.id} className={`rounded-sm bg-raised px-2 py-0.5 text-xs ${muted ? 'text-text-secondary' : 'text-text'}`}>
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
      <article className="divide-y divide-border rounded border border-border bg-surface p-6 sm:p-8">
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
      </article>

      <aside className="lg:sticky lg:top-20 lg:self-start">
        <div className="divide-y divide-border rounded border border-border bg-surface p-5">
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

export function JobWorkspacePage() {
  const { jobId } = useParams<{ jobId: string }>();
  const { data: job, isLoading, isError } = useEmployerJob(jobId);
  const { data: applicants } = useApplicants(jobId);
  const jobAction = useJobAction();
  const navigate = useNavigate();
  const { user } = useAuth();
  const { toast } = useToast();
  const [tab, setTab] = useState('overview');
  const [busy, setBusy] = useState<string | null>(null);

  const isHr = user?.role === 'EMPLOYER_HR';
  const applicantCount = applicants?.totalItems ?? 0;

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
          { key: 'applicants', label: `Applicants (${applicantCount})` },
        ]}
        active={tab}
        onChange={setTab}
      />

      <div className="mt-6">
        {tab === 'overview' ? (
          <Overview job={job} applicantCount={applicantCount} />
        ) : (
          <ApplicantsTab applicants={applicants?.items ?? []} />
        )}
      </div>
    </div>
  );
}
