import { Link, useSearchParams } from 'react-router-dom';
import { useInterviews } from '../queries';
import type { InterviewListItem } from '../types';
import { Badge } from '../../../components/ui/Badge';
import { Banner } from '../../../components/ui/Banner';
import { Button } from '../../../components/ui/Button';
import { Skeleton } from '../../../components/ui/Skeleton';
import { Table, type Column } from '../../../components/ui/Table';
import { Tabs } from '../../../components/ui/Tabs';
import { formatDate } from '../../../lib/format';

export function InterviewsPage() {
  const [params, setParams] = useSearchParams();
  const tab = params.get('tab') === 'past' ? 'past' : 'upcoming';
  const page = Number(params.get('page') ?? '0');
  const { data, isLoading, isError, refetch } = useInterviews(tab, page);

  function changeTab(next: string) {
    const copy = new URLSearchParams(params);
    if (next === 'upcoming') copy.delete('tab');
    else copy.set('tab', next);
    copy.delete('page');
    setParams(copy);
  }

  function changePage(next: number) {
    const copy = new URLSearchParams(params);
    if (next === 0) copy.delete('page');
    else copy.set('page', String(next));
    setParams(copy);
  }

  const columns: Column<InterviewListItem>[] = [
    {
      header: 'Date and time',
      render: (row) => (
        <Link
          to={`/employer/interviews/${row.id}`}
          className="font-medium text-primary-text hover:underline"
        >
          {formatDate(row.scheduledAt)}
        </Link>
      ),
    },
    { header: 'Candidate', render: (row) => row.candidateName ?? '—' },
    { header: 'Job', render: (row) => row.jobTitle },
    {
      header: 'Interviewers',
      render: (row) => row.interviewers.map((person) => person.name).join(', ') || '—',
    },
    {
      header: 'My feedback',
      render: (row) =>
        row.myFeedbackSubmitted ? (
          <Badge tone="success">Submitted</Badge>
        ) : (
          <Badge tone="neutral">Not submitted</Badge>
        ),
    },
    {
      header: '',
      render: (row) => (
        <Link to={`/employer/interviews/${row.id}`}>
          <Button variant="ghost" className="h-8 px-2 text-xs">
            Open
          </Button>
        </Link>
      ),
    },
  ];

  const totalPages = data?.totalPages ?? 0;

  return (
    <div>
      <div className="mb-6">
        <h1 className="text-[28px] font-bold leading-9">Interviews</h1>
        <p className="text-text-secondary">
          Your upcoming and past interviews. Interviewers see the interviews they are assigned to.
        </p>
      </div>

      <div className="mb-4">
        <Tabs
          tabs={[
            { key: 'upcoming', label: 'Upcoming' },
            { key: 'past', label: 'Past' },
          ]}
          active={tab}
          onChange={changeTab}
        />
      </div>

      {isLoading && <Skeleton className="h-48 w-full rounded-xl" />}
      {isError && (
        <Banner tone="error">
          Could not load interviews.{' '}
          <button type="button" className="underline" onClick={() => refetch()}>
            Retry
          </button>
        </Banner>
      )}

      {!isLoading && !isError && (
        <Table
          columns={columns}
          rows={data?.items ?? []}
          rowKey={(row) => row.id}
          empty={
            <div className="rounded-xl border border-border bg-surface p-8 text-center text-sm text-text-secondary">
              No {tab} interviews.
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
