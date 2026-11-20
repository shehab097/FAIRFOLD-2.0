import { useQuery } from '@tanstack/react-query';
import { X } from 'lucide-react';
import { useState } from 'react';
import { suggestSkills } from '../../../api/skills';
import { Checkbox } from '../../../components/ui/Checkbox';
import { Icon } from '../../../components/ui/Icon';
import { Input } from '../../../components/ui/Input';

export interface SelectedSkill {
  id: string;
  name: string;
  mustHave: boolean;
}

export function SkillPicker({
  value,
  onChange,
  allowMustHave = false,
}: {
  value: SelectedSkill[];
  onChange: (next: SelectedSkill[]) => void;
  allowMustHave?: boolean;
}) {
  const [q, setQ] = useState('');

  const { data: suggestions = [] } = useQuery({
    queryKey: ['skills', 'suggest', q],
    queryFn: () => suggestSkills(q),
    enabled: q.trim().length > 0,
  });

  function add(skill: { id: string; name: string }) {
    if (value.some((s) => s.id === skill.id)) {
      setQ('');
      return;
    }
    onChange([...value, { id: skill.id, name: skill.name, mustHave: false }]);
    setQ('');
  }

  function remove(id: string) {
    onChange(value.filter((s) => s.id !== id));
  }

  function toggleMustHave(id: string) {
    onChange(value.map((s) => (s.id === id ? { ...s, mustHave: !s.mustHave } : s)));
  }

  return (
    <div>
      <Input value={q} onChange={(e) => setQ(e.target.value)} placeholder="Add a skill…" autoComplete="off" />
      {q.trim().length > 0 && suggestions.length > 0 && (
        <div className="mt-1 border border-border bg-raised">
          {suggestions.map((s) => (
            <button
              key={s.id}
              type="button"
              onClick={() => add(s)}
              className="block w-full px-3 py-2 text-left text-sm text-text hover:bg-primary-subtle"
            >
              {s.name}
            </button>
          ))}
        </div>
      )}
      {value.length > 0 && (
        <div className="mt-2 flex flex-col gap-1">
          {value.map((s) => (
            <div
              key={s.id}
              className="inline-flex w-fit items-center gap-2 border border-border bg-raised px-2 py-1 text-sm text-text"
            >
              <span>{s.name}</span>
              {allowMustHave && (
                <span className="border-l border-border pl-2">
                  <Checkbox checked={s.mustHave} onChange={() => toggleMustHave(s.id)} label="Must have" />
                </span>
              )}
              <button type="button" onClick={() => remove(s.id)} className="text-text-subtle hover:text-text">
                <Icon icon={X} size={14} />
              </button>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
