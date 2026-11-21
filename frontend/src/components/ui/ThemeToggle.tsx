import { Moon, Sun } from 'lucide-react';
import { useTheme } from '../../app/theme/ThemeContext';
import { cn } from '../../lib/cn';
import { Icon } from './Icon';

interface ThemeToggleProps {
  className?: string;
  showLabel?: boolean;
}

export function ThemeToggle({ className, showLabel = false }: ThemeToggleProps) {
  const { resolvedTheme, toggleTheme } = useTheme();
  const isDark = resolvedTheme === 'dark';

  return (
    <button
      type="button"
      onClick={toggleTheme}
      aria-label={isDark ? 'Switch to light mode' : 'Switch to dark mode'}
      title={isDark ? 'Switch to light mode' : 'Switch to dark mode'}
      className={cn(
        'relative inline-flex h-9 items-center justify-center gap-2 rounded-lg border border-border bg-surface px-2.5 text-text-secondary transition-colors hover:border-border-strong hover:bg-raised hover:text-text focus:outline-none',
        className,
      )}
    >
      <Icon icon={isDark ? Sun : Moon} size={18} className="transition-transform duration-200" />
      {showLabel && (
        <span className="text-xs font-medium">
          {isDark ? 'Light mode' : 'Dark mode'}
        </span>
      )}
    </button>
  );
}

