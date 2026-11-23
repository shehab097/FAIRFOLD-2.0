import { useMemo, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { Star } from 'lucide-react';
import {
  useCreatePracticeSession,
  usePracticeQuestions,
  usePracticeSessions,
  useRatePracticeSession,
} from '../queries';
import type { PracticeSession, StarComponent, StarFeedback } from '../types';
import { getPracticeSession } from '../../../api/practice';
import { Badge } from '../../../components/ui/Badge';
import { Banner } from '../../../components/ui/Banner';
import { Button } from '../../../components/ui/Button';
import { Card } from '../../../components/ui/Card';
import { Icon } from '../../../components/ui/Icon';
import { Select } from '../../../components/ui/Select';
import { Skeleton } from '../../../components/ui/Skeleton';
import { Table, type Column } from '../../../components/ui/Table';
import { useToast } from '../../../components/ui/Toast';
import { getApiErrorMessage } from '../../../lib/errors';
import { formatDate } from '../../../lib/format';
import type { PracticeSessionListItem } from '../types';

const COMPONENTS: { key: keyof Pick<StarFeedback, 'situation' | 'task' | 'action' | 'result'>; label: string }[] = [
  { key: 'situation', label: 'Situation' },
  { key: 'task', label: 'Task' },
  { key: 'action', label: 'Action' },
  { key: 'result', label: 'Result' },
];

function Rating({ value }: { value: number }) {
  return (
    <span className="inline-flex items-center gap-0.5" aria-label={`${value} out of 5`}>
      {[1, 2, 3, 4, 5].map((star) => (
        <Icon
          key={star}
          icon={Star}
          size={14}
          className={star <= value ? 'text-accent' : 'text-text-subtle'}
        />
      ))}
    </span>
  );
}

function ComponentBlock({ label, component }: { label: string; component: StarComponent }) {
  return (
    <div className="rounded-lg border border-border bg-bg p-3">
      <div className="flex items-center justify-between gap-2">
        <span className="text-sm font-semibold text-text">{label}</span>
        <Rating value={component.rating} />
      </div>
      <ul className="mt-2 space-y-2">
        {component.points.map((point, index) => (
          <li key={index} className="text-sm text-text">
            {point.comment && <div>{point.comment}</div>}
            {point.quote && (
              <div className="mt-1 rounded-md bg-highlight px-2 py-1 text-xs text-text">
                “{point.quote}”
              </div>
            )}
          </li>
        ))}
        {component.points.length === 0 && (
          <li className="text-xs text-text-subtle">No quoted evidence for this part.</li>
        )}
      </ul>
    </div>
  );
}

export function PracticePage() {
  const [params, setParams] = useSearchParams();
  const page = Number(params.get('page') ?? '0');
  const [topic, setTopic] = useState('');
  const { data: questions, isLoading: loadingQuestions, isError: questionsError } =
    usePracticeQuestions(topic || undefined);
  const history = usePracticeSessions(page);
  const create = useCreatePracticeSession();
  const rate = useRatePracticeSession();
  const { toast } = useToast();

  const [questionId, setQuestionId] = useState('');
  const [answer, setAnswer] = useState('');
  const [result, setResult] = useState<PracticeSession | null>(null);
  const [error, setError] = useState<string | null>(null);

  const topics = useMemo(
    () => Array.from(new Set((questions ?? []).map((q) => q.topic))).sort(),
    [questions],
  );
  const selected = questions?.find((q) => q.id === questionId) ?? null;

  async function submit() {
    setError(null);
    if (!questionId) {
      setError('Choose a question first.');
      return;
    }
    if (answer.trim().length < 20) {
      setError('Write at least 20 characters.');
      return;
    }
    try {
      const session = await create.mutateAsync({ questionId, answer: answer.trim() });
      setResult(session);
      toast(
        session.feedbackAvailable
          ? 'Feedback ready.'
          : 'Answer saved. Feedback is unavailable right now.',
        session.feedbackAvailable ? 'success' : 'info',
      );
    } catch (err) {
      setError(getApiErrorMessage(err));
    }
  }

  async function openSession(id: string) {
    try {
      setResult(await getPracticeSession(id));
    } catch (err) {
      toast(getApiErrorMessage(err), 'error');
    }
  }

  async function setRating(value: number) {
    if (!result) return;
    try {
      setResult(await rate.mutateAsync({ id: result.id, rating: value }));
      toast('Rating saved.', 'success');
    } catch (err) {
      toast(getApiErrorMessage(err), 'error');
    }
  }

  function setPage(next: number) {
    const copy = new URLSearchParams(params);
    if (next === 0) copy.delete('page');
    else copy.set('page', String(next));
    setParams(copy);
  }

  const columns: Column<PracticeSessionListItem>[] = [
    { header: 'Question', render: (row) => row.questionText ?? 'Question' },
    { header: 'Topic', render: (row) => row.topic ?? '—' },
    {
      header: 'Feedback',
      render: (row) =>
        row.feedbackAvailable ? (
          <Badge tone="success">Available</Badge>
        ) : (
          <Badge tone="warning">Unavailable</Badge>
        ),
    },
    { header: 'Rating', render: (row) => (row.rating ? <Rating value={row.rating} /> : '—') },
    { header: 'Date', render: (row) => formatDate(row.createdAt) },
    {
      header: '',
      render: (row) => (
        <Button variant="ghost" className="h-8 px-2 text-xs" onClick={() => openSession(row.id)}>
          Open
        </Button>
      ),
    },
  ];

  const totalPages = history.data?.totalPages ?? 0;

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-[28px] font-bold leading-9">Interview practice</h1>
        <p className="text-text-secondary">
          Practice is private and never affects your applications. Answer in writing and get
          STAR-structured feedback.
        </p>
      </div>

      <Banner tone="info">
        Your answer is anonymised before it is sent to the AI provider. Practice answers are stored
        only for you.
      </Banner>

      {!result && (
        <Card>
          <div className="grid gap-4 sm:grid-cols-2">
            <label className="block">
              <span className="mb-1 block text-xs text-text-secondary">Topic</span>
              <Select
                value={topic}
                onChange={(e) => {
                  setTopic(e.target.value);
                  setQuestionId('');
                }}
              >
                <option value="">All topics</option>
                {topics.map((item) => (
                  <option key={item} value={item}>
                    {item}
                  </option>
                ))}
              </Select>
            </label>
            <label className="block">
              <span className="mb-1 block text-xs text-text-secondary">Question</span>
              <Select value={questionId} onChange={(e) => setQuestionId(e.target.value)}>
                <option value="">Choose a question…</option>
                {(questions ?? []).map((question) => (
                  <option key={question.id} value={question.id}>
                    {question.text}
                  </option>
                ))}
              </Select>
            </label>
          </div>

          {loadingQuestions && <Skeleton className="mt-4 h-24 w-full rounded-lg" />}
          {questionsError && <Banner tone="error">Could not load practice questions.</Banner>}
          {!loadingQuestions && !questionsError && (questions?.length ?? 0) === 0 && (
            <p className="mt-4 text-sm text-text-subtle">
              No practice questions yet. An admin manages the question bank.
            </p>
          )}

          <div className="mt-4">
            <label className="block">
              <span className="mb-1 block text-xs text-text-secondary">Your answer</span>
              <textarea
                value={answer}
                onChange={(e) => setAnswer(e.target.value.slice(0, 3000))}
                rows={8}
                maxLength={3000}
                className="w-full rounded-lg border border-border bg-bg px-3 py-2 text-sm text-text placeholder:text-text-subtle focus:border-primary focus:outline-none"
                placeholder="Use Situation, Task, Action and Result. Include what you actually did and the outcome."
              />
            </label>
            <div className="mt-2 flex items-center justify-between gap-3">
              <span className="text-xs text-text-secondary">{answer.length}/3000</span>
              <Button loading={create.isPending} onClick={submit} disabled={!selected}>
                Get STAR feedback
              </Button>
            </div>
            {error && <p className="mt-2 text-sm text-accent">{error}</p>}
          </div>
        </Card>
      )}

      {result && (
        <div className="space-y-4">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <h2 className="text-xl font-semibold">{result.questionText}</h2>
              <p className="text-sm text-text-secondary">
                {result.topic} · rubric {result.rubricVersion}
              </p>
            </div>
            <Button variant="secondary" onClick={() => setResult(null)}>
              Practice another
            </Button>
          </div>

          <Card>
            <h3 className="mb-2 text-sm font-semibold text-text">Your answer</h3>
            <p className="whitespace-pre-wrap text-sm text-text-secondary">{result.answer}</p>
          </Card>

          {!result.feedbackAvailable || !result.feedback ? (
            <>
              <Banner tone="warning">
                Feedback is unavailable right now. Your answer was saved and you can try another
                question later.
              </Banner>
              <Card>
                <h3 className="mb-2 text-sm font-semibold">Was this helpful?</h3>
                <RatingButtons value={result.rating} onRate={setRating} pending={rate.isPending} />
              </Card>
            </>
          ) : (
            <>
              <div className="grid gap-3 md:grid-cols-2">
                {COMPONENTS.map((item) => (
                  <ComponentBlock
                    key={item.key}
                    label={item.label}
                    component={result.feedback![item.key]}
                  />
                ))}
              </div>
              <Card>
                <h3 className="text-sm font-semibold text-text">Summary</h3>
                <p className="mt-1 text-sm text-text">{result.feedback.summary}</p>
              </Card>
              <Card>
                <h3 className="mb-2 text-sm font-semibold">Was this helpful?</h3>
                <RatingButtons value={result.rating} onRate={setRating} pending={rate.isPending} />
              </Card>
            </>
          )}

          <Link to="/candidate/profile" className="text-sm text-primary-text hover:underline">
            Back to profile
          </Link>
        </div>
      )}

      <section>
        <h2 className="mb-3 text-xl font-semibold">Practice history</h2>
        {history.isLoading && <Skeleton className="h-40 w-full rounded-xl" />}
        {history.isError && <Banner tone="error">Could not load practice history.</Banner>}
        {!history.isLoading && !history.isError && (
          <Table
            columns={columns}
            rows={history.data?.items ?? []}
            rowKey={(row) => row.id}
            empty={
              <div className="rounded-xl border border-border bg-surface p-8 text-center text-sm text-text-secondary">
                No practice sessions yet.
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
              <Button variant="secondary" disabled={page <= 0} onClick={() => setPage(page - 1)}>
                Previous
              </Button>
              <Button
                variant="secondary"
                disabled={page + 1 >= totalPages}
                onClick={() => setPage(page + 1)}
              >
                Next
              </Button>
            </div>
          </div>
        )}
      </section>
    </div>
  );
}

function RatingButtons({
  value,
  onRate,
  pending,
}: {
  value: number | null;
  onRate: (rating: number) => void;
  pending: boolean;
}) {
  return (
    <div className="flex items-center gap-1">
      {[1, 2, 3, 4, 5].map((star) => (
        <button
          key={star}
          type="button"
          aria-label={`Rate ${star} out of 5`}
          disabled={pending}
          onClick={() => onRate(star)}
          className={star <= (value ?? 0) ? 'text-accent' : 'text-text-subtle hover:text-accent'}
        >
          <Icon icon={Star} size={22} />
        </button>
      ))}
    </div>
  );
}
