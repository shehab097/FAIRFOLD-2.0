import { Link } from 'react-router-dom';

/**
 * FairFold brand mark & wordmark:
 * Geometric folded 'F' logo in Deep Forest Green (#225100), Primary Green (#2F6C00), and Electric Lime (#9FE870).
 */
export function Logo({ to = '/', subtitle }: { to?: string; subtitle?: string }) {
  return (
    <Link to={to} className="inline-flex flex-col items-start gap-1.5 text-text transition-colors hover:text-primary">
      <span className="inline-flex items-center gap-2.5">
        <svg width="28" height="28" viewBox="0 0 200 200" fill="none" aria-hidden="true" className="shrink-0">
          {/* Vertical Spine (Deep Forest Green) */}
          <polygon points="55,30 85,30 85,170 55,150" fill="#225100" />
          {/* Top Horizontal Fold (Primary Green) */}
          <polygon points="55,30 155,30 135,60 55,60" fill="#2F6C00" />
          {/* Middle Arm Fold (Electric Lime) */}
          <polygon points="85,85 135,85 120,112 85,112" fill="#9FE870" />
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
