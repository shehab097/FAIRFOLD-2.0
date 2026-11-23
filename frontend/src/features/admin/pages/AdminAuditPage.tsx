import { useState } from 'react';
import { Download, Search } from 'lucide-react';
import { useAudit } from '../adminQueries';
import { downloadAuditCsv } from '../../../api/admin';
import type { AuditEntry, AuditFilters } from '../adminTypes';
import { Banner } from '../../../components/ui/Banner';
import { Button } from '../../../components/ui/Button';
import { Icon } from '../../../components/ui/Icon';
import { Input } from '../../../components/ui/Input';
import { Skeleton } from '../../../components/ui/Skeleton';
import { Table, type Column } from '../../../components/ui/Table';
import { useToast } from '../../../components/ui/Toast';
import { getApiErrorMessage } from '../../../lib/errors';
import { formatDate } from '../../../lib/format';

function detailsText(details: unknown): string {
  try {
    return JSON.stringify(details);
  } catch {
    return '{}';
  }
}

export function AdminAuditPage() {
  const { toast } = useToast();
  const [actionInput, setActionInput] = useState('');
  const [resourceInput, setResourceInput] = useState('');
  const [actorInput, setActorInput] = useState('');
  const [fromInput, setFromInput] = useState('');
  const [toInput, setToInput] = useState('');
  const [filters, setFilters] = useState<AuditFilters>({});
  const [page, setPage] = useState(0);
  const [exporting, setExporting] = useState(false);

  const { data, isLoading, isError, refetch } = useAudit(filters, page);

  function apply() {
    const next: AuditFilters = {};
    if (actionInput.trim()) next.action = actionInput.trim();
    if (resourceInput.trim()) next.resourceType = resourceInput.trim();
    if (actorInput.trim()) next.actorId = actorInput.trim();
    if (fromInput) next.from = fromInput;
    if (toInput) next.to = toInput;
    setFilters(next);
    setPage(0);
  }

  async function exportCsv() {
    setExporting(true);
    try {
      await downloadAuditCsv(filters);
      toast('Audit CSV downloaded.', 'success');
    } catch (error) {
      toast(getApiErrorMessage(error), 'error');
    } finally {
      setExporting(false);
    }
  }

  const columns: Column<AuditEntry>[] = [
    {
      header: 'Time',
      render: (row) => (
        <span className="whitespace-nowrap text-sm text-text-secondary">
          {formatDate(row.createdAt)}
        </span>
      ),
    },
    {
      header: 'Actor',
      render: (row) => (
        <span className="text-sm">
          {row.actorEmail ?? <span className="text-text-subtle">Former user</span>}
        </span>
      ),
    },
    {
      header: 'Action',
      render: (row) => <span className="font-mono text-xs text-text">{row.action}</span>,
    },
    {
      header: 'Resource',
      render: (row) => (
        <div className="text-sm">
          <div className="text-text">{row.resourceType}</div>
          <div className="max-w-[180px] truncate font-mono text-xs text-text-subtle">
            {row.resourceId ?? '—'}
          </div>
        </div>
      ),
    },
    {
      header: 'Details',
      render: (row) => (
        <code className="block max-w-[360px] truncate rounded bg-bg px-2 py-1 text-xs text-text-secondary">
          {detailsText(row.details)}
        </code>
      ),
    },
  ];

  return (
    <div className="space-y-5">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="text-[28px] font-bold leading-9">Audit log</h1>
          <p className="text-sm text-text-secondary">
            Every security and business action, filterable by action, resource, user and date.
          </p>
        </div>
        <Button variant="secondary" loading={exporting} onClick={exportCsv}>
          <Icon icon={Download} size={16} /> Export CSV
        </Button>
      </div>

      <div className="grid gap-3 rounded-xl border border-border bg-surface p-4 sm:grid-cols-2 xl:grid-cols-5">
        <label className="block">
          <span className="mb-1 block text-xs font-semibold uppercase tracking-[0.06em] text-text-secondary">
            Action
          </span>
          <Input
            value={actionInput}
            onChange={(event) => setActionInput(event.target.value)}
            placeholder="e.g. admin_unlock"
          />
        </label>
        <label className="block">
          <span className="mb-1 block text-xs font-semibold uppercase tracking-[0.06em] text-text-secondary">
            Resource type
          </span>
          <Input
            value={resourceInput}
            onChange={(event) => setResourceInput(event.target.value)}
            placeholder="e.g. user"
          />
        </label>
        <label className="block">
          <span className="mb-1 block text-xs font-semibold uppercase tracking-[0.06em] text-text-secondary">
            User ID
          </span>
          <Input
            value={actorInput}
            onChange={(event) => setActorInput(event.target.value)}
            placeholder="Actor UUID"
          />
        </label>
        <label className="block">
          <span className="mb-1 block text-xs font-semibold uppercase tracking-[0.06em] text-text-secondary">
            From
          </span>
          <Input type="date" value={fromInput} onChange={(event) => setFromInput(event.target.value)} />
        </label>
        <label className="block">
          <span className="mb-1 block text-xs font-semibold uppercase tracking-[0.06em] text-text-secondary">
            To
          </span>
          <Input type="date" value={toInput} onChange={(event) => setToInput(event.target.value)} />
        </label>
        <div className="flex items-end sm:col-span-2 xl:col-span-5">
          <Button onClick={apply}>
            <Icon icon={Search} size={16} /> Apply filters
          </Button>
        </div>
      </div>

      {isError && (
        <Banner tone="error">
          Could not load the audit log.{' '}
          <button type="button" className="underline" onClick={() => refetch()}>
            Retry
          </button>
        </Banner>
      )}

      {isLoading || !data ? (
        <Skeleton className="h-64 w-full rounded-xl" />
      ) : (
        <>
          <Table
            columns={columns}
            rows={data.items}
            rowKey={(row) => row.id}
            empty={
              <div className="rounded-xl border border-border bg-surface p-6 text-sm text-text-secondary">
                No audit rows match these filters.
              </div>
            }
          />
          <div className="flex flex-wrap items-center justify-between gap-2 text-sm text-text-secondary">
            <span>
              {data.totalItems.toLocaleString()} row{data.totalItems === 1 ? '' : 's'}
            </span>
            <div className="flex items-center gap-2">
              <Button
                variant="secondary"
                disabled={page === 0}
                onClick={() => setPage((value) => Math.max(0, value - 1))}
              >
                Previous
              </Button>
              <span>
                Page {data.totalPages === 0 ? 0 : page + 1} of {data.totalPages}
              </span>
              <Button
                variant="secondary"
                disabled={page + 1 >= data.totalPages}
                onClick={() => setPage((value) => value + 1)}
              >
                Next
              </Button>
            </div>
          </div>
        </>
      )}
    </div>
  );
}
