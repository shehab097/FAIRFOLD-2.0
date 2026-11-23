import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Bar } from 'react-chartjs-2';
import type { ChartOptions } from 'chart.js';
import '../../../charts/defaults';
import { CHART_COLORS } from '../../../charts/defaults';
import { useJourney } from '../queries';
import { Badge } from '../../../components/ui/Badge';
import { Banner } from '../../../components/ui/Banner';
import { Button } from '../../../components/ui/Button';
import { Card } from '../../../components/ui/Card';
import { Skeleton } from '../../../components/ui/Skeleton';
import { Table, type Column } from '../../../components/ui/Table';
import type { JourneySkill, JourneyTimelineItem } from '../journeyTypes';

function monthIndex(value: string | null): number | null {
  if (!value) return null;
  const [year, month] = value.split('-').map(Number);
  if (!year || !month) return null;
  return year * 12 + month - 1;
}

function indexLabel(index: number): string {
  const year = Math.floor(index / 12);
  const month = index % 12;
  return new Date(year, month, 1).toLocaleDateString('en-GB', {
    month: 'short',
    year: 'numeric',
  });
}

function monthLabel(value: string | null): string {
  const index = monthIndex(value);
  return index === null ? '—' : indexLabel(index);
}

function chartOptions(min: number, max: number): ChartOptions<'bar'> {
  return {
    indexAxis: 'y',
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { display: false },
      tooltip: {
        callbacks: {
          label: (context) => {
            const raw = context.raw as [number, number];
            return `${indexLabel(min + raw[0])} – ${indexLabel(min + raw[1] - 1)}`;
          },
        },
      },
    },
    scales: {
      x: {
        min: 0,
        max: Math.max(1, max - min + 1),
        grid: { color: CHART_COLORS.grid },
        ticks: {
          callback: (value) => indexLabel(min + Number(value)),
        },
      },
      y: { grid: { display: false } },
    },
  };
}

export function JourneyPage() {
  const { data, isLoading, isError, refetch } = useJourney();
  const [timelineTable, setTimelineTable] = useState(false);
  const [skillsTable, setSkillsTable] = useState(false);

  if (isLoading) return <Skeleton className="h-96 w-full rounded-xl" />;
  if (isError || !data) {
    return (
      <Banner tone="error">
        Could not load your journey.{' '}
        <button type="button" className="underline" onClick={() => refetch()}>
          Retry
        </button>
      </Banner>
    );
  }

  const timeline = data.timeline;
  const skills = data.skills;

  const timelineRows = timeline
    .map((item) => {
      const start = monthIndex(item.startMonth);
      const end = item.isCurrent ? monthIndex(new Date().toISOString().slice(0, 7)) : monthIndex(item.endMonth);
      return start === null || end === null ? null : { item, start, end: Math.max(start, end) };
    })
    .filter((row): row is { item: JourneyTimelineItem; start: number; end: number } => row !== null);

  const skillRows = skills
    .map((skill) => {
      const start = monthIndex(skill.firstUsedMonth);
      const end = monthIndex(skill.lastUsedMonth);
      return start === null || end === null
        ? null
        : { skill, start, end: Math.max(start, end) };
    })
    .filter((row): row is { skill: JourneySkill; start: number; end: number } => row !== null);

  const timelineMin = timelineRows.length ? Math.min(...timelineRows.map((r) => r.start)) : 0;
  const timelineMax = timelineRows.length ? Math.max(...timelineRows.map((r) => r.end)) : 1;
  const skillMin = skillRows.length ? Math.min(...skillRows.map((r) => r.start)) : 0;
  const skillMax = skillRows.length ? Math.max(...skillRows.map((r) => r.end)) : 1;

  const timelineColumns: Column<JourneyTimelineItem>[] = [
    { header: 'Role', render: (row) => <span className="font-medium">{row.title}</span> },
    { header: 'Employer', render: (row) => row.employer ?? '—' },
    { header: 'Start', render: (row) => monthLabel(row.startMonth) },
    { header: 'End', render: (row) => (row.isCurrent ? 'Present' : monthLabel(row.endMonth)) },
  ];

  const skillColumns: Column<JourneySkill>[] = [
    { header: 'Skill', render: (row) => <span className="font-medium">{row.name}</span> },
    { header: 'Level', render: (row) => row.level ?? '—' },
    { header: 'First used', render: (row) => monthLabel(row.firstUsedMonth) },
    { header: 'Last used', render: (row) => monthLabel(row.lastUsedMonth) },
    {
      header: 'Roles',
      render: (row) => row.roleTitles.join(', ') || '—',
    },
  ];

  if (timeline.length === 0 && skills.length === 0) {
    return (
      <div className="space-y-4">
        <h1 className="text-[28px] font-bold leading-9">Journey</h1>
        <Card>
          <h2 className="text-xl font-semibold">Your career timeline is empty</h2>
          <p className="mt-1 text-sm text-text-secondary">
            Upload a resume or add work history and skills to see your journey.
          </p>
          <div className="mt-4 flex flex-wrap gap-3">
            <Link to="/candidate/resume">
              <Button>Upload resume</Button>
            </Link>
            <Link to="/candidate/profile">
              <Button variant="secondary">Edit profile</Button>
            </Link>
          </div>
        </Card>
      </div>
    );
  }

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-[28px] font-bold leading-9">Journey</h1>
        <p className="text-text-secondary">
          Your roles over time and when each skill was first and last used. Only you see employer
          names.
        </p>
      </div>

      <section>
        <div className="mb-3 flex flex-wrap items-center justify-between gap-2">
          <h2 className="text-xl font-semibold">Career timeline</h2>
          <Button variant="secondary" onClick={() => setTimelineTable((value) => !value)}>
            {timelineTable ? 'View as chart' : 'View as table'}
          </Button>
        </div>
        {timeline.length === 0 ? (
          <div className="rounded-xl border border-border bg-surface p-6 text-sm text-text-secondary">
            Add work history on your profile to build the timeline.
          </div>
        ) : timelineTable ? (
          <Table columns={timelineColumns} rows={timeline} rowKey={(row) => row.id} />
        ) : timelineRows.length === 0 ? (
          <div className="rounded-xl border border-border bg-surface p-6 text-sm text-text-secondary">
            Add start and end months to your roles to plot them.
          </div>
        ) : (
          <div
            className="rounded-xl border border-border bg-surface p-4 shadow-sm"
            style={{ height: Math.max(220, timelineRows.length * 58) }}
          >
            <Bar
              data={{
                labels: timelineRows.map((row) => row.item.title),
                datasets: [
                  {
                    data: timelineRows.map((row) => [row.start - timelineMin, row.end - timelineMin + 1]),
                    backgroundColor: CHART_COLORS.primary,
                    borderRadius: 0,
                    barPercentage: 0.65,
                  },
                ],
              }}
              options={chartOptions(timelineMin, timelineMax)}
            />
          </div>
        )}
      </section>

      <section>
        <div className="mb-3 flex flex-wrap items-center justify-between gap-2">
          <h2 className="text-xl font-semibold">Skill evolution</h2>
          <Button variant="secondary" onClick={() => setSkillsTable((value) => !value)}>
            {skillsTable ? 'View as chart' : 'View as table'}
          </Button>
        </div>
        {skills.length === 0 ? (
          <div className="rounded-xl border border-border bg-surface p-6 text-sm text-text-secondary">
            Add skills on your profile and work history to see how they evolved.
          </div>
        ) : skillsTable ? (
          <Table columns={skillColumns} rows={skills} rowKey={(row) => row.skillId} />
        ) : skillRows.length === 0 ? (
          <div className="rounded-xl border border-border bg-surface p-6 text-sm text-text-secondary">
            Skills without work-history dates appear in the table view.
          </div>
        ) : (
          <div
            className="rounded-xl border border-border bg-surface p-4 shadow-sm"
            style={{ height: Math.max(220, skillRows.length * 38) }}
          >
            <Bar
              data={{
                labels: skillRows.map((row) => row.skill.name),
                datasets: [
                  {
                    data: skillRows.map((row) => [row.start - skillMin, row.end - skillMin + 1]),
                    backgroundColor: CHART_COLORS.accent,
                    borderRadius: 0,
                    barPercentage: 0.65,
                  },
                ],
              }}
              options={chartOptions(skillMin, skillMax)}
            />
          </div>
        )}
      </section>

      <div className="flex flex-wrap gap-2">
        {skills.some((skill) => skill.verified) && <Badge tone="success">Verified skills included</Badge>}
        <Link to="/candidate/profile" className="text-sm text-primary-text hover:underline">
          Edit profile and work history
        </Link>
      </div>
    </div>
  );
}
