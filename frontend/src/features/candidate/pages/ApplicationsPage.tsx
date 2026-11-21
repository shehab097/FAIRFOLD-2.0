import { Link, useSearchParams } from 'react-router-dom';
import { useApplications } from '../queries';
import type { ApplicationStatus, CandidateApplicationListItem } from '../types';
import { Banner } from '../../../components/ui/Banner';
import { Badge } from '../../../components/ui/Badge';
import { Button } from '../../../components/ui/Button';
import { Select } from '../../../components/ui/Select';
import { Skeleton } from '../../../components/ui/Skeleton';
import { Table, type Column } from '../../../components/ui/Table';
import { formatDate } from '../../../lib/format';

const STATUSES: ApplicationStatus[] = [
  'applied',
  'screening',
  'screened',
  'not_matched',
  'shortlisted',
  'interview',
  'hired',
  'rejected',
];

export function ApplicationsPage() {
  const [params, setParams] = useSearchParams();
  const status = (params.get('status') ?? '') as ApplicationStatus | '';
  const page = Number(params.get('page') ?? '0');

  const { data, isLoading, isError } = useApplications(status || undefined, page);

  function setStatus(value: string) {
    const next = new URLSearchParams(params);
    if (value) next.set('status', value);
    else next.delete('status');
    next.delete('page');
    setParams(next);
  }

  function setPage(p: number) {
    const next = new URLSearchParams(params);
    next.set('page', String(p));
    setParams(next);
  }

  const columns: Column<CandidateApplicationListItem>[] = [
    {
      header: 'Job',
      render: (a) => (
        <Link to={`/candidate/applications/${a.id}`} className="text-primary-text hover:underline">
          {a.jobTitle ?? 'Job'}
        </Link>
      ),
    },
    { header: 'Company', render: (a) => a.companyName ?? 'Confidential' },
    { header: 'Status', render: (a) => <Badge status={a.status} /> },
    { header: 'Applied', render: (a) => formatDate(a.appliedAt) },
  ];

  const totalPages = data?.totalPages ?? 0;

  return (
    <div>
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="text-[28px] font-bold leading-9">Applications</h1>
          <p className="text-text-secondary">Track the jobs you applied to.</p>
        </div>
      </div>

      <div className="mb-4">
        <Select value={status} onChange={(e) => setStatus(e.target.value)} aria-label="Status filter" className="w-48">
          <option value="">All statuses</option>
          {STATUSES.map((s) => (
            <option key={s} value={s}>
              {s.replace('_', ' ')}
            </option>
          ))}
        </Select>
      </div>

      {isLoading && <Skeleton className="h-64 w-full" />}
      {isError && <Banner tone="error">Could not load your applications.</Banner>}

      {!isLoading && !isError && (
        <Table
          columns={columns}
          rows={data?.items ?? []}
          rowKey={(a) => a.id}
          empty={
            <div className="border border-border bg-surface p-8 text-center text-text-secondary">
              No applications yet.{' '}
              <Link to="/" className="text-primary-text hover:underline">
                Browse jobs
              </Link>
            </div>
          }
        />
      )}

      {totalPages > 1 && (
        <div className="mt-4 flex items-center justify-between">
          <Button variant="secondary" disabled={page === 0} onClick={() => setPage(page - 1)}>
            Previous
          </Button>
          <span className="text-sm text-text-secondary">
            Page {page + 1} of {totalPages}
          </span>
          <Button variant="secondary" disabled={page >= totalPages - 1} onClick={() => setPage(page + 1)}>
            Next
          </Button>
        </div>
      )}
    </div>
  );
}
