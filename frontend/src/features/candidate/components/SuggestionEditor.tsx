import { useEffect, useState } from 'react';
import { Plus, Trash2 } from 'lucide-react';
import type {
  CandidateSkillLevel,
  ResumeSuggestionSkill,
  ResumeSuggestionWork,
  ResumeSuggestions,
  WorkHistoryInput,
} from '../types';
import { EDUCATION_LEVELS, SKILL_LEVELS } from '../types';
import { Button } from '../../../components/ui/Button';
import { Checkbox } from '../../../components/ui/Checkbox';
import { Dialog } from '../../../components/ui/Dialog';
import { Icon } from '../../../components/ui/Icon';
import { Input } from '../../../components/ui/Input';
import { Select } from '../../../components/ui/Select';
import { useToast } from '../../../components/ui/Toast';
import { getApiErrorMessage } from '../../../lib/errors';

interface EditableSkill extends ResumeSuggestionSkill {
  checked: boolean;
}

interface EditableWork extends ResumeSuggestionWork {
  key: number;
}

interface Props {
  suggestions: ResumeSuggestions;
  saving: boolean;
  onSave: (input: {
    skills: { name: string; level: string; years: number | null }[];
    totalExperienceYears: number | null;
    educationLevel: string | null;
    jobTitles: string[];
    workHistory: WorkHistoryInput[];
  }) => Promise<void>;
}

let nextKey = 1;

export function SuggestionEditor({ suggestions, saving, onSave }: Props) {
  const { toast } = useToast();
  const [skills, setSkills] = useState<EditableSkill[]>([]);
  const [total, setTotal] = useState('');
  const [education, setEducation] = useState('');
  const [titles, setTitles] = useState('');
  const [work, setWork] = useState<EditableWork[]>([]);
  const [confirmOpen, setConfirmOpen] = useState(false);

  useEffect(() => {
    setSkills(suggestions.skills.map((s) => ({ ...s, checked: true })));
    setTotal(suggestions.totalExperienceYears?.toString() ?? '');
    setEducation(suggestions.educationLevel ?? '');
    setTitles(suggestions.jobTitles.join('\n'));
    setWork(suggestions.workHistory.map((w) => ({ ...w, key: nextKey++ })));
  }, [suggestions]);

  function updateSkill(index: number, patch: Partial<EditableSkill>) {
    setSkills((current) => current.map((s, i) => (i === index ? { ...s, ...patch } : s)));
  }

  function updateWork(index: number, patch: Partial<EditableWork>) {
    setWork((current) => current.map((w, i) => (i === index ? { ...w, ...patch } : w)));
  }

  function addWork() {
    setWork((current) => [
      ...current,
      {
        key: nextKey++,
        title: '',
        employer: '',
        startMonth: '',
        endMonth: '',
        isCurrent: false,
        skillsUsed: [],
      },
    ]);
  }

  async function save() {
    const payload = {
      skills: skills
        .filter((s) => s.checked && s.name.trim())
        .map((s) => ({
          name: s.name.trim(),
          level: s.level,
          years: s.years === null ? null : Number(s.years),
        })),
      totalExperienceYears: total.trim() === '' ? null : Number(total),
      educationLevel: education || null,
      jobTitles: titles
        .split('\n')
        .map((t) => t.trim())
        .filter(Boolean),
      workHistory: work
        .filter((w) => w.title.trim())
        .map((w) => ({
          title: w.title.trim(),
          employer: w.employer?.trim() || null,
          startMonth: w.startMonth || null,
          endMonth: w.isCurrent ? null : w.endMonth || null,
          isCurrent: w.isCurrent,
          skillsUsed: w.skillsUsed.map((s) => s.trim()).filter(Boolean),
        })),
    };
    try {
      await onSave(payload);
      toast('Saved to your profile.', 'success');
      setConfirmOpen(false);
    } catch (err) {
      toast(getApiErrorMessage(err), 'error');
    }
  }

  return (
    <div className="space-y-6">
      <section className="rounded-xl border border-border bg-surface p-5 shadow-sm">
        <h3 className="mb-1 text-base font-semibold text-text">Experience and education</h3>
        <p className="mb-4 text-xs text-text-secondary">
          Check the suggestions you agree with, then edit anything that looks wrong.
        </p>
        <div className="grid gap-4 sm:grid-cols-2">
          <div>
            <label className="mb-1 block text-xs text-text-secondary">Total experience (years)</label>
            <Input
              type="number"
              min={0}
              max={60}
              step="0.1"
              value={total}
              onChange={(e) => setTotal(e.target.value)}
            />
          </div>
          <div>
            <label className="mb-1 block text-xs text-text-secondary">Education level</label>
            <Select value={education} onChange={(e) => setEducation(e.target.value)}>
              <option value="">—</option>
              {EDUCATION_LEVELS.map((level) => (
                <option key={level} value={level}>
                  {level.replace('_', ' ')}
                </option>
              ))}
            </Select>
          </div>
          <div className="sm:col-span-2">
            <label className="mb-1 block text-xs text-text-secondary">
              Job titles (one per line)
            </label>
            <textarea
              value={titles}
              onChange={(e) => setTitles(e.target.value)}
              rows={3}
              className="w-full rounded-md border border-border bg-bg px-3 py-2 text-sm text-text placeholder:text-text-subtle focus:border-primary focus:outline-none"
              placeholder="Backend Engineer"
            />
          </div>
        </div>
      </section>

      <section className="rounded-xl border border-border bg-surface p-5 shadow-sm">
        <h3 className="mb-3 text-base font-semibold text-text">Suggested skills</h3>
        {skills.length === 0 && (
          <p className="text-sm text-text-subtle">No skills were found. Add them on your profile.</p>
        )}
        <div className="divide-y divide-border">
          {skills.map((skill, index) => (
            <div key={`${skill.name}-${index}`} className="grid items-center gap-3 py-3 sm:grid-cols-[1fr_140px_110px]">
              <Checkbox
                checked={skill.checked}
                onChange={(checked) => updateSkill(index, { checked })}
                label={skill.name}
              />
              <Select
                value={skill.level}
                onChange={(e) =>
                  updateSkill(index, { level: e.target.value as CandidateSkillLevel })
                }
                aria-label={`${skill.name} level`}
              >
                {SKILL_LEVELS.map((level) => (
                  <option key={level} value={level}>
                    {level}
                  </option>
                ))}
              </Select>
              <Input
                type="number"
                min={0}
                max={60}
                step="0.1"
                value={skill.years ?? ''}
                onChange={(e) =>
                  updateSkill(index, {
                    years: e.target.value === '' ? null : Number(e.target.value),
                  })
                }
                placeholder="Years"
                aria-label={`${skill.name} years`}
              />
            </div>
          ))}
        </div>
      </section>

      <section className="rounded-xl border border-border bg-surface p-5 shadow-sm">
        <div className="mb-3 flex items-center justify-between">
          <h3 className="text-base font-semibold text-text">Work history</h3>
          <Button variant="secondary" onClick={addWork}>
            <Icon icon={Plus} size={16} />
            Add role
          </Button>
        </div>
        {work.length === 0 && (
          <p className="text-sm text-text-subtle">
            No roles were extracted. Add them here or on your profile.
          </p>
        )}
        <div className="space-y-4">
          {work.map((item, index) => (
            <div key={item.key} className="rounded-lg border border-border p-4">
              <div className="grid gap-3 sm:grid-cols-2">
                <div>
                  <label className="mb-1 block text-xs text-text-secondary">Title</label>
                  <Input
                    value={item.title}
                    onChange={(e) => updateWork(index, { title: e.target.value })}
                    placeholder="Backend Engineer"
                  />
                </div>
                <div>
                  <label className="mb-1 block text-xs text-text-secondary">Employer</label>
                  <Input
                    value={item.employer ?? ''}
                    onChange={(e) => updateWork(index, { employer: e.target.value })}
                    placeholder="Company"
                  />
                </div>
                <div>
                  <label className="mb-1 block text-xs text-text-secondary">Start month</label>
                  <Input
                    type="month"
                    value={item.startMonth ?? ''}
                    onChange={(e) => updateWork(index, { startMonth: e.target.value })}
                  />
                </div>
                <div>
                  <label className="mb-1 block text-xs text-text-secondary">End month</label>
                  <Input
                    type="month"
                    value={item.endMonth ?? ''}
                    disabled={item.isCurrent}
                    onChange={(e) => updateWork(index, { endMonth: e.target.value })}
                  />
                </div>
                <div className="sm:col-span-2">
                  <Checkbox
                    checked={item.isCurrent}
                    onChange={(checked) =>
                      updateWork(index, { isCurrent: checked, endMonth: checked ? '' : item.endMonth })
                    }
                    label="I currently work here"
                  />
                </div>
                <div className="sm:col-span-2">
                  <label className="mb-1 block text-xs text-text-secondary">
                    Skills used (comma separated)
                  </label>
                  <Input
                    value={item.skillsUsed.join(', ')}
                    onChange={(e) =>
                      updateWork(index, {
                        skillsUsed: e.target.value.split(',').map((s) => s.trim()),
                      })
                    }
                    placeholder="Java, Spring Boot"
                  />
                </div>
              </div>
              <div className="mt-3 flex justify-end">
                <Button
                  variant="danger"
                  className="h-8 px-2 text-xs"
                  onClick={() => setWork((current) => current.filter((_, i) => i !== index))}
                >
                  <Icon icon={Trash2} size={14} />
                  Remove
                </Button>
              </div>
            </div>
          ))}
        </div>
      </section>

      <div className="flex justify-end">
        <Button onClick={() => setConfirmOpen(true)}>Save to profile</Button>
      </div>

      <Dialog open={confirmOpen} title="Save to profile" onClose={() => setConfirmOpen(false)}>
        <p className="text-sm text-text">
          This replaces your current work history with the list above. Skills and experience are
          updated too. You can still edit everything later on your profile.
        </p>
        <div className="mt-6 flex justify-end gap-3">
          <Button variant="secondary" onClick={() => setConfirmOpen(false)}>
            Cancel
          </Button>
          <Button loading={saving} onClick={save}>
            Save to profile
          </Button>
        </div>
      </Dialog>
    </div>
  );
}
