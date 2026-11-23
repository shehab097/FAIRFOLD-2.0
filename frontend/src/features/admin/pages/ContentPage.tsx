import { useEffect, useState, type ReactNode } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { Check, ExternalLink, Pencil, Plus, RefreshCw } from 'lucide-react';
import {
  useAssessments,
  useCheckResourceLinks,
  useCreateResource,
  useResources,
  useSetAssessmentActive,
  useSetResourceActive,
  useUpdateResource,
} from '../queries';
import {
  useAdminPracticeQuestions,
  useCreateAdminPracticeQuestion,
  useSetAdminPracticeQuestionActive,
  useUpdateAdminPracticeQuestion,
} from '../../practice/queries';
import type { AdminPracticeQuestion } from '../../practice/types';
import { Badge } from '../../../components/ui/Badge';
import { Banner } from '../../../components/ui/Banner';
import { Button } from '../../../components/ui/Button';
import { Dialog } from '../../../components/ui/Dialog';
import { Icon } from '../../../components/ui/Icon';
import { Input } from '../../../components/ui/Input';
import { Select } from '../../../components/ui/Select';
import { Skeleton } from '../../../components/ui/Skeleton';
import { Switch } from '../../../components/ui/Switch';
import { Table, type Column } from '../../../components/ui/Table';
import { Tabs } from '../../../components/ui/Tabs';
import { Textarea } from '../../../components/ui/Textarea';
import { useToast } from '../../../components/ui/Toast';
import { getApiErrorMessage } from '../../../lib/errors';
import { formatDate } from '../../../lib/format';
import { SkillSelect, type SelectedSkillRef } from '../components/SkillSelect';
import type {
  AssessmentListItem,
  LearningResource,
  ResourceCost,
  ResourceInput,
  ResourceLevel,
} from '../types';

const RESOURCE_CODES: Record<ResourceCost, string> = { free: 'Free', paid: 'Paid' };
const LEVEL_LABELS: Record<ResourceLevel, string> = {
  beginner: 'Beginner',
  intermediate: 'Intermediate',
  advanced: 'Advanced',
};

function Field({ label, error, children }: { label: string; error?: string; children: ReactNode }) {
  return (
    <label className="block">
      <span className="mb-1 block text-xs font-semibold uppercase tracking-[0.06em] text-text-secondary">
        {label}
      </span>
      {children}
      {error && <span className="mt-1 block text-sm text-accent">{error}</span>}
    </label>
  );
}

function ResourceDialog({
  open,
  resource,
  onClose,
}: {
  open: boolean;
  resource: LearningResource | null;
  onClose: () => void;
}) {
  const create = useCreateResource();
  const update = useUpdateResource();
  const { toast } = useToast();

  const [skill, setSkill] = useState<SelectedSkillRef | null>(null);
  const [title, setTitle] = useState('');
  const [url, setUrl] = useState('');
  const [cost, setCost] = useState<ResourceCost>('free');
  const [language, setLanguage] = useState('English');
  const [level, setLevel] = useState<ResourceLevel>('beginner');
  const [active, setActive] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    if (!open) return;
    setSkill(resource ? { id: resource.skillId, name: resource.skillName ?? 'Skill' } : null);
    setTitle(resource?.title ?? '');
    setUrl(resource?.url ?? '');
    setCost(resource?.cost ?? 'free');
    setLanguage(resource?.language ?? 'English');
    setLevel(resource?.level ?? 'beginner');
    setActive(resource?.active ?? true);
    setError(null);
    setFieldErrors({});
  }, [open, resource]);

  const busy = create.isPending || update.isPending;

  async function save() {
    setError(null);
    setFieldErrors({});
    if (!skill) {
      setFieldErrors({ skillId: 'Choose a skill.' });
      return;
    }
    if (!title.trim()) {
      setFieldErrors({ title: 'Enter a title.' });
      return;
    }
    if (!url.trim()) {
      setFieldErrors({ url: 'Enter a URL.' });
      return;
    }
    const input: ResourceInput = {
      skillId: skill.id,
      title: title.trim(),
      url: url.trim(),
      cost,
      language: language.trim() || 'English',
      level,
      active,
    };
    try {
      if (resource) {
        await update.mutateAsync({ id: resource.id, input });
        toast('Resource updated.', 'success');
      } else {
        await create.mutateAsync(input);
        toast('Resource added.', 'success');
      }
      onClose();
    } catch (err) {
      const api = err as { response?: { data?: { errors?: { field: string; message: string }[] } } };
      const fields = api.response?.data?.errors ?? [];
      setFieldErrors(Object.fromEntries(fields.map((f) => [f.field, f.message])));
      setError(getApiErrorMessage(err));
    }
  }

  return (
    <Dialog open={open} title={resource ? 'Edit resource' : 'New resource'} onClose={onClose}>
      <div className="space-y-4">
        <Field label="Skill" error={fieldErrors.skillId}>
          <SkillSelect value={skill} onChange={setSkill} />
        </Field>
        <Field label="Title" error={fieldErrors.title}>
          <Input value={title} onChange={(e) => setTitle(e.target.value)} maxLength={500} />
        </Field>
        <Field label="URL" error={fieldErrors.url}>
          <Input
            value={url}
            onChange={(e) => setUrl(e.target.value)}
            placeholder="https://…"
            type="url"
          />
        </Field>
        <div className="grid grid-cols-2 gap-3">
          <Field label="Cost">
            <Select value={cost} onChange={(e) => setCost(e.target.value as ResourceCost)}>
              <option value="free">Free</option>
              <option value="paid">Paid</option>
            </Select>
          </Field>
          <Field label="Level">
            <Select value={level} onChange={(e) => setLevel(e.target.value as ResourceLevel)}>
              {Object.entries(LEVEL_LABELS).map(([value, label]) => (
                <option key={value} value={value}>
                  {label}
                </option>
              ))}
            </Select>
          </Field>
        </div>
        <Field label="Language" error={fieldErrors.language}>
          <Input value={language} onChange={(e) => setLanguage(e.target.value)} maxLength={50} />
        </Field>
        <div className="flex items-center justify-between rounded-lg border border-border bg-bg px-3 py-2">
          <span className="text-sm text-text">Active</span>
          <Switch checked={active} onChange={setActive} label="Active" />
        </div>
        {error && <p className="text-sm text-accent">{error}</p>}
        <div className="flex justify-end gap-2">
          <Button variant="secondary" onClick={onClose} disabled={busy}>
            Cancel
          </Button>
          <Button onClick={save} loading={busy}>
            Save
          </Button>
        </div>
      </div>
    </Dialog>
  );
}

function PracticeQuestionDialog({
  open,
  question,
  onClose,
}: {
  open: boolean;
  question: AdminPracticeQuestion | null;
  onClose: () => void;
}) {
  const create = useCreateAdminPracticeQuestion();
  const update = useUpdateAdminPracticeQuestion();
  const { toast } = useToast();
  const [text, setText] = useState('');
  const [topic, setTopic] = useState('');
  const [active, setActive] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!open) return;
    setText(question?.text ?? '');
    setTopic(question?.topic ?? '');
    setActive(question?.active ?? true);
    setError(null);
  }, [open, question]);

  async function save() {
    setError(null);
    if (!text.trim() || !topic.trim()) {
      setError('Question text and topic are required.');
      return;
    }
    try {
      if (question) {
        await update.mutateAsync({ id: question.id, input: { text: text.trim(), topic: topic.trim(), active } });
        toast('Practice question updated.', 'success');
      } else {
        await create.mutateAsync({ text: text.trim(), topic: topic.trim(), active });
        toast('Practice question added.', 'success');
      }
      onClose();
    } catch (err) {
      setError(getApiErrorMessage(err));
    }
  }

  return (
    <Dialog
      open={open}
      title={question ? 'Edit practice question' : 'New practice question'}
      onClose={onClose}
    >
      <div className="space-y-4">
        <Field label="Question">
          <Textarea
            rows={3}
            value={text}
            onChange={(e) => setText(e.target.value)}
            maxLength={1000}
            placeholder="Tell us about a time you…"
          />
        </Field>
        <Field label="Topic">
          <Input
            value={topic}
            onChange={(e) => setTopic(e.target.value)}
            maxLength={100}
            placeholder="Behavioral, Leadership, SQL…"
          />
        </Field>
        <div className="flex items-center justify-between rounded-lg border border-border bg-bg px-3 py-2">
          <span className="text-sm text-text">Active</span>
          <Switch checked={active} onChange={setActive} label="Active" />
        </div>
        {error && <p className="text-sm text-accent">{error}</p>}
        <div className="flex justify-end gap-2">
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button loading={create.isPending || update.isPending} onClick={save}>
            Save
          </Button>
        </div>
      </div>
    </Dialog>
  );
}

export function ContentPage() {
  const [params, setParams] = useSearchParams();
  const rawTab = params.get('tab');
  const tab = rawTab === 'resources' ? 'resources' : rawTab === 'practice' ? 'practice' : 'assessments';
  const page = Number(params.get('page') ?? '0');
  const assessments = useAssessments(page);
  const resources = useResources(page);
  const practice = useAdminPracticeQuestions(page);
  const setAssessmentActive = useSetAssessmentActive();
  const setResourceActive = useSetResourceActive();
  const setPracticeActive = useSetAdminPracticeQuestionActive();
  const checkLinks = useCheckResourceLinks();
  const { toast } = useToast();
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editing, setEditing] = useState<LearningResource | null>(null);
  const [practiceDialogOpen, setPracticeDialogOpen] = useState(false);
  const [editingQuestion, setEditingQuestion] = useState<AdminPracticeQuestion | null>(null);
  const [busyId, setBusyId] = useState<string | null>(null);

  function changeTab(next: string) {
    const copy = new URLSearchParams(params);
    if (next === 'assessments') copy.delete('tab');
    else copy.set('tab', next);
    copy.delete('page');
    setParams(copy);
  }

  function changePage(next: number) {
    const copy = new URLSearchParams(params);
    if (next === 0) copy.delete('page');
    else copy.set('page', String(next));
    setParams(copy);
  }

  async function toggleAssessment(row: AssessmentListItem, active: boolean) {
    setBusyId(row.id);
    try {
      await setAssessmentActive.mutateAsync({ id: row.id, active });
      toast(active ? 'Assessment activated.' : 'Assessment deactivated.', 'success');
    } catch (err) {
      toast(getApiErrorMessage(err), 'error');
    } finally {
      setBusyId(null);
    }
  }

  async function toggleResource(row: LearningResource, active: boolean) {
    setBusyId(row.id);
    try {
      await setResourceActive.mutateAsync({ id: row.id, active });
      toast(active ? 'Resource activated.' : 'Resource deactivated.', 'success');
    } catch (err) {
      toast(getApiErrorMessage(err), 'error');
    } finally {
      setBusyId(null);
    }
  }

  async function togglePractice(row: AdminPracticeQuestion, active: boolean) {
    setBusyId(row.id);
    try {
      await setPracticeActive.mutateAsync({ id: row.id, active });
      toast(active ? 'Practice question activated.' : 'Practice question deactivated.', 'success');
    } catch (err) {
      toast(getApiErrorMessage(err), 'error');
    } finally {
      setBusyId(null);
    }
  }

  async function runLinkCheck() {
    try {
      const result = await checkLinks.mutateAsync();
      toast(`Checked ${result.checked} link${result.checked === 1 ? '' : 's'}; ${result.broken} broken.`, result.broken ? 'error' : 'success');
    } catch (err) {
      toast(getApiErrorMessage(err), 'error');
    }
  }

  const assessmentColumns: Column<AssessmentListItem>[] = [
    {
      header: 'Title',
      render: (row) => (
        <Link
          to={`/admin/assessments/${row.id}`}
          className="font-medium text-text hover:text-primary-text"
        >
          {row.title}
        </Link>
      ),
    },
    { header: 'Skill', render: (row) => row.skillName ?? '—' },
    { header: 'Questions', render: (row) => String(row.questionCount) },
    { header: 'Duration', render: (row) => `${row.durationMinutes} min` },
    { header: 'Pass mark', render: (row) => `${row.passMark}%` },
    { header: 'Attempts', render: (row) => String(row.attemptCount) },
    {
      header: 'Active',
      render: (row) => (
        <Switch
          checked={row.active}
          onChange={(value) => toggleAssessment(row, value)}
          label={`Active: ${row.title}`}
          disabled={busyId === row.id}
        />
      ),
    },
    {
      header: '',
      className: 'w-24',
      render: (row) => (
        <Link
          to={`/admin/assessments/${row.id}`}
          className="inline-flex items-center gap-1 text-sm font-semibold text-primary-text hover:underline"
        >
          <Icon icon={Pencil} size={14} /> Edit
        </Link>
      ),
    },
  ];

  const resourceColumns: Column<LearningResource>[] = [
    {
      header: 'Title',
      render: (row) => (
        <a
          href={row.url}
          target="_blank"
          rel="noreferrer"
          className="inline-flex max-w-[280px] items-center gap-1 font-medium text-text hover:text-primary-text"
        >
          <span className="truncate">{row.title}</span>
          <Icon icon={ExternalLink} size={13} />
        </a>
      ),
    },
    { header: 'Skill', render: (row) => row.skillName ?? '—' },
    { header: 'Cost', render: (row) => RESOURCE_CODES[row.cost] },
    { header: 'Language', render: (row) => row.language },
    { header: 'Level', render: (row) => LEVEL_LABELS[row.level] },
    {
      header: 'Status',
      render: (row) =>
        row.broken ? (
          <Badge status="broken" />
        ) : (
          <Badge tone="success" icon={Check}>
            OK
          </Badge>
        ),
    },
    {
      header: 'Last checked',
      render: (row) => (row.lastCheckedAt ? formatDate(row.lastCheckedAt) : 'Never'),
    },
    {
      header: 'Active',
      render: (row) => (
        <Switch
          checked={row.active}
          onChange={(value) => toggleResource(row, value)}
          label={`Active: ${row.title}`}
          disabled={busyId === row.id}
        />
      ),
    },
    {
      header: '',
      className: 'w-24',
      render: (row) => (
        <button
          type="button"
          onClick={() => {
            setEditing(row);
            setDialogOpen(true);
          }}
          className="inline-flex items-center gap-1 text-sm font-semibold text-primary-text hover:underline"
        >
          <Icon icon={Pencil} size={14} /> Edit
        </button>
      ),
    },
  ];

  const practiceColumns: Column<AdminPracticeQuestion>[] = [
    {
      header: 'Question',
      render: (row) => (
        <span className="block max-w-[520px] truncate font-medium text-text">{row.text}</span>
      ),
    },
    { header: 'Topic', render: (row) => row.topic },
    {
      header: 'Active',
      render: (row) => (
        <Switch
          checked={row.active}
          onChange={(value) => togglePractice(row, value)}
          label={`Active: ${row.text}`}
          disabled={busyId === row.id}
        />
      ),
    },
    {
      header: '',
      className: 'w-24',
      render: (row) => (
        <button
          type="button"
          onClick={() => {
            setEditingQuestion(row);
            setPracticeDialogOpen(true);
          }}
          className="inline-flex items-center gap-1 text-sm font-semibold text-primary-text hover:underline"
        >
          <Icon icon={Pencil} size={14} /> Edit
        </button>
      ),
    },
  ];

  const list = tab === 'assessments' ? assessments : tab === 'resources' ? resources : practice;
  const totalPages = list.data?.totalPages ?? 0;

  return (
    <div>
      <div className="mb-6 flex flex-wrap items-start justify-between gap-3">
        <div>
          <h1 className="text-[28px] font-bold leading-9">Content</h1>
          <p className="text-text-secondary">
            Manage assessments, questions and learning resources. Content is deactivated, never
            deleted.
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          {tab === 'assessments' ? (
            <Link
              to="/admin/assessments/new"
              className="inline-flex h-10 items-center justify-center gap-2 rounded-lg bg-primary px-4 text-sm font-semibold text-white transition-colors hover:bg-primary-hover"
            >
              <Icon icon={Plus} size={16} /> New assessment
            </Link>
          ) : tab === 'resources' ? (
            <>
              <Button
                variant="secondary"
                onClick={runLinkCheck}
                loading={checkLinks.isPending}
              >
                <Icon icon={RefreshCw} size={16} /> Check links now
              </Button>
              <Button
                onClick={() => {
                  setEditing(null);
                  setDialogOpen(true);
                }}
              >
                <Icon icon={Plus} size={16} /> New resource
              </Button>
            </>
          ) : (
            <Button
              onClick={() => {
                setEditingQuestion(null);
                setPracticeDialogOpen(true);
              }}
            >
              <Icon icon={Plus} size={16} /> New question
            </Button>
          )}
        </div>
      </div>

      <div className="mb-4">
        <Tabs
          tabs={[
            { key: 'assessments', label: 'Assessments' },
            { key: 'resources', label: 'Resources' },
            { key: 'practice', label: 'Practice questions' },
          ]}
          active={tab}
          onChange={changeTab}
        />
      </div>

      {list.isLoading && (
        <div className="space-y-2">
          <Skeleton className="h-10 w-full rounded-lg" />
          <Skeleton className="h-10 w-full rounded-lg" />
          <Skeleton className="h-10 w-full rounded-lg" />
          <Skeleton className="h-10 w-full rounded-lg" />
        </div>
      )}

      {list.isError && (
        <Banner tone="warning">
          Could not load content.{' '}
          <button type="button" className="underline" onClick={() => list.refetch()}>
            Retry
          </button>
        </Banner>
      )}

      {!list.isLoading && !list.isError && tab === 'assessments' && (
        <Table
          columns={assessmentColumns}
          rows={assessments.data?.items ?? []}
          rowKey={(row) => row.id}
          empty={
            <div className="rounded-xl border border-border bg-surface p-8 text-center text-sm text-text-secondary">
              No assessments yet. Create the first one to start proving skills.
            </div>
          }
        />
      )}

      {!list.isLoading && !list.isError && tab === 'resources' && (
        <Table
          columns={resourceColumns}
          rows={resources.data?.items ?? []}
          rowKey={(row) => row.id}
          empty={
            <div className="rounded-xl border border-border bg-surface p-8 text-center text-sm text-text-secondary">
              No resources yet. Add a course, article or book for a skill.
            </div>
          }
        />
      )}

      {!list.isLoading && !list.isError && tab === 'practice' && (
        <Table
          columns={practiceColumns}
          rows={practice.data?.items ?? []}
          rowKey={(row) => row.id}
          empty={
            <div className="rounded-xl border border-border bg-surface p-8 text-center text-sm text-text-secondary">
              No practice questions yet. Add the first one.
            </div>
          }
        />
      )}

      {totalPages > 1 && (
        <div className="mt-4 flex items-center justify-between text-sm text-text-secondary">
          <span>
            Page {page + 1} of {totalPages}
          </span>
          <div className="flex gap-2">
            <Button
              variant="secondary"
              disabled={page <= 0}
              onClick={() => changePage(page - 1)}
            >
              Previous
            </Button>
            <Button
              variant="secondary"
              disabled={page + 1 >= totalPages}
              onClick={() => changePage(page + 1)}
            >
              Next
            </Button>
          </div>
        </div>
      )}

      <ResourceDialog
        open={dialogOpen}
        resource={editing}
        onClose={() => setDialogOpen(false)}
      />

      <PracticeQuestionDialog
        open={practiceDialogOpen}
        question={editingQuestion}
        onClose={() => setPracticeDialogOpen(false)}
      />
    </div>
  );
}
