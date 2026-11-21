import { forwardRef, type TextareaHTMLAttributes } from 'react';
import { cn } from '../../lib/cn';

interface TextareaProps extends TextareaHTMLAttributes<HTMLTextAreaElement> {
  error?: boolean;
}

export const Textarea = forwardRef<HTMLTextAreaElement, TextareaProps>(
  ({ className, error = false, ...props }, ref) => (
    <textarea
      ref={ref}
      className={cn(
        'w-full rounded-sm border bg-bg px-3 py-2 text-sm text-text placeholder:text-text-subtle focus:border-primary focus:outline-none',
        error ? 'border-accent' : 'border-border',
        className,
      )}
      {...props}
    />
  ),
);
Textarea.displayName = 'Textarea';
