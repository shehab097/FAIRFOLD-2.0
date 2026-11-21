import { forwardRef, type InputHTMLAttributes } from 'react';
import { cn } from '../../lib/cn';

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  error?: boolean;
}

export const Input = forwardRef<HTMLInputElement, InputProps>(
  ({ className, error = false, ...props }, ref) => (
    <input
      ref={ref}
      className={cn(
        'h-10 w-full rounded-lg border bg-bg px-3 text-sm text-text placeholder:text-text-subtle transition-colors focus:border-primary focus:outline-none disabled:bg-surface disabled:text-text-subtle',
        error ? 'border-accent' : 'border-border',
        className,
      )}
      {...props}
    />
  ),
);
Input.displayName = 'Input';
