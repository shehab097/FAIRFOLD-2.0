import { Check } from 'lucide-react';
import { cn } from '../../lib/cn';

interface CheckboxProps {
  checked: boolean;
  onChange: (checked: boolean) => void;
  label?: string;
  disabled?: boolean;
}

export function Checkbox({ checked, onChange, label, disabled = false }: CheckboxProps) {
  return (
    <button
      type="button"
      role="checkbox"
      aria-checked={checked}
      disabled={disabled}
      onClick={() => onChange(!checked)}
      className="inline-flex items-center gap-2 disabled:cursor-not-allowed disabled:opacity-60"
    >
      <span
        className={cn(
          'flex h-[18px] w-[18px] items-center justify-center border',
          checked ? 'border-primary bg-primary text-white' : 'border-border-strong bg-bg text-transparent',
        )}
      >
        <Check size={14} strokeWidth={3} />
      </span>
      {label && <span className="text-sm text-text">{label}</span>}
    </button>
  );
}
