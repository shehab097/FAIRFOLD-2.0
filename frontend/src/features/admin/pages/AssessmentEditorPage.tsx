import { useEffect, useState, type ReactNode } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { ArrowDown, ArrowUp, Plus, Trash2 } from 'lucide-react';
import {
  useApproveAssessment,
  useAssessment,
  useCreateAssessment,
  useGenerateAssessment,
  useUpdateAssessment,
} from '../queries';
import { Badge } from '../../../components/ui/Badge';
import { Banner } from '../../../components/ui/Banner';
import { Button } from '../../../components/ui/Button';
import { Icon } from '../../../components/ui/Icon';
import { Input } from '../../../components/ui/Input';
import { Skeleton } from '../../../components/ui/Skeleton';
import { Switch } from '../../../components/ui/Switch';
import { Textarea } from '../../../components/ui/Textarea';
import { useToast } from '../../../components/ui/Toast';
import { getApiErrorCode, getApiErrorMessage, getFieldErrors } from '../../../lib/errors';
import { SkillSelect, type SelectedSkillRef } from '../components/SkillSelect';
import type { AssessmentDetail, AssessmentInput } from '../types';

interface EditQuestion {
  text: string;
  options: string[];
  correctIndex: number;
}

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

function letter(index: number): string {
  return String.fromCharCode(97 + index);
}

function emptyQuestion(): EditQuestion {
  return { text: '', options: ['', ''], correctIndex: 0 };
}

function toEditQuestions(detail: AssessmentDetail): EditQuestion[] {
  return detail.questions.map((question) => ({
    text: question.text,
    options: question.options.map((option) => option.text),
    correctIndex: Math.max(
      0,
      question.options.findIndex((option) => option.id === question.correctOptionId),
    ),
  }));
}

export function AssessmentEditorPage() {
  const { assessmentId } = useParams<{ assessmentId: string }>();
  const isEdit = Boolean(assessmentId);
  const navigate = useNavigate();
  const { data: detail, isLoading, isError, refetch } = useAssessment(assessmentId);
  const create = useCreateAssessment();
  const update = useUpdateAssessment(assessmentId ?? '');
  const generate = useGenerateAssessment();
  const approve = useApproveAssessment();
  const { toast } = useToast();

  const [title, setTitle] = useState('');
  const [skill, setSkill] = useState<SelectedSkillRef | null>(null);
  const [durationMinutes, setDurationMinutes] = useState('30');
  const [passMark, setPassMark] = useState('70');
  const [questionsPerAttempt, setQuestionsPerAttempt] = useState('10');
  const [active, setActive] = useState(true);
  const [questions, setQuestions] = useState<EditQuestion[]>([emptyQuestion()]);
  const [error, setError] = useState<string | null>(null);
  const [lockedError, setLockedError] = useState(false);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  const locked = Boolean(detail && detail.attemptCount > 0);
  const busy = create.isPending || update.isPending;

  useEffect(() => {
    if (!detail) return;
    setTitle(detail.title);
    setSkill({ id: detail.skillId, name: detail.skillName ?? 'Skill' });
    setDurationMinutes(String(detail.durationMinutes));
    setPassMark(String(detail.passMark));
    setQuestionsPerAttempt(String(detail.questionsPerAttempt));
    setActive(detail.active);
    setQuestions(toEditQuestions(detail));
  }, [detail]);

  function patchQuestion(index: number, patch: Partial<EditQuestion>) {
    setQuestions((current) =>
      current.map((question, i) => (i === index ? { ...question, ...patch } : question)),
    );
  }

  function addQuestion() {
    setQuestions((current) => [...current, emptyQuestion()]);
  }

  function removeQuestion(index: number) {
    setQuestions((current) => current.filter((_, i) => i !== index));
  }

  function moveQuestion(index: number, delta: number) {
    setQuestions((current) => {
      const target = index + delta;
      if (target < 0 || target >= current.length) return current;
      const copy = [...current];
      [copy[index], copy[target]] = [copy[target], copy[index]];
      return copy;
    });
  }

  function addOption(questionIndex: number) {
    setQuestions((current) =>
      current.map((question, i) =>
        i === questionIndex && question.options.length < 6
          ? { ...question, options: [...question.options, ''] }
          : question,
      ),
    );
  }

  function removeOption(questionIndex: number, optionIndex: number) {
    setQuestions((current) =>
      current.map((question, i) => {
        if (i !== questionIndex || question.options.length <= 2) return question;
        const options = question.options.filter((_, o) => o !== optionIndex);
        let correctIndex = question.correctIndex;
        if (optionIndex === correctIndex) correctIndex = 0;
        else if (optionIndex < correctIndex) correctIndex -= 1;
        return { ...question, options, correctIndex };
      }),
    );
  }

  async function save() {
    setError(null);
    setLockedError(false);
    setFieldErrors({});
    const errors: Record<string, string> = {};
    if (!title.trim()) errors.title = 'Enter a title.';
    if (!skill) errors.skillId = 'Choose a skill.';
    const duration = Number(durationMinutes);
    if (!Number.isInteger(duration) || duration < 5 || duration > 180) {
      errors.durationMinutes = 'Duration must be between 5 and 180 minutes.';
    }
    const pass = Number(passMark);
    if (!Number.isInteger(pass) || pass < 1 || pass > 100) {
      errors.passMark = 'Pass mark must be between 1 and 100.';
    }
    const perAttempt = Number(questionsPerAttempt);
    if (!Number.isInteger(perAttempt) || perAttempt < 1 || perAttempt > 50) {
      errors.questionsPerAttempt = 'Questions per attempt must be between 1 and 50.';
    }
    questions.forEach((question, qi) => {
      if (!question.text.trim()) errors[`questions[${qi}].text`] = 'Enter the question text.';
      question.options.forEach((option, oi) => {
        if (!option.trim()) errors[`questions[${qi}].options[${oi}].text`] = 'Enter option text.';
      });
    });
    if (Object.keys(errors).length > 0) {
      setFieldErrors(errors);
      return;
    }

    const input: AssessmentInput = {
      title: title.trim(),
      skillId: (skill as SelectedSkillRef).id,
      durationMinutes: duration,
      passMark: pass,
      active,
      questionsPerAttempt: perAttempt,
      questions: questions.map((question) => ({
        text: question.text.trim(),
        options: question.options.map((option, index) => ({
          id: letter(index),
          text: option.trim(),
        })),
        correctOptionId: letter(question.correctIndex),
      })),
    };

    try {
      if (isEdit && assessmentId) {
        await update.mutateAsync(input);
        toast('Assessment saved.', 'success');
        setLockedError(false);
      } else {
        const created = await create.mutateAsync(input);
        toast('Assessment created.', 'success');
        navigate(`/admin/assessments/${created.id}`);
      }
    } catch (err) {
      const code = getApiErrorCode(err);
      if (code === 'ASSESSMENT_HAS_ATTEMPTS') {
        setLockedError(true);
      }
      setFieldErrors(getFieldErrors(err));
      setError(getApiErrorMessage(err));
    }
  }

  async function generatePool() {
    if (!assessmentId) return;
    try {
      await generate.mutateAsync(assessmentId);
      toast('30 questions generated. Review and edit them, then approve.', 'success');
    } catch (err) {
      toast(getApiErrorMessage(err), 'error');
    }
  }

  async function approvePool() {
    if (!assessmentId) return;
    try {
      await approve.mutateAsync(assessmentId);
      toast('Assessment approved and activated.', 'success');
    } catch (err) {
      toast(getApiErrorMessage(err), 'error');
    }
  }

  if (isEdit && isLoading) {
    return (
      <div className="space-y-3">
        <Skeleton className="h-10 w-1/3 rounded-lg" />
        <Skeleton className="h-40 w-full rounded-xl" />
        <Skeleton className="h-64 w-full rounded-xl" />
      </div>
    );
  }

  if (isEdit && isError) {
    return (
      <Banner tone="warning">
        Could not load this assessment.{' '}
        <button type="button" className="underline" onClick={() => refetch()}>
          Retry
        </button>
      </Banner>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <Link to="/admin/content" className="text-sm text-text-secondary hover:text-text">
            ← Back to content
          </Link>
          <h1 className="mt-1 text-[28px] font-bold leading-9">
            {isEdit ? 'Edit assessment' : 'New assessment'}
          </h1>
          <p className="text-text-secondary">
            Multiple-choice only. Candidates see the questions in this order.
          </p>
          {detail && (
            <div className="mt-2 flex flex-wrap items-center gap-2 text-sm text-text-secondary">
              <Badge
                tone={
                  detail.reviewStatus === 'approved'
                    ? 'success'
                    : detail.reviewStatus === 'pending_review'
                      ? 'progress'
                      : 'warning'
                }
              >
                {detail.reviewStatus.replace('_', ' ')}
              </Badge>
              <span>
                Pool {detail.poolSize} · {detail.questionsPerAttempt} per attempt
              </span>
              <span>· Demand {detail.candidateDemandCount}</span>
            </div>
          )}
        </div>
        <div className="flex flex-wrap gap-2">
          {detail?.reviewStatus === 'pending_generation' && (
            <Button variant="secondary" loading={generate.isPending} onClick={generatePool}>
              Generate 30 questions
            </Button>
          )}
          {detail?.reviewStatus === 'pending_review' && (
            <Button variant="secondary" loading={approve.isPending} onClick={approvePool}>
              Approve &amp; Activate
            </Button>
          )}
          <Button onClick={save} loading={busy}>
            Save
          </Button>
        </div>
      </div>

      {detail?.reviewStatus === 'pending_review' && (
        <Banner tone="info">
          The generated pool is ready for review. Check all questions, set how many each candidate
          sees, then approve and activate.
        </Banner>
      )}

      {locked && (
        <Banner tone="warning">
          Candidates have attempted this assessment, so its skill and questions are locked. You can
          still change the title, duration, pass mark and active state. To change the content,
          deactivate this assessment and create a new one.
        </Banner>
      )}

      {lockedError && (
        <Banner tone="error">
          The questions differ from the stored version, but attempts already exist. Deactivate this
          assessment and create a new one instead.
        </Banner>
      )}

      {error && !lockedError && <Banner tone="error">{error}</Banner>}

      <section className="rounded-xl border border-border bg-surface p-5 shadow-sm sm:p-6">
        <h2 className="mb-4 text-xl font-semibold">Details</h2>
        <div className="grid gap-4 sm:grid-cols-2">
          <div className="sm:col-span-2">
            <Field label="Title" error={fieldErrors.title}>
              <Input
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                maxLength={255}
                placeholder="JavaScript Fundamentals"
              />
            </Field>
          </div>
          <div className="sm:col-span-2">
            <Field label="Skill" error={fieldErrors.skillId}>
              <SkillSelect value={skill} onChange={setSkill} disabled={locked} />
            </Field>
          </div>
          <Field label="Duration (minutes)" error={fieldErrors.durationMinutes}>
            <Input
              type="number"
              min={5}
              max={180}
              value={durationMinutes}
              onChange={(e) => setDurationMinutes(e.target.value)}
            />
          </Field>
          <Field label="Pass mark (%)" error={fieldErrors.passMark}>
            <Input
              type="number"
              min={1}
              max={100}
              value={passMark}
              onChange={(e) => setPassMark(e.target.value)}
            />
          </Field>
          <Field label="Questions per attempt" error={fieldErrors.questionsPerAttempt}>
            <Input
              type="number"
              min={1}
              max={50}
              value={questionsPerAttempt}
              onChange={(e) => setQuestionsPerAttempt(e.target.value)}
            />
          </Field>
          <div className="flex items-center justify-between rounded-lg border border-border bg-bg px-3 py-2 sm:col-span-2">
            <div>
              <div className="text-sm font-medium text-text">Active</div>
              <div className="text-xs text-text-secondary">
                Inactive assessments are hidden from candidates and job requirements.
              </div>
            </div>
            <Switch checked={active} onChange={setActive} label="Active" />
          </div>
        </div>
      </section>

      <section className="space-y-4">
        <div className="flex items-center justify-between">
          <h2 className="text-xl font-semibold">Questions</h2>
          {!locked && (
            <Button variant="secondary" onClick={addQuestion}>
              <Icon icon={Plus} size={16} /> Add question
            </Button>
          )}
        </div>

        {questions.map((question, qi) => (
          <div key={qi} className="rounded-xl border border-border bg-surface p-5 shadow-sm">
            <div className="mb-3 flex items-center justify-between gap-2">
              <h3 className="text-base font-semibold">Question {qi + 1}</h3>
              {!locked && (
                <div className="flex items-center gap-1">
                  <Button
                    variant="ghost"
                    className="h-8 px-2 text-xs"
                    disabled={qi === 0}
                    onClick={() => moveQuestion(qi, -1)}
                    aria-label="Move question up"
                  >
                    <Icon icon={ArrowUp} size={16} />
                  </Button>
                  <Button
                    variant="ghost"
                    className="h-8 px-2 text-xs"
                    disabled={qi === questions.length - 1}
                    onClick={() => moveQuestion(qi, 1)}
                    aria-label="Move question down"
                  >
                    <Icon icon={ArrowDown} size={16} />
                  </Button>
                  <Button
                    variant="danger"
                    className="h-8 px-2 text-xs"
                    disabled={questions.length <= 1}
                    onClick={() => removeQuestion(qi)}
                    aria-label="Remove question"
                  >
                    <Icon icon={Trash2} size={16} />
                  </Button>
                </div>
              )}
            </div>

            <Field label="Question text" error={fieldErrors[`questions[${qi}].text`]}>
              <Textarea
                rows={2}
                value={question.text}
                onChange={(e) => patchQuestion(qi, { text: e.target.value })}
                disabled={locked}
                maxLength={2000}
              />
            </Field>

            <div className="mt-4 space-y-2">
              <div className="text-xs font-semibold uppercase tracking-[0.06em] text-text-secondary">
                Options (2 to 6, choose the correct one)
              </div>
              {question.options.map((option, oi) => (
                <div key={oi} className="flex items-center gap-2">
                  <input
                    type="radio"
                    name={`correct-${qi}`}
                    checked={question.correctIndex === oi}
                    onChange={() => patchQuestion(qi, { correctIndex: oi })}
                    disabled={locked}
                    aria-label={`Option ${letter(oi)} is correct`}
                    className="h-4 w-4 shrink-0 accent-primary"
                  />
                  <span className="w-5 text-sm font-semibold uppercase text-text-secondary">
                    {letter(oi)}
                  </span>
                  <Input
                    value={option}
                    onChange={(e) =>
                      patchQuestion(qi, {
                        options: question.options.map((value, index) =>
                          index === oi ? e.target.value : value,
                        ),
                      })
                    }
                    disabled={locked}
                    maxLength={1000}
                    error={Boolean(fieldErrors[`questions[${qi}].options[${oi}].text`])}
                  />
                  {!locked && (
                    <Button
                      variant="ghost"
                      className="h-8 px-2"
                      disabled={question.options.length <= 2}
                      onClick={() => removeOption(qi, oi)}
                      aria-label={`Remove option ${letter(oi)}`}
                    >
                      <Icon icon={Trash2} size={16} />
                    </Button>
                  )}
                </div>
              ))}
              {fieldErrors[`questions[${qi}].options[0].text`] && (
                <p className="text-sm text-accent">
                  {fieldErrors[`questions[${qi}].options[0].text`]}
                </p>
              )}
              {!locked && question.options.length < 6 && (
                <Button variant="ghost" className="h-8 px-2 text-xs" onClick={() => addOption(qi)}>
                  <Icon icon={Plus} size={14} /> Add option
                </Button>
              )}
            </div>
          </div>
        ))}
      </section>

      <div className="flex justify-end gap-2">
        <Link
          to="/admin/content"
          className="inline-flex h-10 items-center justify-center rounded-lg border border-border-strong px-4 text-sm font-semibold text-text hover:bg-raised"
        >
          Cancel
        </Link>
        <Button onClick={save} loading={busy}>
          Save
        </Button>
      </div>
    </div>
  );
}
