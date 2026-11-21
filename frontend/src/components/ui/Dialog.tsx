import { X } from 'lucide-react';
import type { ReactNode } from 'react';
import { Icon } from './Icon';

export function Dialog({
  open,
  title,
  onClose,
  children,
}: {
  open: boolean;
  title: string;
  onClose: () => void;
  children: ReactNode;
}) {
  if (!open) return null;
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-scrim p-4" onClick={onClose}>
      <div
        className="w-full max-w-[520px] rounded-2xl border border-border-strong bg-raised p-6 shadow-xl"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="mb-4 flex items-center justify-between">
          <h2 className="text-xl font-semibold text-text">{title}</h2>
          <button onClick={onClose} className="text-text-secondary hover:text-text">
            <Icon icon={X} size={20} />
          </button>
        </div>
        {children}
      </div>
    </div>
  );
}
