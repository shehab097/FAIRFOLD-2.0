import { Link } from 'react-router-dom';
import { useDashboard } from '../queries';
import { Banner } from '../../../components/ui/Banner';
import { Button } from '../../../components/ui/Button';
import { Card } from '../../../components/ui/Card';
import { Skeleton } from '../../../components/ui/Skeleton';
import { Table, type Column } from '../../../components/ui/Table';
import { Badge } from '../../../components/ui/Badge';
import { Check } from 'lucide-react';
import { Icon } from '../../../components/ui/Icon';
import { formatDate } from '../../../lib/format';
import type { LatestApplication } from '../types';

function Step({ done, label, to }: { done: boolean; label: string; to: string }) {
  return (
    <div className="flex items-center justify-between border-b border-border px-4 py-3 last:border-b-0">
      <div className="flex items-center gap-3">
        <span
          className={
            done
              ? 'flex h-5 w-5 items-center justify-center bg-primary text-white'
              : 'h-5 w-5 border border-border-strong'
          }
        >
          {done && <Icon icon={Check} size={14} strokeWidth={3} />}
        </span>
        <span className={done ? 'text-text' : 'text-text-secondary'}>{label}</span>
      </div>
      <Button variant="ghost" onClick={() => (window.location.href = to)}>
        {done ? 'Edit' : 'Do it'}
      </Button>
    </div>
  );
}

export function DashboardPage() {
  const { data, isLoading, isError } = useDashboard();

  if (isLoading) return <Skeleton className="h-64 w-full" />;
  if (isError || !data) return <Banner tone="error">Could not load your dashboard.</Banner>;

  const setupComplete =
    data.checklist.profileSaved && data.checklist.resumeUploaded && data.checklist.skillsAdded;

  const columns: Column<LatestApplication>[] = [
    {
      header: 'Job',
      render: (a) => (
        <Link to={`/candidate/applications/${a.id}`} className="text-primary-text hover:underline">
          {a.jobTitle ?? 'Job'}
        </Link>
      ),
    },
    { header: 'Status', render: (a) => <Badge status={a.status} /> },
    { header: 'Applied', render: (a) => formatDate(a.appliedAt) },
  ];

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-[28px] font-bold leading-9">Dashboard</h1>
        <p className="text-text-secondary">
          {setupComplete
            ? 'Your profile is ready — browse jobs and track your applications.'
            : 'Complete your profile to start applying.'}
        </p>
      </div>

      <div className={setupComplete ? 'grid gap-6' : 'grid gap-6 lg:grid-cols-2'}>
        {!setupComplete && (
          <Card className="p-0">
            <h2 className="px-4 pt-4 text-xs font-semibold uppercase tracking-[0.06em] text-text-secondary">
              Setup checklist
            </h2>
            <div className="mt-2">
              <Step done={data.checklist.profileSaved} label="Add your title and location" to="/candidate/profile" />
              <Step done={data.checklist.resumeUploaded} label="Upload your resume" to="/candidate/resume" />
              <Step done={data.checklist.skillsAdded} label="Add your skills" to="/candidate/profile" />
            </div>
          </Card>
        )}

        <Card className="p-0">
          <div className="px-4 pt-4">
            <h2 className="text-xs font-semibold uppercase tracking-[0.06em] text-text-secondary">
              Latest applications
            </h2>
          </div>
          <div className="mt-2">
            {data.latestApplications.length === 0 ? (
              <div className="px-4 pb-4 text-sm text-text-subtle">
                No applications yet.{' '}
                <Link to="/jobs" className="text-primary-text hover:underline">
                  Browse jobs
                </Link>
              </div>
            ) : (
              <Table columns={columns} rows={data.latestApplications} rowKey={(a) => a.id} />
            )}
          </div>
        </Card>
      </div>
    </div>
  );
}
