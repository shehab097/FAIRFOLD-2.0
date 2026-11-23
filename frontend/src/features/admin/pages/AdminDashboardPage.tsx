import { Link } from 'react-router-dom';
import { Bot, Briefcase, DollarSign, FileText, Users } from 'lucide-react';
import { useAdminStats } from '../adminQueries';
import { Banner } from '../../../components/ui/Banner';
import { Card } from '../../../components/ui/Card';
import { Icon } from '../../../components/ui/Icon';
import { Skeleton } from '../../../components/ui/Skeleton';

function label(value: string): string {
  return value.replace(/_/g, ' ').toLowerCase().replace(/^\w/, (c: string) => c.toUpperCase());
}

export function AdminDashboardPage() {
  const { data, isLoading, isError, refetch } = useAdminStats();

  if (isLoading) return <Skeleton className="h-72 w-full rounded-xl" />;
  if (isError || !data) {
    return (
      <Banner tone="error">
        Could not load admin statistics.{' '}
        <button type="button" className="underline" onClick={() => refetch()}>
          Retry
        </button>
      </Banner>
    );
  }

  const totalUsers = Object.values(data.usersByRole).reduce((sum, value) => sum + value, 0);
  const cost = Number(data.aiCostThisMonthUsd ?? 0);
  const blocks = [
    { label: 'Users', value: totalUsers.toLocaleString(), icon: Users, to: '/admin/users', hint: 'All accounts' },
    {
      label: 'Active jobs',
      value: (data.jobsByStatus.active ?? 0).toLocaleString(),
      icon: Briefcase,
      to: '/admin/content',
      hint: 'Published job posts',
    },
    {
      label: 'Applications',
      value: data.applicationsTotal.toLocaleString(),
      icon: FileText,
      to: '/admin/content',
      hint: 'All time',
    },
    {
      label: 'AI calls this month',
      value: data.aiCallsThisMonth.toLocaleString(),
      icon: Bot,
      to: '/admin/audit',
      hint: 'Recorded provider calls',
    },
    {
      label: 'Estimated AI cost',
      value: `$${cost.toFixed(2)}`,
      icon: DollarSign,
      to: '/admin/audit',
      hint: 'This month, USD',
    },
  ];

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-[28px] font-bold leading-9">Admin dashboard</h1>
        <p className="text-sm text-text-secondary">
          Platform counts and AI usage for the current month (UTC).
        </p>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-5">
        {blocks.map((block) => (
          <Link key={block.label} to={block.to} className="group">
            <Card className="h-full transition-colors group-hover:border-border-strong">
              <div className="flex items-center justify-between">
                <span className="text-xs font-semibold uppercase tracking-[0.06em] text-text-secondary">
                  {block.label}
                </span>
                <Icon icon={block.icon} size={18} className="text-primary-text" />
              </div>
              <div className="mt-3 text-[28px] font-bold leading-9 text-text">{block.value}</div>
              <div className="mt-1 text-xs text-text-subtle">{block.hint}</div>
            </Card>
          </Link>
        ))}
      </div>

      <div className="grid gap-4 lg:grid-cols-2">
        <Card>
          <h2 className="text-lg font-semibold">Users by role</h2>
          <dl className="mt-3 divide-y divide-border">
            {Object.entries(data.usersByRole).map(([role, count]) => (
              <div key={role} className="flex items-center justify-between py-2.5 text-sm">
                <dt className="text-text-secondary">{label(role)}</dt>
                <dd className="font-semibold text-text">{count.toLocaleString()}</dd>
              </div>
            ))}
          </dl>
        </Card>
        <Card>
          <h2 className="text-lg font-semibold">Jobs by status</h2>
          <dl className="mt-3 divide-y divide-border">
            {Object.entries(data.jobsByStatus).map(([status, count]) => (
              <div key={status} className="flex items-center justify-between py-2.5 text-sm">
                <dt className="text-text-secondary">{label(status)}</dt>
                <dd className="font-semibold text-text">{count.toLocaleString()}</dd>
              </div>
            ))}
          </dl>
        </Card>
      </div>
    </div>
  );
}
