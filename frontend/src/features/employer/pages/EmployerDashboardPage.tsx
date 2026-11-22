import { Link } from 'react-router-dom';
import { Briefcase, Inbox, ShieldAlert, Timer, Users } from 'lucide-react';
import { useEmployerDashboard } from '../queries';
import { Banner } from '../../../components/ui/Banner';
import { Button } from '../../../components/ui/Button';
import { Icon } from '../../../components/ui/Icon';
import { Skeleton } from '../../../components/ui/Skeleton';

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
