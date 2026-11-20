import { useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { useCompany, useDeleteJob, useEmployerJobs, useJobAction } from '../queries';
import type { JobAction } from '../../../api/employerJobs';
import { Badge } from '../../../components/ui/Badge';
import { Banner } from '../../../components/ui/Banner';
import { Button } from '../../../components/ui/Button';
import { Dialog } from '../../../components/ui/Dialog';
import { Select } from '../../../components/ui/Select';
import { Skeleton } from '../../../components/ui/Skeleton';
import { Table, type Column } from '../../../components/ui/Table';
import { useToast } from '../../../components/ui/Toast';
import { getApiErrorMessage } from '../../../lib/errors';
import { formatDate } from '../../../lib/format';
import type { EmployerJobListItem, JobStatus } from '../../jobs/types';

type RowAction = JobAction | 'edit' | 'delete';

function actionsFor(status: JobStatus): { label: string; value: RowAction }[] {
  switch (status) {
    case 'draft':
      return [
        { label: 'Edit', value: 'edit' },
        { label: 'Activate', value: 'activate' },
        { label: 'Duplicate', value: 'duplicate' },
        { label: 'Delete', value: 'delete' },
      ];
    case 'active':
      return [
        { label: 'Pause', value: 'pause' },
        { label: 'Close', value: 'close' },
        { label: 'Duplicate', value: 'duplicate' },
      ];
    case 'paused':
      return [
        { label: 'Resume', value: 'resume' },
        { label: 'Close', value: 'close' },
        { label: 'Duplicate', value: 'duplicate' },
      ];
    case 'closed':
      return [{ label: 'Duplicate', value: 'duplicate' }];
  }
}

export function JobsPage() {
  const [params, setParams] = useSearchParams();
  const status = (params.get('status') ?? '') as JobStatus | '';
  const page = Number(params.get('page') ?? '0');

  const { data, isLoading, isError } = useEmployerJobs(status || undefined, page);
  const { data: company } = useCompany();
  const navigate = useNavigate();
  const { toast } = useToast();
  const jobAction = useJobAction();
  const deleteJob = useDeleteJob();
  const [toDelete, setToDelete] = useState<EmployerJobListItem | null>(null);
  const [busyId, setBusyId] = useState<string | null>(null);

  const incomplete = Boolean(company) && !(company?.name && company?.industry && company?.size);

  function setStatusFilter(value: string) {
    const next = new URLSearchParams(params);
    if (value) next.set('status', value);
    else next.delete('status');
    next.delete('page');
    setParams(next);
  }

  async function runAction(job: EmployerJobListItem, value: RowAction) {
    if (value === 'edit') {
      navigate(`/employer/jobs/${job.id}/edit`);
      return;
    }
    if (value === 'delete') {
      setToDelete(job);
      return;
    }
    setBusyId(job.id);
    try {
      await jobAction.mutateAsync({ id: job.id, action: value });
      toast('Job updated.', 'success');
    } catch (err) {
      toast(getApiErrorMessage(err), 'error');
    } finally {
      setBusyId(null);
    }
  }

  async function confirmDelete() {
    if (!toDelete) return;
    try {
      await deleteJob.mutateAsync(toDelete.id);
      toast('Job deleted.', 'success');
      setToDelete(null);
    } catch (err) {
      toast(getApiErrorMessage(err), 'error');
    }
  }

  const columns: Column<EmployerJobListItem>[] = [
    {
      header: 'Title',
      render: (j) => (
        <Link to={`/employer/jobs/${j.id}`} className="font-medium text-text hover:text-primary-text">
          {j.title}
        </Link>
      ),
    },
    { header: 'Status', render: (j) => <Badge status={j.status} /> },
    { header: 'Applications', render: (j) => String(j.applicationsCount) },
    { header: 'Created', render: (j) => formatDate(j.createdAt) },
    {
      header: '',
      render: (j) => (
        <div className="flex flex-wrap justify-end gap-1">
          {actionsFor(j.status).map((a) => (
            <Button
              key={a.value}
              variant={a.value === 'delete' ? 'danger' : 'ghost'}
              className="h-8 px-2 text-xs"
              loading={busyId === j.id}
              onClick={() => runAction(j, a.value)}
            >
              {a.label}
            </Button>
          ))}
        </div>
      ),
    },
  ];

  return (
    <div>
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="text-[28px] font-bold leading-9">Jobs</h1>
          <p className="text-text-secondary">Post and manage open roles.</p>
        </div>
        <Button onClick={() => navigate('/employer/jobs/new')}>New job</Button>
      </div>

      {incomplete && (
        <div className="mb-6">
          <Banner tone="warning">
            Complete your{' '}
            <Link to="/employer/company" className="underline">
              company profile
            </Link>{' '}
            before activating a job.
          </Banner>
        </div>
      )}

      <div className="mb-4">
        <Select value={status} onChange={(e) => setStatusFilter(e.target.value)} aria-label="Status filter" className="w-48">
          <option value="">All statuses</option>
          <option value="draft">Draft</option>
          <option value="active">Active</option>
          <option value="paused">Paused</option>
          <option value="closed">Closed</option>
        </Select>
      </div>

      {isLoading && (
        <div className="space-y-3">
          {[0, 1, 2, 3].map((i) => (
            <Skeleton key={i} className="h-12 w-full" />
          ))}
        </div>
      )}

      {isError && <Banner tone="error">Could not load jobs.</Banner>}

      {!isLoading && !isError && (
        <Table
          columns={columns}
          rows={data?.items ?? []}
          rowKey={(j) => j.id}
          empty={
            <div className="border border-border bg-surface p-8 text-center text-text-secondary">
              No jobs yet. Create your first job.
            </div>
          }
        />
      )}

      <Dialog open={Boolean(toDelete)} title="Delete job" onClose={() => setToDelete(null)}>
        <p className="text-text">Delete “{toDelete?.title}”? This cannot be undone.</p>
        <div className="mt-6 flex justify-end gap-3">
          <Button variant="secondary" onClick={() => setToDelete(null)}>
            Cancel
          </Button>
          <Button variant="danger" loading={deleteJob.isPending} onClick={confirmDelete}>
            Delete
          </Button>
        </div>
      </Dialog>
    </div>
  );
}
