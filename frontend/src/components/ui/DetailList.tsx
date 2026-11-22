import type { ReactNode } from 'react';
import { cn } from '../../lib/cn';

export function DetailItem({ label, value }: { label: string; value: ReactNode }) {
  const empty = value === null || value === undefined || value === '';
  return (
    <div className="min-w-0">
      <dt className="text-xs text-text-secondary">{label}</dt>
      <dd className="mt-0.5 break-words text-sm font-medium text-text">
        {empty ? <span className="font-normal text-text-subtle">Not specified</span> : value}
      </dd>
    </div>
  );
}

export function DetailGrid({
  children,
  className,
}: {
  children: ReactNode;
  className?: string;
}) {
  return <dl className={cn('grid gap-x-8 gap-y-4 sm:grid-cols-2', className)}>{children}</dl>;
}

export function DetailSection({
  title,
  description,
  children,
}: {
  title: string;
  description?: string;
  children: ReactNode;
}) {
  return (
    <section>
      <h3 className="text-sm font-semibold text-text">{title}</h3>
      {description && <p className="mt-0.5 text-xs text-text-secondary">{description}</p>}
      <div className="mt-3">{children}</div>
    </section>
  );
}
