import { cn } from '../../lib/cn';

export interface Tab {
  key: string;
  label: string;
}

export function Tabs({
  tabs,
  active,
  onChange,
}: {
  tabs: Tab[];
  active: string;
  onChange: (key: string) => void;
}) {
  return (
    <div className="border-b border-border">
      <div className="flex gap-4">
        {tabs.map((tab) => (
          <button
            key={tab.key}
            type="button"
            onClick={() => onChange(tab.key)}
            className={cn(
              '-mb-px border-b-2 px-1 py-2 text-sm',
              active === tab.key
                ? 'border-primary font-semibold text-text'
                : 'border-transparent text-text-secondary hover:text-text',
            )}
          >
            {tab.label}
          </button>
        ))}
      </div>
    </div>
  );
}
