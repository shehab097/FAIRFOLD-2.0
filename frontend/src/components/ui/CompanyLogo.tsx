import { Building2 } from 'lucide-react';
import { cn } from '../../lib/cn';

const SIZES = {
  sm: 'h-8 w-8 rounded-lg text-[11px]',
  md: 'h-10 w-10 rounded-lg text-sm',
  lg: 'h-14 w-14 rounded-xl text-base',
  xl: 'h-20 w-20 rounded-2xl text-xl',
} as const;

interface CompanyLogoProps {
  name: string | null;
  logoUrl: string | null;
  size?: keyof typeof SIZES;
  className?: string;
}

/** Company logo image, falling back to the company initials (or an icon). */
export function CompanyLogo({ name, logoUrl, size = 'md', className }: CompanyLogoProps) {
  const initials = (name ?? '')
    .split(/\s+/)
    .filter(Boolean)
    .map((word) => word[0])
    .join('')
    .slice(0, 2)
    .toUpperCase();

  return (
    <div
      className={cn(
        'flex aspect-square shrink-0 items-center justify-center overflow-hidden border border-border bg-surface font-bold text-primary-text',
        SIZES[size],
        className,
      )}
    >
      {logoUrl ? (
        <img
          src={logoUrl}
          alt={name ? `${name} logo` : 'Company logo'}
          className="block h-full w-full object-cover object-center"
        />
      ) : initials ? (
        <span className="flex h-full w-full items-center justify-center bg-primary-subtle">{initials}</span>
      ) : (
        <span className="flex h-full w-full items-center justify-center bg-primary-subtle">
          <Building2 className="h-1/2 w-1/2" />
        </span>
      )}
    </div>
  );
}
