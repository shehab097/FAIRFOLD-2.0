import type { DroppedPlaceholder } from '../types';

const PLACEHOLDER = /(\[(?:EMPLOYER|INSTITUTION|NAME|EMAIL|PHONE|URL|YEAR|DATE)(?:_\d+)?\])/g;

const LABELS: Record<string, string> = {
  NAME: 'Name',
  EMAIL: 'Email',
  PHONE: 'Phone',
  URL: 'Link',
  YEAR: 'Year',
  DATE: 'Date',
};

export function StrippedPreview({
  text,
  placeholders,
}: {
  text: string;
  placeholders: DroppedPlaceholder[];
}) {
  const byPlaceholder = new Map(placeholders.map((p) => [p.placeholder, p]));
  const parts = text.split(PLACEHOLDER);

  return (
    <div className="whitespace-pre-wrap break-words text-sm leading-7 text-text">
      {parts.map((part, index) => {
        const match = part.match(/^\[([A-Z]+)(?:_\d+)?\]$/);
        if (!match) return <span key={index}>{part}</span>;
        const key = part.slice(1, -1);
        const placeholder = byPlaceholder.get(key);
        const label = placeholder
          ? placeholder.kind === 'employer'
            ? 'Employer'
            : 'Institution'
          : LABELS[match[1]] ?? 'Removed';
        return (
          <span
            key={index}
            title={placeholder ? `Replaced: ${placeholder.originalValue}` : 'Personal detail removed'}
            className="mx-0.5 inline-flex items-center rounded-full border border-border bg-primary-subtle px-2 py-0.5 text-xs font-semibold text-primary-text"
          >
            {label}
          </span>
        );
      })}
    </div>
  );
}
