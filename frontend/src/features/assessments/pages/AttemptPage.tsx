import { useEffect, useRef, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Check, Clock, ExternalLink } from 'lucide-react';
import {
  useSaveAnswers,
  useStartAttempt,
  useSubmitAttempt,
} from '../queries';
import { consentAttempt, saveExplanation } from '../../../api/assessments';
import { devCompleteAttempt } from '../../../api/dev';
import { extractApiError, getApiErrorMessage } from '../../../lib/errors';
import { formatDate } from '../../../lib/format';
import { Badge } from '../../../components/ui/Badge';
import { Banner } from '../../../components/ui/Banner';
import { Button } from '../../../components/ui/Button';
import { Dialog } from '../../../components/ui/Dialog';
import { Icon } from '../../../components/ui/Icon';
import { Input } from '../../../components/ui/Input';
import { ScoreMeter } from '../../../components/ui/ScoreMeter';
import { Skeleton } from '../../../components/ui/Skeleton';
import { useToast } from '../../../components/ui/Toast';
import { QuestionNavigator } from '../components/QuestionNavigator';
import { SaveIndicator } from '../components/SaveIndicator';
import { formatRemaining, useCountdown } from '../hooks/useCountdown';
import { useProctorEvents } from '../hooks/useProctorEvents';
import type { AttemptView } from '../types';

function ExplanationCard({ attemptId, initial }: { attemptId: string; initial: string | null }) {
  const [text, setText] = useState(initial ?? '');
  const [saving, setSaving] = useState(false);
  const [saved, setSaved] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function save() {
    setSaving(true);
    setError(null);
    try {
      await saveExplanation(attemptId, text.trim());
      setSaved(true);
    } catch (err) {
      setError(getApiErrorMessage(err));
    } finally {
      setSaving(false);
    }
  }

  return (
    <section className="rounded-xl border border-border bg-surface p-5 shadow-sm">
      <h2 className="text-lg font-semibold">Add an explanation</h2>
      <p className="text-sm text-text-secondary">
        Optional. Your employer sees this next to the neutral focus signals. It does not change
        your score.
      </p>
      <textarea
        value={text}
        onChange={(e) => {
          setText(e.target.value.slice(0, 500));
          setSaved(false);
        }}
        rows={3}
        maxLength={500}
        className="mt-3 w-full rounded-lg border border-border bg-bg px-3 py-2 text-sm text-text placeholder:text-text-subtle focus:border-primary focus:outline-none"
        placeholder="For example: my connection dropped for a minute."
      />
      <div className="mt-2 flex items-center justify-between gap-3">
        <span className="text-xs text-text-secondary">{text.length}/500</span>
        <div className="flex items-center gap-3">
          {saved && <span className="text-sm text-primary-text">Saved</span>}
          <Button variant="secondary" loading={saving} onClick={save}>
            Save explanation
          </Button>
        </div>
      </div>
      {error && <p className="mt-1 text-sm text-accent">{error}</p>}
    </section>
  );
}

function ResultView({
  result,
  onBack,
}: {
  result: AttemptView;
  onBack: () => void;
}) {
  const passed = Boolean(result.passed);
  const retryFuture = Boolean(
    result.retryAvailableAt && new Date(result.retryAvailableAt) > new Date(),
  );

  return (
    <div className="space-y-6">
      <div className="rounded-2xl border border-border bg-surface p-6 text-center shadow-sm">
        <div className="text-xs font-semibold uppercase tracking-[0.06em] text-text-secondary">
          {result.status === 'auto_submitted' ? 'Time ran out' : 'Assessment result'}
        </div>
        <h1 className="mt-1 text-2xl font-bold text-text">{result.assessmentTitle}</h1>
        <div className="mt-4 flex justify-center">
          <Badge status={passed ? 'verified' : 'failed'} />
        </div>
        <div className="mt-5 text-5xl font-bold tabular-nums text-text">{result.score}%</div>
        <p className="mt-1 text-sm text-text-secondary">
          Pass mark {result.passMark}% · {result.correctCount} of {result.totalQuestions} correct
        </p>
        <div className="mx-auto mt-4 max-w-sm">
          <ScoreMeter score={result.score ?? 0} cutoff={result.passMark ?? null} />
        </div>
        {passed ? (
          <p className="mt-4 text-sm text-primary-text">
            Verified badge added to {result.skillName ?? 'this skill'}.
          </p>
        ) : (
          <p className="mt-4 text-sm text-text-secondary">
            Study the resources below, then retake when the cooldown ends.
          </p>
        )}
        {result.status === 'auto_submitted' && (
          <p className="mt-2 text-xs text-text-secondary">
            Your saved answers were submitted automatically when the timer ended.
          </p>
        )}
      </div>

      {!passed && retryFuture && (
        <Banner tone="info">
          You can retake this assessment after {formatDate(result.retryAvailableAt)}.
        </Banner>
      )}

      {!passed && (result.resources?.length ?? 0) > 0 && (
        <section className="rounded-xl border border-border bg-surface p-5 shadow-sm">
          <h2 className="text-lg font-semibold">Learning resources</h2>
          <p className="text-sm text-text-secondary">
            Curated by FairFold for {result.skillName ?? 'this skill'}.
          </p>
          <ul className="mt-3 divide-y divide-border">
            {result.resources?.map((resource) => (
              <li key={resource.id} className="flex items-center justify-between gap-3 py-3">
                <div>
                  <a
                    href={resource.url}
                    target="_blank"
                    rel="noreferrer"
                    className="inline-flex items-center gap-1.5 font-medium text-primary-text hover:underline"
                  >
                    {resource.title}
                    <Icon icon={ExternalLink} size={14} />
                  </a>
                  <div className="text-xs text-text-secondary">
                    {resource.cost === 'free' ? 'Free' : 'Paid'} · {resource.language} ·{' '}
                    {resource.level}
                  </div>
                </div>
              </li>
            ))}
          </ul>
        </section>
      )}

      <div className="flex flex-wrap justify-center gap-2">
        <Button variant="secondary" onClick={onBack}>
          Back to assessments
        </Button>
        {!passed && !retryFuture && (
          <Button onClick={() => window.location.reload()}>Retake now</Button>
        )}
      </div>

      {result.proctoringConsent === true && (
        <ExplanationCard attemptId={result.attemptId} initial={result.explanation ?? null} />
      )}
    </div>
  );
}

export function AttemptPage() {
  const { assessmentId } = useParams<{ assessmentId: string }>();
  const navigate = useNavigate();
  const start = useStartAttempt();
  const saveMutation = useSaveAnswers();
  const submitMutation = useSubmitAttempt();
  const { toast } = useToast();

  const [view, setView] = useState<AttemptView | null>(null);
  const [result, setResult] = useState<AttemptView | null>(null);
  const [answers, setAnswers] = useState<Record<string, string>>({});
  const [current, setCurrent] = useState(0);
  const [offsetMs, setOffsetMs] = useState(0);
  const [loading, setLoading] = useState(true);
  const [fatal, setFatal] = useState<string | null>(null);
  const [cooldownUntil, setCooldownUntil] = useState<string | null>(null);
  const [dirty, setDirty] = useState(false);
  const [saving, setSaving] = useState(false);
  const [saved, setSaved] = useState(false);
  const [saveError, setSaveError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [consentOpen, setConsentOpen] = useState(false);
  const [consentSaving, setConsentSaving] = useState(false);
  const [consentError, setConsentError] = useState<string | null>(null);
  const [devCorrect, setDevCorrect] = useState('2');
  const [devBusy, setDevBusy] = useState(false);

  const startedRef = useRef(false);
  const submittedRef = useRef(false);
  const fiveWarned = useRef(false);
  const oneWarned = useRef(false);
  const answersRef = useRef(answers);
  answersRef.current = answers;

  const proctor = useProctorEvents(
    view?.attemptId ?? null,
    view?.proctoringConsent === true && !result,
  );

  useEffect(() => {
    if (startedRef.current || !assessmentId) return;
    startedRef.current = true;
    start
      .mutateAsync(assessmentId)
      .then((data) => {
        setOffsetMs(new Date(data.serverTime).getTime() - Date.now());
        setView(data);
        setAnswers(data.answers ?? {});
        if (data.status !== 'in_progress') {
          setResult(data);
        } else if (data.proctoringConsent === null || data.proctoringConsent === undefined) {
          setConsentOpen(true);
        }
      })
      .catch((err) => {
        const api = extractApiError(err);
        if (api?.code === 'COOLDOWN' && api.retryAvailableAt) {
          setCooldownUntil(api.retryAvailableAt);
        }
        setFatal(getApiErrorMessage(err));
      })
      .finally(() => setLoading(false));
    // The start request must run once per mount, not on every render.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [assessmentId]);

  const remaining = useCountdown(view?.endsAt, offsetMs);
  const questions = view?.questions ?? [];

  useEffect(() => {
    if (remaining === null || result) return;
    if (!fiveWarned.current && remaining <= 300 && remaining > 60) {
      fiveWarned.current = true;
      toast('5 minutes left.', 'info');
    }
    if (!oneWarned.current && remaining <= 60) {
      oneWarned.current = true;
      toast('1 minute left.', 'error');
    }
  }, [remaining, result, toast]);

  useEffect(() => {
    if (!view || result || !dirty) return;
    const timer = window.setTimeout(async () => {
      const sent = answersRef.current;
      setSaving(true);
      setSaveError(null);
      try {
        await saveMutation.mutateAsync({ attemptId: view.attemptId, answers: sent });
        if (answersRef.current === sent) {
          setDirty(false);
        }
        setSaved(true);
      } catch (err) {
        setSaveError(getApiErrorMessage(err));
      } finally {
        setSaving(false);
      }
    }, 2000);
    return () => window.clearTimeout(timer);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [answers, dirty, view, result]);

  async function handleSubmit() {
    if (!view || result || submittedRef.current) return;
    submittedRef.current = true;
    setSubmitting(true);
    setConfirmOpen(false);
    try {
      if (dirty) {
        await saveMutation.mutateAsync({ attemptId: view.attemptId, answers: answersRef.current });
        setDirty(false);
      }
      await proctor.flush();
      const submitted = await submitMutation.mutateAsync(view.attemptId);
      setResult(submitted);
      setSaved(true);
      toast(submitted.passed ? 'Assessment passed.' : 'Attempt submitted.', submitted.passed ? 'success' : 'info');
    } catch (err) {
      submittedRef.current = false;
      toast(getApiErrorMessage(err), 'error');
    } finally {
      setSubmitting(false);
    }
  }

  useEffect(() => {
    if (remaining === 0 && view && !result && !submittedRef.current) {
      void handleSubmit();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [remaining, view, result]);

  async function devComplete(count: number) {
    if (!view) return;
    setDevBusy(true);
    try {
      const submitted = await devCompleteAttempt(view.attemptId, count);
      setResult(submitted);
      setAnswers(submitted.answers ?? answers);
      setSaved(true);
      toast('Dev shortcut submitted the attempt.', 'info');
    } catch (err) {
      toast(getApiErrorMessage(err), 'error');
    } finally {
      setDevBusy(false);
    }
  }

  async function chooseConsent(given: boolean) {    if (!view) return;
    setConsentSaving(true);
    setConsentError(null);
    try {
      await consentAttempt(view.attemptId, given);
      setView({ ...view, proctoringConsent: given });
      setConsentOpen(false);
    } catch (err) {
      setConsentError(getApiErrorMessage(err));
    } finally {
      setConsentSaving(false);
    }
  }

  if (loading) {
    return (
      <div className="space-y-3">
        <Skeleton className="h-16 w-full rounded-xl" />
        <Skeleton className="h-64 w-full rounded-xl" />
      </div>
    );
  }

  if (fatal) {
    return (
      <div className="mx-auto max-w-lg space-y-4">
        <Banner tone="warning">{fatal}</Banner>
        {cooldownUntil && (
          <p className="text-sm text-text-secondary">
            Retake available {formatDate(cooldownUntil)}.
          </p>
        )}
        <Button onClick={() => navigate('/candidate/assessments')}>
          Back to assessments
        </Button>
      </div>
    );
  }

  if (result) {
    return <ResultView result={result} onBack={() => navigate('/candidate/assessments')} />;
  }

  if (!view || questions.length === 0) {
    return <Banner tone="error">This assessment has no questions.</Banner>;
  }

  const question = questions[Math.min(current, questions.length - 1)];
  const answeredCount = questions.filter((q) => Boolean(answers[q.id])).length;
  const unanswered = questions.length - answeredCount;
  const timerLow = remaining !== null && remaining <= 60;

  function choose(optionId: string) {
    setAnswers((prev) => ({ ...prev, [question.id]: optionId }));
    setDirty(true);
    setSaved(false);
  }

  return (
    <div className="space-y-5">
      <div className="flex flex-wrap items-start justify-between gap-3 rounded-xl border border-border bg-surface p-4 shadow-sm">
        <div>
          <h1 className="text-xl font-semibold text-text">{view.assessmentTitle}</h1>
          <p className="text-sm text-text-secondary">
            {view.skillName ?? 'Skill'} · {answeredCount} of {questions.length} answered
          </p>
        </div>
        <div className="text-right">
          <div
            aria-live="polite"
            className={
              timerLow
                ? 'inline-flex items-center gap-1.5 text-2xl font-bold tabular-nums text-accent'
                : 'inline-flex items-center gap-1.5 text-2xl font-bold tabular-nums text-text'
            }
          >
            <Icon icon={Clock} size={20} />
            {formatRemaining(remaining)}
          </div>
          <div className="text-xs text-text-secondary">Time left</div>
        </div>
      </div>

      <div className="flex flex-wrap items-center justify-between gap-3">
        <QuestionNavigator
          questions={questions}
          current={current}
          answers={answers}
          onSelect={setCurrent}
        />
        <SaveIndicator saving={saving} saved={saved} error={saveError} />
      </div>

      {import.meta.env.DEV && (
        <div className="rounded-xl border border-dashed border-accent bg-accent-subtle p-3">
          <div className="mb-2 text-xs font-semibold uppercase tracking-[0.06em] text-accent">
            Dev tools — development build only
          </div>
          <div className="flex flex-wrap items-center gap-2">
            <Button
              variant="secondary"
              className="h-8 px-3 text-xs"
              loading={devBusy}
              onClick={() => devComplete(questions.length)}
            >
              All correct
            </Button>
            <Button
              variant="secondary"
              className="h-8 px-3 text-xs"
              loading={devBusy}
              onClick={() => devComplete(0)}
            >
              All wrong
            </Button>
            <Input
              type="number"
              min={0}
              max={questions.length}
              value={devCorrect}
              onChange={(e) => setDevCorrect(e.target.value)}
              className="h-8 w-20 text-xs"
              aria-label="Number of correct answers"
            />
            <Button
              variant="secondary"
              className="h-8 px-3 text-xs"
              loading={devBusy}
              onClick={() => devComplete(Math.min(questions.length, Math.max(0, Number(devCorrect) || 0)))}
            >
              Submit with {Math.min(questions.length, Math.max(0, Number(devCorrect) || 0))} correct
            </Button>
          </div>
        </div>
      )}

      <div className="rounded-xl border border-border bg-surface p-5 shadow-sm sm:p-6">
        <div className="text-xs font-semibold uppercase tracking-[0.06em] text-text-secondary">
          Question {current + 1} of {questions.length}
        </div>
        <h2 className="mt-1 text-lg font-semibold text-text">{question.text}</h2>

        <div role="radiogroup" aria-label="Answer options" className="mt-4 space-y-2">
          {question.options.map((option) => {
            const selected = answers[question.id] === option.id;
            return (
              <button
                key={option.id}
                type="button"
                role="radio"
                aria-checked={selected}
                onClick={() => choose(option.id)}
                className={
                  selected
                    ? 'flex w-full items-center gap-3 rounded-lg border border-primary bg-primary-subtle px-3 py-3 text-left text-sm text-text'
                    : 'flex w-full items-center gap-3 rounded-lg border border-border bg-bg px-3 py-3 text-left text-sm text-text hover:bg-raised'
                }
              >
                <span
                  className={
                    selected
                      ? 'flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-primary text-white'
                      : 'h-5 w-5 shrink-0 rounded-full border border-border-strong bg-surface'
                  }
                >
                  {selected && <Icon icon={Check} size={13} strokeWidth={3} />}
                </span>
                <span>{option.text}</span>
              </button>
            );
          })}
        </div>

        <div className="mt-6 flex flex-wrap items-center justify-between gap-2">
          <Button
            variant="secondary"
            disabled={current === 0}
            onClick={() => setCurrent((value) => Math.max(0, value - 1))}
          >
            Previous
          </Button>
          <div className="flex gap-2">
            {current < questions.length - 1 && (
              <Button
                variant="secondary"
                onClick={() => setCurrent((value) => Math.min(questions.length - 1, value + 1))}
              >
                Next
              </Button>
            )}
            <Button onClick={() => setConfirmOpen(true)} loading={submitting}>
              Submit
            </Button>
          </div>
        </div>
      </div>

      <Dialog open={confirmOpen} title="Submit assessment?" onClose={() => setConfirmOpen(false)}>
        <p className="text-sm text-text-secondary">
          {unanswered === 0
            ? 'All questions are answered. You cannot change your answers after submitting.'
            : `${unanswered} question${unanswered === 1 ? ' is' : 's are'} unanswered and will count as wrong. You cannot change your answers after submitting.`}
        </p>
        <div className="mt-5 flex justify-end gap-2">
          <Button variant="secondary" onClick={() => setConfirmOpen(false)} disabled={submitting}>
            Keep working
          </Button>
          <Button onClick={handleSubmit} loading={submitting}>
            Submit assessment
          </Button>
        </div>
      </Dialog>

      <Dialog open={consentOpen} title="Focus signals" onClose={() => {}}>
        <div className="space-y-4">
          <p className="text-sm text-text">
            FairFold can record when you leave the tab or the window loses focus during this
            attempt. These signals are shown to the employer as neutral notes, with your optional
            explanation. They never fail your attempt and never change your score.
          </p>
          <p className="text-sm text-text-secondary">
            You can decline; the assessment works the same. Your choice is recorded once.
          </p>
          {consentError && <p className="text-sm text-accent">{consentError}</p>}
          <div className="flex justify-end gap-2">
            <Button
              variant="secondary"
              loading={consentSaving}
              onClick={() => chooseConsent(false)}
            >
              No thanks
            </Button>
            <Button loading={consentSaving} onClick={() => chooseConsent(true)}>
              I agree
            </Button>
          </div>
        </div>
      </Dialog>
    </div>
  );
}
