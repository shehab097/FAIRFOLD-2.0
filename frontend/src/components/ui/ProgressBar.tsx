import { cn } from '../../lib/cn';

export function ProgressBar({
  value,
  max,
  label,
  tone = 'primary',
  className,
}: {
  value: number;
  max: number;
  label?: string;
  tone?: 'primary' | 'danger';
  className?: string;
}) {
  const safeMax = Math.max(0, max);
  const percent = safeMax === 0 ? 0 : Math.min(100, Math.round((value / safeMax) * 100));
  return (
    <div className={className}>
      {label && (
        <div className="mb-1.5 flex items-center justify-between text-xs text-text-secondary">
          <span>{label}</span>
          <span className="tabular-nums">
            {value} / {max}
          </span>
        </div>
      )}
      <div
        className="h-2 overflow-hidden rounded-full bg-raised"
        role="progressbar"
        aria-valuemin={0}
        aria-valuemax={safeMax}
        aria-valuenow={Math.min(value, safeMax)}
        aria-label={label ?? 'Progress'}
      >
        <div
          className={cn('h-full rounded-full', tone === 'danger' ? 'bg-danger' : 'bg-primary')}
          style={{ width: `${percent}%` }}
        />
      </div>
    </div>
  );
}
