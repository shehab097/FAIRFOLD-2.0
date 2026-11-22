import { cn } from '../../lib/cn';

export function ScoreMeter({
  score,
  cutoff = null,
  className,
}: {
  score: number | null;
  cutoff?: number | null;
  className?: string;
}) {
  if (score === null) {
    return <span className="text-sm text-text-subtle">Not scored</span>;
  }
  const clamped = Math.max(0, Math.min(100, score));
  return (
    <div className={cn('min-w-[120px]', className)}>
      <div className="mb-1 flex items-baseline gap-2">
        <span className="text-lg font-bold tabular-nums text-text">{score}</span>
        <span className="text-xs text-text-secondary">/ 100</span>
      </div>
      <div
        className="relative h-2 overflow-hidden rounded-full bg-raised"
        role="meter"
        aria-valuemin={0}
        aria-valuemax={100}
        aria-valuenow={clamped}
        aria-label={`Score ${score} out of 100`}
      >
        <div className="h-full rounded-full bg-primary" style={{ width: `${clamped}%` }} />
        {cutoff !== null && (
          <span
            className="absolute top-0 h-full w-0.5 bg-accent"
            style={{ left: `${Math.max(0, Math.min(100, cutoff))}%` }}
            title={`Cut-off ${cutoff}`}
          />
        )}
      </div>
    </div>
  );
}
