import { Link } from 'react-router-dom';

/**
 * FairFold brand mark & wordmark:
 * Geometric folded 'F' logo in Burnt Orange (#C2410C), Vibrant Orange (#F97316), and Warm Amber (#FB923C).
 */
export function Logo({ to = '/', subtitle }: { to?: string; subtitle?: string }) {
  return (
    <Link to={to} className="inline-flex flex-col items-start gap-1.5 text-text transition-colors hover:text-primary">
      <span className="inline-flex items-center gap-2.5">
        <svg width="28" height="28" viewBox="0 0 200 200" fill="none" aria-hidden="true" className="shrink-0">
          {/* Vertical Spine (Deep Burnt Orange) */}
          <polygon points="55,30 85,30 85,170 55,150" fill="#C2410C" />
          {/* Top Horizontal Fold (Vibrant Orange) */}
          <polygon points="55,30 155,30 135,60 55,60" fill="#F97316" />
          {/* Middle Arm Fold (Warm Amber) */}
          <polygon points="85,85 135,85 120,112 85,112" fill="#FB923C" />
        </svg>
        <span className="text-lg font-bold leading-none tracking-tight text-text">FairFold</span>
      </span>
      {subtitle && (
        <span className="rounded-md bg-primary-subtle px-2 py-0.5 text-[10px] font-semibold uppercase tracking-[0.06em] text-primary-text">
          {subtitle}
        </span>
      )}
    </Link>
  );
}
