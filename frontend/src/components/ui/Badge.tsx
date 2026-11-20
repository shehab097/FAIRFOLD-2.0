import type { LucideIcon } from 'lucide-react';
import type { ReactNode } from 'react';
import { cn } from '../../lib/cn';
import { statusStyle, statusToneClasses, type StatusTone } from '../../lib/statusMap';
import { Icon } from './Icon';

interface BadgeProps {
  status?: string;
  tone?: StatusTone;
  icon?: LucideIcon;
  children?: ReactNode;
}

export function Badge({ status, tone, icon, children }: BadgeProps) {
  if (status) {
    const style = statusStyle(status);
    return (
      <span
        className={cn(
          'inline-flex h-6 items-center gap-1 px-2 text-xs font-semibold uppercase tracking-[0.06em]',
          statusToneClasses(style.tone),
        )}
      >
        <Icon icon={style.icon} size={16} />
        {style.label}
      </span>
    );
  }

  const resolvedTone = tone ?? 'neutral';
  return (
    <span
      className={cn(
        'inline-flex h-6 items-center gap-1 px-2 text-xs font-semibold uppercase tracking-[0.06em]',
        statusToneClasses(resolvedTone),
      )}
    >
      {icon && <Icon icon={icon} size={16} />}
      {children}
    </span>
  );
}
