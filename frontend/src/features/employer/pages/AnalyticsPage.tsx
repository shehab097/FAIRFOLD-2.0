import { useState } from 'react';
import { Bar } from 'react-chartjs-2';
import type { ChartData, ChartOptions } from 'chart.js';
import '../../../charts/defaults';
import { CHART_COLORS } from '../../../charts/defaults';
import { useAnalytics } from '../analyticsQueries';
import { useEmployerJobs } from '../queries';
import type {
  AnalyticsFunnelStage,
  AnalyticsOverride,
  AnalyticsTimeToHire,
  AnalyticsFilters,
} from '../analyticsTypes';
import { Badge } from '../../../components/ui/Badge';
import { Banner } from '../../../components/ui/Banner';
import { Button } from '../../../components/ui/Button';
import { ChartWithTable } from '../../../components/ui/ChartWithTable';
import { Icon } from '../../../components/ui/Icon';
import { Input } from '../../../components/ui/Input';
import { Select } from '../../../components/ui/Select';
import { Skeleton } from '../../../components/ui/Skeleton';
import { Table, type Column } from '../../../components/ui/Table';
import { formatDay } from '../../../lib/format';
import { Search } from 'lucide-react';

const STAGE_LABELS: Record<string, string> = {
  applied: 'Applied',
  screened: 'Screened',
  shortlisted: 'Shortlisted',
  interview: 'Interview',
  hired: 'Hired',
};

function monthLabel(month: string): string {
  const [year, value] = month.split('-').map(Number);
  return new Date(year, value - 1, 1).toLocaleDateString('en-GB', {
    month: 'short',
    year: 'numeric',
  });
}

function directionLabel(direction: string): string {
  return direction.replace(/_/g, ' ');
}

export function AnalyticsPage() {
  const [jobId, setJobId] = useState('');
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [filters, setFilters] = useState<AnalyticsFilters>({});
  const { data, isLoading, isError, refetch } = useAnalytics(filters);
  const jobs = useEmployerJobs(undefined, 0);

  function apply() {
    setFilters({
      jobId: jobId || undefined,
      from: from || undefined,
      to: to || undefined,
    });
  }

  if (isLoading) return <Skeleton className="h-80 w-full rounded-xl" />;
  if (isError || !data) {
    return (
      <Banner tone="error">
        Could not load analytics.{' '}
        <button type="button" className="underline" onClick={() => refetch()}>
          Retry
        </button>
      </Banner>
    );
  }

  const funnelChart: ChartData<'bar'> = {
    labels: data.funnel.map((stage) => STAGE_LABELS[stage.stage] ?? stage.stage),
    datasets: [
      {
        data: data.funnel.map((stage) => stage.count),
        backgroundColor: CHART_COLORS.primary,
        borderRadius: 0,
        barPercentage: 0.7,
      },
    ],
  };
  const funnelOptions: ChartOptions<'bar'> = {
    indexAxis: 'y',
    responsive: true,
    maintainAspectRatio: false,
    plugins: { legend: { display: false } },
    scales: {
      x: { beginAtZero: true, ticks: { precision: 0 }, grid: { color: CHART_COLORS.grid } },
      y: { grid: { display: false } },
    },
  };

  const hireChart: ChartData<'bar'> = {
    labels: data.timeToHire.map((row) => monthLabel(row.month)),
    datasets: [
      {
        label: 'Average days',
        data: data.timeToHire.map((row) => row.avgDays),
        backgroundColor: CHART_COLORS.accent,
        borderRadius: 0,
        barPercentage: 0.65,
      },
    ],
  };
  const hireOptions: ChartOptions<'bar'> = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: { legend: { display: false } },
    scales: {
      y: { beginAtZero: true, grid: { color: CHART_COLORS.grid } },
      x: { grid: { display: false } },
    },
  };

  const funnelColumns: Column<AnalyticsFunnelStage>[] = [
    { header: 'Stage', render: (row) => STAGE_LABELS[row.stage] ?? row.stage },
    { header: 'Applications', render: (row) => row.count.toLocaleString() },
  ];
  const hireColumns: Column<AnalyticsTimeToHire>[] = [
    { header: 'Month', render: (row) => monthLabel(row.month) },
    { header: 'Hires', render: (row) => row.hires.toLocaleString() },
    { header: 'Average days', render: (row) => row.avgDays.toFixed(1) },
  ];
  const overrideColumns: Column<AnalyticsOverride>[] = [
    { header: 'Date', render: (row) => formatDay(row.date.slice(0, 10)) },
    { header: 'User', render: (row) => row.userName },
    { header: 'Job', render: (row) => row.jobTitle },
    { header: 'Candidate', render: (row) => row.alias },
    {
      header: 'Direction',
      render: (row) => (
        <Badge tone={row.direction === 'shortlisted_below_cutoff' ? 'warning' : 'failure'}>
          {directionLabel(row.direction)}
        </Badge>
      ),
    },
    { header: 'Reason', render: (row) => row.reason ?? '—' },
  ];

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-[28px] font-bold leading-9">Analytics</h1>
        <p className="text-sm text-text-secondary">
          Hiring funnel, time-to-hire and reason-logged overrides for your company.
        </p>
      </div>

      <div className="grid gap-3 rounded-xl border border-border bg-surface p-4 sm:grid-cols-2 xl:grid-cols-4">
        <label className="block">
          <span className="mb-1 block text-xs font-semibold uppercase tracking-[0.06em] text-text-secondary">
            Job
          </span>
          <Select value={jobId} onChange={(event) => setJobId(event.target.value)}>
            <option value="">All jobs</option>
            {jobs.data?.items.map((job) => (
              <option key={job.id} value={job.id}>
                {job.title}
              </option>
            ))}
          </Select>
        </label>
        <label className="block">
          <span className="mb-1 block text-xs font-semibold uppercase tracking-[0.06em] text-text-secondary">
            From
          </span>
          <Input type="date" value={from} onChange={(event) => setFrom(event.target.value)} />
        </label>
        <label className="block">
          <span className="mb-1 block text-xs font-semibold uppercase tracking-[0.06em] text-text-secondary">
            To
          </span>
          <Input type="date" value={to} onChange={(event) => setTo(event.target.value)} />
        </label>
        <div className="flex items-end">
          <Button onClick={apply}>
            <Icon icon={Search} size={16} /> Apply filters
          </Button>
        </div>
      </div>

      <div className="rounded-xl border border-border bg-surface p-5 shadow-sm">
        <div className="text-xs font-semibold uppercase tracking-[0.06em] text-text-secondary">
          Candidates pulled back into screening
        </div>
        <div className="mt-1 text-3xl font-bold tabular-nums">{data.pullBacks.toLocaleString()}</div>
        <p className="mt-1 text-xs text-text-subtle">
          Applications with a pull-back in the selected range.
        </p>
      </div>

      <ChartWithTable
        title="Pipeline funnel"
        description="Applications created in the selected range, counted once per stage reached."
        chart={
          <div className="h-[280px] rounded-xl border border-border bg-surface p-4 shadow-sm">
            <Bar data={funnelChart} options={funnelOptions} />
          </div>
        }
        table={<Table columns={funnelColumns} rows={data.funnel} rowKey={(row) => row.stage} />}
      />

      <ChartWithTable
        title="Time to hire"
        description="Average days from application to hire, grouped by hiring month."
        chart={
          <div className="h-[280px] rounded-xl border border-border bg-surface p-4 shadow-sm">
            {data.timeToHire.length === 0 ? (
              <div className="flex h-full items-center justify-center text-sm text-text-secondary">
                No hires in this range.
              </div>
            ) : (
              <Bar data={hireChart} options={hireOptions} />
            )}
          </div>
        }
        table={<Table columns={hireColumns} rows={data.timeToHire} rowKey={(row) => row.month} />}
      />

      <section className="space-y-3">
        <div>
          <h2 className="text-lg font-semibold">Overrides</h2>
          <p className="text-sm text-text-secondary">
            Decisions outside the score cut-off with their written reasons. Candidates are shown by
            alias.
          </p>
        </div>
        <Table
          columns={overrideColumns}
          rows={data.overrides}
          rowKey={(row) => `${row.date}-${row.alias}-${row.direction}`}
          empty={
            <div className="rounded-xl border border-border bg-surface p-6 text-sm text-text-secondary">
              No overrides in this range.
            </div>
          }
        />
      </section>
    </div>
  );
}
