import { Link } from 'react-router-dom';
import { Briefcase, Inbox, ShieldAlert, Timer, Users } from 'lucide-react';
import { useEmployerDashboard } from '../queries';
import type {
  EmployerDashboardApplication,
  EmployerDashboardJob,
  EmployerDashboardPipeline,
} from '../../jobs/types';
import { Badge } from '../../../components/ui/Badge';
import { Banner } from '../../../components/ui/Banner';
import { Button } from '../../../components/ui/Button';
import { Icon } from '../../../components/ui/Icon';
import { Skeleton } from '../../../components/ui/Skeleton';
import { Table, type Column } from '../../../components/ui/Table';
import { formatDate } from '../../../lib/format';

const PIPELINE: { key: keyof EmployerDashboardPipeline; label: string }[] = [
  { key: 'applied', label: 'Applied' },
  { key: 'screening', label: 'Screening' },
  { key: 'screened', label: 'Screened' },
  { key: 'shortlisted', label: 'Shortlisted' },
  { key: 'interview', label: 'Interview' },
  { key: 'hired', label: 'Hired' },
  { key: 'rejected', label: 'Rejected' },
  { key: 'notMatched', label: 'Not matched' },
];

function pipelineShape() {
  return {
    applied: 0,
    screening: 0,
    screened: 0,
    shortlisted: 0,
    interview: 0,
    hired: 0,
    rejected: 0,
    notMatched: 0,
  };
}

export function EmployerDashboardPage() {
  const { data, isLoading, isError, isFetching, refetch } = useEmployerDashboard();

  if (isLoading) return <Skeleton className="h-64 w-full" />;
  if (isError || !data) return <Banner tone="error">Could not load the dashboard.</Banner>;

  const blocks = [
    {
      label: 'Open jobs',
      value: data.openJobs,
      icon: Briefcase,
      to: '/employer/jobs',
      hint: 'Active job posts',
    },
    {
      label: 'New applications',
      value: data.newApplications,
      icon: Inbox,
      to: '/employer/jobs',
      hint: 'Created in the last 7 days',
    },
    {
      label: 'Awaiting screening',
      value: data.awaitingScreening,
      icon: Timer,
      to: '/employer/jobs',
      hint: 'Applicants not screened yet',
    },
    {
      label: 'Bias flags',
      value: data.biasFlags,
      icon: ShieldAlert,
      to: '/employer/jobs',
      hint: 'Screened candidates with a removed claim',
    },
    {
      label: 'Interviews this week',
      value: data.interviewsThisWeek,
      icon: Users,
      to: '/employer/interviews',
      hint: 'Scheduled interview slots',
    },
  ].filter((block) => block.value !== null && block.value !== undefined);

  const recentColumns: Column<EmployerDashboardApplication>[] = [
    {
      header: 'Candidate',
      render: (row) => (
        <Link
          to={`/employer/jobs/${row.jobId}/applications/${row.id}`}
          className="inline-flex rounded-full border border-border bg-raised px-2.5 py-0.5 text-xs font-semibold text-text hover:border-primary-text hover:text-primary-text"
        >
          {row.alias}
        </Link>
      ),
    },
    {
      header: 'Job',
      render: (row) => (
        <Link to={`/employer/jobs/${row.jobId}`} className="text-primary-text hover:underline">
          {row.jobTitle ?? 'Job'}
        </Link>
      ),
    },
    {
      header: 'Score',
      render: (row) =>
        row.score === null ? (
          <span className="text-text-subtle">—</span>
        ) : (
          <span className="font-semibold tabular-nums">{row.score}</span>
        ),
    },
    { header: 'Status', render: (row) => <Badge status={row.status} /> },
    {
      header: 'Bias',
      render: (row) =>
        row.biasStatus ? <Badge status={row.biasStatus} /> : <span className="text-text-subtle">—</span>,
    },
    {
      header: 'Applied',
      render: (row) => <span className="text-text-secondary">{formatDate(row.appliedAt)}</span>,
    },
    {
      header: '',
      render: (row) => (
        <Link to={`/employer/jobs/${row.jobId}/applications/${row.id}`}>
          <Button variant="ghost" className="h-8 px-2 text-xs">
            Review
          </Button>
        </Link>
      ),
    },
  ];

  const jobColumns: Column<EmployerDashboardJob>[] = [
    {
      header: 'Job',
      render: (row) => (
        <Link to={`/employer/jobs/${row.id}`} className="font-medium text-primary-text hover:underline">
          {row.title}
        </Link>
      ),
    },
    { header: 'Applicants', render: (row) => <span className="tabular-nums">{row.applicants}</span> },
    {
      header: 'Awaiting screening',
      render: (row) => <span className="tabular-nums">{row.awaitingScreening}</span>,
    },
    { header: 'Screened', render: (row) => <span className="tabular-nums">{row.screened}</span> },
  ];

  const pipeline = data.pipeline ?? pipelineShape();

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h1 className="text-[28px] font-bold leading-9">Dashboard</h1>
          <p className="text-sm text-text-secondary">
            Hiring activity for your company. Refreshes every 15 seconds. Scores are advisory — you decide.
          </p>
        </div>
        <Button variant="secondary" loading={isFetching} onClick={() => refetch()}>
          Refresh
        </Button>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {blocks.map((block) => (
          <Link
            key={block.label}
            to={block.to}
            className="rounded-xl border border-border bg-surface p-5 shadow-sm transition-colors hover:bg-raised"
          >
            <div className="flex items-center justify-between">
              <span className="text-xs font-semibold uppercase tracking-[0.06em] text-text-secondary">
                {block.label}
              </span>
              <span className="flex h-9 w-9 items-center justify-center rounded-lg bg-primary-subtle text-primary-text">
                <Icon icon={block.icon} size={18} />
              </span>
            </div>
            <div className="mt-4 text-3xl font-bold tabular-nums text-text">{block.value}</div>
            <div className="mt-1 text-xs text-text-subtle">{block.hint}</div>
          </Link>
        ))}
      </div>

      <section className="space-y-3">
        <div>
          <h2 className="text-lg font-semibold">Pipeline</h2>
          <p className="text-sm text-text-secondary">Every application for this company, by current status.</p>
        </div>
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
          {PIPELINE.map((item) => (
            <div key={item.key} className="rounded-xl border border-border bg-surface p-4 shadow-sm">
              <div className="text-xs font-semibold uppercase tracking-[0.06em] text-text-secondary">
                {item.label}
              </div>
              <div className="mt-1 text-2xl font-bold tabular-nums text-text">{pipeline[item.key]}</div>
            </div>
          ))}
        </div>
      </section>

      <section className="space-y-3">
        <div className="flex flex-wrap items-end justify-between gap-2">
          <div>
            <h2 className="text-lg font-semibold">Recent applications</h2>
            <p className="text-sm text-text-secondary">The five newest applications across your jobs.</p>
          </div>
          <Link to="/employer/jobs" className="text-sm text-primary-text hover:underline">
            View all jobs
          </Link>
        </div>
        {data.recentApplications.length === 0 ? (
          <div className="rounded-xl border border-border bg-surface p-8 text-center text-sm text-text-secondary shadow-sm">
            No applications yet.
          </div>
        ) : (
          <Table
            columns={recentColumns}
            rows={data.recentApplications}
            rowKey={(row) => row.id}
          />
        )}
      </section>

      <section className="space-y-3">
        <div>
          <h2 className="text-lg font-semibold">Open jobs</h2>
          <p className="text-sm text-text-secondary">Applicant activity for your active roles.</p>
        </div>
        {data.jobs.length === 0 ? (
          <div className="rounded-xl border border-border bg-surface p-8 text-center text-sm text-text-secondary shadow-sm">
            No active jobs. <Link to="/employer/jobs/new" className="text-primary-text hover:underline">Create one</Link>.
          </div>
        ) : (
          <Table columns={jobColumns} rows={data.jobs} rowKey={(row) => row.id} />
        )}
      </section>

      <div className="rounded-xl border border-border bg-surface p-5 shadow-sm">
        <h2 className="text-base font-semibold">Next steps</h2>
        <ul className="mt-2 space-y-1.5 text-sm text-text-secondary">
          <li>Open a job and use <strong className="text-text">Screen All</strong> to score new applicants.</li>
          <li>Open a candidate to see quoted evidence before shortlisting.</li>
          <li>Shortlisting reveals identity and writes an audit entry.</li>
        </ul>
      </div>
    </div>
  );
}
