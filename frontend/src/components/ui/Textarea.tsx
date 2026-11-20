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
        'w-full border bg-bg px-3 py-2 text-sm text-text placeholder:text-text-subtle',
        error ? 'border-accent' : 'border-border-strong',
        className,
      )}
      {...props}
    />
  ),
);
Textarea.displayName = 'Textarea';
