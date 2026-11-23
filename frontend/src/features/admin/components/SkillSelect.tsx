import { useQuery } from '@tanstack/react-query';
import { X } from 'lucide-react';
import { useState } from 'react';
import { suggestSkills } from '../../../api/skills';
import { Icon } from '../../../components/ui/Icon';
import { Input } from '../../../components/ui/Input';

export interface SelectedSkillRef {
  id: string;
  name: string;
}

/** Single-skill picker backed by the shared skill suggestions endpoint. */
export function SkillSelect({
  value,
  onChange,
  disabled = false,
  label = 'Skill',
}: {
  value: SelectedSkillRef | null;
  onChange: (skill: SelectedSkillRef | null) => void;
  disabled?: boolean;
  label?: string;
}) {
  const [q, setQ] = useState('');

  const { data: suggestions = [] } = useQuery({
    queryKey: ['skills', 'suggest', q],
    queryFn: () => suggestSkills(q),
    enabled: q.trim().length > 0 && !disabled && !value,
  });

  if (value) {
    return (
      <div className="flex flex-wrap items-center gap-2">
        <span className="inline-flex items-center gap-2 rounded-full border border-border bg-raised px-3 py-1 text-sm text-text">
          {value.name}
          {!disabled && (
            <button
              type="button"
              onClick={() => onChange(null)}
              aria-label={`Remove ${value.name}`}
              className="text-text-subtle hover:text-text"
            >
              <Icon icon={X} size={14} />
            </button>
          )}
        </span>
      </div>
    );
  }

  return (
    <div>
      <Input
        value={q}
        onChange={(e) => setQ(e.target.value)}
        placeholder={`Search for a ${label.toLowerCase()}…`}
        autoComplete="off"
        disabled={disabled}
        aria-label={label}
      />
      {q.trim().length > 0 && suggestions.length > 0 && (
        <div className="mt-1 max-h-48 overflow-y-auto rounded-lg border border-border bg-raised">
          {suggestions.map((s) => (
            <button
              key={s.id}
              type="button"
              onClick={() => {
                onChange({ id: s.id, name: s.name });
                setQ('');
              }}
              className="block w-full px-3 py-2 text-left text-sm text-text hover:bg-primary-subtle"
            >
              {s.name}
            </button>
          ))}
        </div>
      )}
      {q.trim().length > 0 && suggestions.length === 0 && (
        <p className="mt-1 text-xs text-text-secondary">No matching skills.</p>
      )}
    </div>
  );
}
