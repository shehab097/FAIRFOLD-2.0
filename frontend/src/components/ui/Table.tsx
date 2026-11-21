import type { ReactNode } from 'react';
import { cn } from '../../lib/cn';

export interface Column<T> {
  header: string;
  render: (row: T) => ReactNode;
  className?: string;
}

export function Table<T>({
  columns,
  rows,
  rowKey,
  empty,
}: {
  columns: Column<T>[];
  rows: T[];
  rowKey: (row: T) => string;
  empty?: ReactNode;
}) {
  if (rows.length === 0 && empty) {
    return <>{empty}</>;
  }
  return (
    <div className="overflow-x-auto rounded-xl border border-border bg-surface shadow-sm">
      <table className="w-full border-collapse text-sm">
        <thead>
          <tr className="bg-bg">
            {columns.map((c) => (
              <th
                key={c.header}
                className={cn(
                  'px-4 py-3 text-left text-xs font-semibold uppercase tracking-[0.06em] text-text-secondary',
                  c.className,
                )}
              >
                {c.header}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <tr key={rowKey(row)} className="border-t border-border hover:bg-raised">
              {columns.map((c) => (
                <td key={c.header} className={cn('h-12 px-4 align-middle text-text', c.className)}>
                  {c.render(row)}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
