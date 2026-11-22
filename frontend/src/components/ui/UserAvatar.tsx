import { User as UserIcon } from 'lucide-react';
import { cn } from '../../lib/cn';
import { Icon } from './Icon';

export type UserAvatarSize = 'xs' | 'sm' | 'md' | 'lg' | 'xl';

const SIZE_CLASS: Record<UserAvatarSize, string> = {
  xs: 'h-6 w-6 rounded-md text-[10px]',
  sm: 'h-9 w-9 rounded-lg text-xs',
  md: 'h-10 w-10 rounded-lg text-sm',
  lg: 'h-14 w-14 rounded-2xl text-base',
  xl: 'h-20 w-20 rounded-2xl text-xl',
};

interface UserAvatarProps {
  name?: string | null;
  avatarUrl?: string | null;
  size?: UserAvatarSize;
  className?: string;
}

export function UserAvatar({ name, avatarUrl, size = 'sm', className }: UserAvatarProps) {
  const shape = SIZE_CLASS[size];

  if (avatarUrl) {
    return (
      <div
        className={cn(
          'flex aspect-square shrink-0 items-center justify-center overflow-hidden bg-surface',
          shape,
          className,
        )}
      >
        <img
          src={avatarUrl}
          alt={name ? `${name} profile photo` : 'Profile photo'}
          className="block h-full w-full object-cover object-center"
        />
      </div>
    );
  }

  const parts = (name ?? '').trim().split(/\s+/).filter(Boolean);
  const initials = `${parts[0]?.[0] ?? ''}${parts.length > 1 ? parts[parts.length - 1][0] : ''}`.toUpperCase();
  const iconSize = size === 'xl' ? 32 : size === 'lg' ? 24 : 16;

  return (
    <div
      className={cn(
        'flex shrink-0 items-center justify-center bg-primary-subtle font-bold text-primary-text',
        shape,
        className,
      )}
    >
      {initials || <Icon icon={UserIcon} size={iconSize} />}
    </div>
  );
}
