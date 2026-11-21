import { Info, TriangleAlert, X } from 'lucide-react';
import type { ReactNode } from 'react';
import { cn } from '../../lib/cn';
import { Icon } from './Icon';

export type BannerTone = 'info' | 'warning' | 'error';

const TONE_CLASSES: Record<BannerTone, string> = {
  info: 'bg-surface border-l-primary',
  warning: 'bg-accent-subtle border-l-accent',
  error: 'bg-danger-subtle border-l-danger',
};

const TONE_ICONS = {
  info: Info,
  warning: TriangleAlert,
  error: X,
};

export function Banner({ tone = 'info', children }: { tone?: BannerTone; children: ReactNode }) {
  return (
    <div className={cn('flex items-start gap-2 rounded-xl border-l-4 px-4 py-3 text-sm text-text shadow-sm', TONE_CLASSES[tone])}>
      <Icon icon={TONE_ICONS[tone]} size={20} className="mt-0.5 shrink-0" />
      <div className="min-w-0">{children}</div>
    </div>
  );
}
