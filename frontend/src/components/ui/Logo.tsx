import { Link } from 'react-router-dom';

/**
 * Clean FairFold brand mark & wordmark:
 * Rounded purple square with a folded coral corner and crisp 'F' monogram.
 */
export function Logo({ to = '/', subtitle }: { to?: string; subtitle?: string }) {
  return (
    <Link to={to} className="inline-flex items-center gap-2.5 text-text hover:text-primary-text">
      <svg width="28" height="28" viewBox="0 0 32 32" fill="none" aria-hidden="true" className="shrink-0">
        <rect width="32" height="32" rx="7" className="fill-primary" />
        <path d="M22 0H25C28.866 0 32 3.13401 32 7V10L22 0Z" className="fill-accent" />
        <path d="M22 0V7C22 8.65685 23.3431 10 25 10H32L22 0Z" className="fill-primary-pressed" />
        <path d="M9 8H20V11.5H12.5V14.8H18.5V18.2H12.5V24H9V8Z" className="fill-text" />
      </svg>
      <span className="flex items-center gap-2">
        <span className="text-lg font-bold tracking-tight text-text">FairFold</span>
        {subtitle && (
          <span className="rounded-md bg-primary-subtle px-2 py-0.5 text-[10px] font-semibold uppercase tracking-[0.06em] text-primary-text">
            {subtitle}
          </span>
        )}
      </span>
    </Link>
  );
}
