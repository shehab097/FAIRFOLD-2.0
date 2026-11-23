import { useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { CreditCard, Sparkles } from 'lucide-react';
import { useBilling, useCheckout, usePayments } from '../billingQueries';
import type { BillingPlan, PaymentItem } from '../billingTypes';
import { Badge } from '../../../components/ui/Badge';
import { Banner } from '../../../components/ui/Banner';
import { Button } from '../../../components/ui/Button';
import { Card } from '../../../components/ui/Card';
import { Icon } from '../../../components/ui/Icon';
import { ProgressBar } from '../../../components/ui/ProgressBar';
import { Skeleton } from '../../../components/ui/Skeleton';
import { Table, type Column } from '../../../components/ui/Table';
import { useToast } from '../../../components/ui/Toast';
import { getApiErrorMessage } from '../../../lib/errors';
import { formatDate, formatMoney } from '../../../lib/format';

const STATUS_TONE: Record<PaymentItem['status'], 'success' | 'warning' | 'failure' | 'neutral'> = {
  completed: 'success',
  initiated: 'warning',
  failed: 'failure',
  cancelled: 'neutral',
};

function PlanCard({
  plan,
  current,
  onBuy,
  busy,
}: {
  plan: BillingPlan;
  current: boolean;
  onBuy: () => void;
  busy: boolean;
}) {
  const purchasable = plan.durationDays !== null && plan.priceBdt > 0;
  return (
    <div className="flex flex-col rounded-xl border border-border bg-surface p-5 shadow-sm">
      <div className="flex items-start justify-between gap-2">
        <div>
          <h3 className="text-lg font-semibold">{plan.name}</h3>
          <p className="text-sm text-text-secondary">
            {plan.monthlyScreenings.toLocaleString()} AI screenings / month
          </p>
        </div>
        {current && <Badge tone="success">Current</Badge>}
      </div>
      <div className="mt-4 text-2xl font-bold tabular-nums">
        {plan.priceBdt === 0 ? 'Free' : formatMoney(plan.priceBdt)}
      </div>
      <p className="mt-1 text-xs text-text-subtle">
        {plan.durationDays ? `${plan.durationDays}-day purchase, no auto-renewal` : 'No expiry'}
      </p>
      <div className="mt-4 flex-1" />
      {purchasable ? (
        <Button loading={busy} onClick={onBuy}>
          <Icon icon={CreditCard} size={16} /> Buy {plan.name}
        </Button>
      ) : (
        <p className="text-xs text-text-subtle">Included by default</p>
      )}
    </div>
  );
}

export function BillingPage() {
  const [searchParams] = useSearchParams();
  const [page, setPage] = useState(0);
  const [buyingId, setBuyingId] = useState<string | null>(null);
  const { toast } = useToast();
  const { data, isLoading, isError, refetch } = useBilling();
  const payments = usePayments(page);
  const checkout = useCheckout();
  const result = searchParams.get('payment');

  if (isLoading) return <Skeleton className="h-72 w-full rounded-xl" />;
  if (isError || !data) {
    return (
      <Banner tone="error">
        Could not load billing.{' '}
        <button type="button" className="underline" onClick={() => refetch()}>
          Retry
        </button>
      </Banner>
    );
  }

  async function buy(plan: BillingPlan) {
    setBuyingId(plan.id);
    try {
      const response = await checkout.mutateAsync(plan.id);
      window.location.assign(response.redirectUrl);
    } catch (error) {
      toast(getApiErrorMessage(error), 'error');
      setBuyingId(null);
    }
  }

  const columns: Column<PaymentItem>[] = [
    { header: 'Date', render: (row) => formatDate(row.createdAt) },
    { header: 'Plan', render: (row) => row.planName },
    {
      header: 'Amount',
      render: (row) => <span className="tabular-nums">{formatMoney(row.amountBdt)}</span>,
    },
    {
      header: 'Status',
      render: (row) => (
        <Badge tone={STATUS_TONE[row.status] ?? 'neutral'}>{row.status}</Badge>
      ),
    },
  ];

  const usedPercent = data.quota === 0 ? 0 : Math.min(100, Math.round((data.used / data.quota) * 100));

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-[28px] font-bold leading-9">Billing</h1>
        <p className="text-sm text-text-secondary">
          Plans change monthly AI screening quota only; scores and ranking are never affected.
        </p>
      </div>

      {result === 'success' && (
        <Banner tone="info">
          Payment completed. Your new plan is active now. <Sparkles className="inline" size={14} />
        </Banner>
      )}
      {result === 'failed' && <Banner tone="error">The payment did not complete. You can try again.</Banner>}
      {result === 'cancelled' && <Banner tone="warning">Payment cancelled. Your plan is unchanged.</Banner>}

      <div className="grid gap-4 lg:grid-cols-2">
        <Card>
          <div className="flex items-start justify-between gap-3">
            <div>
              <h2 className="text-base font-semibold">Current plan</h2>
              <p className="mt-1 text-2xl font-bold">{data.plan.name}</p>
            </div>
            <Badge tone="success">{data.plan.monthlyScreenings} / month</Badge>
          </div>
          <p className="mt-2 text-sm text-text-secondary">
            {data.plan.endsAt ? `Active until ${formatDate(data.plan.endsAt)}` : 'Free plan, no expiry'}
          </p>
        </Card>
        <Card>
          <div className="flex items-end justify-between gap-3">
            <div>
              <h2 className="text-base font-semibold">AI screenings this month</h2>
              <p className="mt-1 text-sm text-text-secondary">
                {data.used.toLocaleString()} of {data.quota.toLocaleString()} used
              </p>
            </div>
            <span className="text-lg font-semibold tabular-nums">{usedPercent}%</span>
          </div>
          <div className="mt-4">
            <ProgressBar value={data.used} max={data.quota} />
          </div>
          <p className="mt-2 text-xs text-text-subtle">
            Rule-based screening does not count against quota.
          </p>
        </Card>
      </div>

      <section className="space-y-3">
        <div>
          <h2 className="text-lg font-semibold">Plans</h2>
          <p className="text-sm text-text-secondary">
            Buying a plan ends any active plan now and starts the new 30-day window immediately.
          </p>
        </div>
        <div className="grid gap-4 md:grid-cols-3">
          {data.plans.map((plan) => (
            <PlanCard
              key={plan.id}
              plan={plan}
              current={plan.name === data.plan.name}
              onBuy={() => buy(plan)}
              busy={checkout.isPending && buyingId === plan.id}
            />
          ))}
        </div>
      </section>

      <section className="space-y-3">
        <h2 className="text-lg font-semibold">Payment history</h2>
        {payments.isLoading || !payments.data ? (
          <Skeleton className="h-40 w-full rounded-xl" />
        ) : (
          <>
            <Table
              columns={columns}
              rows={payments.data.items}
              rowKey={(row) => row.id}
              empty={
                <div className="rounded-xl border border-border bg-surface p-6 text-sm text-text-secondary">
                  No payments yet.
                </div>
              }
            />
            <div className="flex items-center justify-end gap-2 text-sm text-text-secondary">
              <Button
                variant="secondary"
                disabled={page === 0}
                onClick={() => setPage((value) => Math.max(0, value - 1))}
              >
                Previous
              </Button>
              <span>
                Page {payments.data.totalPages === 0 ? 0 : page + 1} of{' '}
                {payments.data.totalPages}
              </span>
              <Button
                variant="secondary"
                disabled={page + 1 >= payments.data.totalPages}
                onClick={() => setPage((value) => value + 1)}
              >
                Next
              </Button>
            </div>
          </>
        )}
      </section>
    </div>
  );
}
