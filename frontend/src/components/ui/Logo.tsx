import { Link } from 'react-router-dom';

/**
 * FairFold brand mark per design.md §4:
 * A solid purple square with one corner cut off diagonally in coral (a straight fold).
 */
export function Logo({ to = '/', subtitle }: { to?: string; subtitle?: string }) {
  return (
    <Link to={to} className="inline-flex items-center gap-2.5 text-text hover:text-primary-text">
      <svg width="26" height="26" viewBox="0 0 26 26" aria-hidden="true" className="shrink-0">
        <polygon points="0,0 18,0 26,8 26,26 0,26" className="fill-primary" />
        <polygon points="18,0 26,0 26,8" className="fill-accent" />
        <polygon points="18,0 18,8 26,8" className="fill-primary-pressed" />
      </svg>
      <span className="flex items-baseline gap-2">
        <span className="text-xl font-bold leading-6 tracking-tight text-text">FairFold</span>
        {subtitle && (
          <span className="border border-border bg-raised px-1.5 py-0.5 text-[10px] font-semibold uppercase tracking-[0.06em] text-primary-text">
            {subtitle}
          </span>
        )}
      </span>
    </Link>
  );
}

