import { useEffect, useState } from 'react';
import { Briefcase, Plus } from 'lucide-react';
import type { WorkHistory } from '../types';
import {
  useCreateWorkHistory,
  useDeleteWorkHistory,
  useUpdateWorkHistory,
  useWorkHistory,
} from '../queries';
import { Button } from '../../../components/ui/Button';
import { Checkbox } from '../../../components/ui/Checkbox';
import { Dialog } from '../../../components/ui/Dialog';
import { Icon } from '../../../components/ui/Icon';
import { Input } from '../../../components/ui/Input';
import { Table, type Column } from '../../../components/ui/Table';
import { useToast } from '../../../components/ui/Toast';
import { getApiErrorMessage } from '../../../lib/errors';

function formatMonth(value: string | null): string {
  if (!value) return '';
  const [year, month] = value.split('-');
  const date = new Date(Number(year), Number(month) - 1, 1);
  return date.toLocaleDateString('en-GB', { month: 'short', year: 'numeric' });
}

function period(item: WorkHistory): string {
  const start = formatMonth(item.startMonth);
  const end = item.isCurrent ? 'Present' : formatMonth(item.endMonth);
  if (!start && !end) return '—';
  return `${start || '—'} – ${end || '—'}`;
}

interface FormState {
  title: string;
  employer: string;
  startMonth: string;
  endMonth: string;
  isCurrent: boolean;
  skills: string;
}

const EMPTY: FormState = {
  title: '',
  employer: '',
  startMonth: '',
  endMonth: '',
  isCurrent: false,
  skills: '',
};

export function WorkHistorySection() {
  const { data: items, isLoading } = useWorkHistory();
  const create = useCreateWorkHistory();
  const update = useUpdateWorkHistory();
  const remove = useDeleteWorkHistory();
  const { toast } = useToast();
  const [editor, setEditor] = useState<{ open: boolean; item: WorkHistory | null }>({
    open: false,
    item: null,
  });
  const [form, setForm] = useState<FormState>(EMPTY);
  const [toDelete, setToDelete] = useState<WorkHistory | null>(null);

  useEffect(() => {
    if (!editor.open) return;
    setForm(
      editor.item
        ? {
            title: editor.item.title,
            employer: editor.item.employer ?? '',
            startMonth: editor.item.startMonth ?? '',
            endMonth: editor.item.endMonth ?? '',
            isCurrent: editor.item.isCurrent,
            skills: editor.item.skillsUsed.join(', '),
          }
        : EMPTY,
    );
  }, [editor]);

  async function save() {
    if (!form.title.trim()) {
      toast('Give this role a title.', 'error');
      return;
    }
    const input = {
      title: form.title.trim(),
      employer: form.employer.trim() || null,
      startMonth: form.startMonth || null,
      endMonth: form.isCurrent ? null : form.endMonth || null,
      isCurrent: form.isCurrent,
      skillsUsed: form.skills
        .split(',')
        .map((s) => s.trim())
        .filter(Boolean),
    };
    try {
      if (editor.item) {
        await update.mutateAsync({ id: editor.item.id, input });
      } else {
        await create.mutateAsync(input);
      }
      toast('Work history saved.', 'success');
      setEditor({ open: false, item: null });
    } catch (err) {
      toast(getApiErrorMessage(err), 'error');
    }
  }

  async function confirmDelete() {
    if (!toDelete) return;
    try {
      await remove.mutateAsync(toDelete.id);
      toast('Work history entry removed.', 'success');
      setToDelete(null);
    } catch (err) {
      toast(getApiErrorMessage(err), 'error');
    }
  }

  const columns: Column<WorkHistory>[] = [
    {
      header: 'Role',
      render: (item) => (
        <div>
          <div className="font-medium text-text">{item.title}</div>
          {item.employer && <div className="text-xs text-text-secondary">{item.employer}</div>}
        </div>
      ),
    },
    { header: 'Period', render: (item) => period(item) },
    {
      header: 'Skills used',
      render: (item) =>
        item.skillsUsed.length ? (
          <div className="flex flex-wrap gap-1">
            {item.skillsUsed.map((skill) => (
              <span
                key={skill}
                className="rounded-full border border-border bg-raised px-2 py-0.5 text-xs text-text-secondary"
              >
                {skill}
              </span>
            ))}
          </div>
        ) : (
          '—'
        ),
    },
    {
      header: '',
      render: (item) => (
        <div className="flex justify-end gap-1">
          <Button
            variant="ghost"
            className="h-8 px-2 text-xs"
            onClick={() => setEditor({ open: true, item })}
          >
            Edit
          </Button>
          <Button variant="danger" className="h-8 px-2 text-xs" onClick={() => setToDelete(item)}>
            Remove
          </Button>
        </div>
      ),
    },
  ];

  return (
    <section className="space-y-4">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <Icon icon={Briefcase} size={18} className="text-primary-text" />
          <h2 className="text-lg font-semibold">Work history ({items?.length ?? 0})</h2>
        </div>
        <Button onClick={() => setEditor({ open: true, item: null })}>
          <Icon icon={Plus} size={16} />
          Add role
        </Button>
      </div>
      {isLoading ? (
        <div className="h-24 animate-pulse rounded-xl border border-border bg-raised" />
      ) : items && items.length === 0 ? (
        <div className="rounded-xl border border-border bg-surface p-8 text-center text-sm text-text-subtle">
          No roles yet. Add your first role.
        </div>
      ) : (
        <Table columns={columns} rows={items ?? []} rowKey={(item) => item.id} />
      )}

      <Dialog
        open={editor.open}
        title={editor.item ? 'Edit role' : 'Add role'}
        onClose={() => setEditor({ open: false, item: null })}
      >
        <div className="space-y-4">
          <div>
            <label className="mb-1 block text-xs text-text-secondary">Title</label>
            <Input
              value={form.title}
              onChange={(e) => setForm({ ...form, title: e.target.value })}
              placeholder="Backend Engineer"
            />
          </div>
          <div>
            <label className="mb-1 block text-xs text-text-secondary">Employer</label>
            <Input
              value={form.employer}
              onChange={(e) => setForm({ ...form, employer: e.target.value })}
              placeholder="Company"
            />
          </div>
          <div className="grid gap-3 sm:grid-cols-2">
            <div>
              <label className="mb-1 block text-xs text-text-secondary">Start month</label>
              <Input
                type="month"
                value={form.startMonth}
                onChange={(e) => setForm({ ...form, startMonth: e.target.value })}
              />
            </div>
            <div>
              <label className="mb-1 block text-xs text-text-secondary">End month</label>
              <Input
                type="month"
                value={form.endMonth}
                disabled={form.isCurrent}
                onChange={(e) => setForm({ ...form, endMonth: e.target.value })}
              />
            </div>
          </div>
          <Checkbox
            checked={form.isCurrent}
            onChange={(checked) =>
              setForm({ ...form, isCurrent: checked, endMonth: checked ? '' : form.endMonth })
            }
            label="I currently work here"
          />
          <div>
            <label className="mb-1 block text-xs text-text-secondary">Skills used (comma separated)</label>
            <Input
              value={form.skills}
              onChange={(e) => setForm({ ...form, skills: e.target.value })}
              placeholder="Java, Spring Boot"
            />
          </div>
          <div className="flex justify-end gap-3">
            <Button variant="secondary" onClick={() => setEditor({ open: false, item: null })}>
              Cancel
            </Button>
            <Button loading={create.isPending || update.isPending} onClick={save}>
              Save
            </Button>
          </div>
        </div>
      </Dialog>

      <Dialog open={Boolean(toDelete)} title="Remove role" onClose={() => setToDelete(null)}>
        <p className="text-text">Remove “{toDelete?.title}” from your work history?</p>
        <div className="mt-6 flex justify-end gap-3">
          <Button variant="secondary" onClick={() => setToDelete(null)}>
            Cancel
          </Button>
          <Button variant="danger" loading={remove.isPending} onClick={confirmDelete}>
            Remove
          </Button>
        </div>
      </Dialog>
    </section>
  );
}
