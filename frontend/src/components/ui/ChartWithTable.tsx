import { useState, type ReactNode } from 'react';
import { Button } from './Button';

export function ChartWithTable({
  title,
  description,
  chart,
  table,
}: {
  title: string;
  description?: string;
  chart: ReactNode;
  table: ReactNode;
}) {
  const [tableView, setTableView] = useState(false);
  return (
    <section className="space-y-3">
      <div className="flex flex-wrap items-end justify-between gap-2">
        <div>
          <h2 className="text-lg font-semibold">{title}</h2>
          {description && <p className="text-sm text-text-secondary">{description}</p>}
        </div>
        <Button variant="secondary" onClick={() => setTableView((value) => !value)}>
          {tableView ? 'View as chart' : 'View as table'}
        </Button>
      </div>
      {tableView ? table : chart}
    </section>
  );
}
