import { Link, useSearchParams } from 'react-router-dom';
import { useShortlisted } from '../queries';
import type { ShortlistedCandidate } from '../types';
import { Badge } from '../../../components/ui/Badge';
import { Banner } from '../../../components/ui/Banner';
import { Button } from '../../../components/ui/Button';
import { Skeleton } from '../../../components/ui/Skeleton';
import { Table, type Column } from '../../../components/ui/Table';
import { formatDate } from '../../../lib/format';

export function ShortlistedPage() {
  const [params, setParams] = useSearchParams();
  const page = Number(params.get('page') ?? '0');
  const { data, isLoading, isError, refetch } = useShortlisted(page);

  function changePage(next: number) {
    const copy = new URLSearchParams(params);
    if (next === 0) copy.delete('page');
    else copy.set('page', String(next));
    setParams(copy);
  }

  const columns: Column<ShortlistedCandidate>[] = [
    {
      header: 'Candidate',
      render: (row) => (
        <div>
          <div className="font-medium text-text">{row.candidateName ?? 'Candidate'}</div>
          <div className="text-xs text-text-secondary">{row.candidateEmail}</div>
        </div>
      ),
    },
    {
      header: 'Job',
      render: (row) => (
        <Link
          to={`/employer/jobs/${row.jobId}`}
          className="text-primary-text hover:underline"
        >
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
      header: 'Shortlisted',
      render: (row) => formatDate(row.shortlistedAt),
    },
    {
      header: '',
      render: (row) => (
        <div className="flex justify-end gap-2">
          <Link to={`/employer/jobs/${row.jobId}/applications/${row.applicationId}`}>
            <Button variant="secondary" className="h-8 px-3 text-xs">
              Review
            </Button>
          </Link>
          {row.status === 'interview' && (
            <Link to="/employer/interviews">
              <Button variant="ghost" className="h-8 px-2 text-xs">
                Interviews
              </Button>
            </Link>
          )}
        </div>
      ),
    },
  ];

  const totalPages = data?.totalPages ?? 0;

  return (
    <div>
      <div className="mb-6">
        <h1 className="text-[28px] font-bold leading-9">Shortlisted</h1>
        <p className="text-text-secondary">
          Every candidate you have shortlisted, across all jobs. Identity is visible because a
          shortlist was made and logged.
        </p>
      </div>

      {isLoading && <Skeleton className="h-48 w-full rounded-xl" />}
      {isError && (
        <Banner tone="error">
          Could not load shortlisted candidates.{' '}
          <button type="button" className="underline" onClick={() => refetch()}>
            Retry
          </button>
        </Banner>
      )}

      {!isLoading && !isError && (
        <Table
          columns={columns}
          rows={data?.items ?? []}
          rowKey={(row) => row.applicationId}
          empty={
            <div className="rounded-xl border border-border bg-surface p-8 text-center text-sm text-text-secondary">
              No shortlisted candidates yet. Shortlist applicants from a job workspace.
            </div>
          }
        />
      )}

      {totalPages > 1 && (
        <div className="mt-4 flex items-center justify-between text-sm text-text-secondary">
          <span>
            Page {page + 1} of {totalPages}
          </span>
          <div className="flex gap-2">
            <Button variant="secondary" disabled={page <= 0} onClick={() => changePage(page - 1)}>
              Previous
            </Button>
            <Button
              variant="secondary"
              disabled={page + 1 >= totalPages}
              onClick={() => changePage(page + 1)}
            >
              Next
            </Button>
          </div>
        </div>
      )}
    </div>
  );
}
