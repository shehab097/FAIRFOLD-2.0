import { useEffect, useState } from 'react';
import { ArrowDown, ArrowUp, Plus, Trash2, Wand2 } from 'lucide-react';
import {
  useGenerateInterviewPack,
  useInterviewPack,
  useSaveInterviewPack,
} from '../queries';
import type { PackQuestion, PackRubricItem } from '../types';
import { Banner } from '../../../components/ui/Banner';
import { Button } from '../../../components/ui/Button';
import { Icon } from '../../../components/ui/Icon';
import { Input } from '../../../components/ui/Input';
import { Skeleton } from '../../../components/ui/Skeleton';
import { Textarea } from '../../../components/ui/Textarea';
import { useToast } from '../../../components/ui/Toast';
import { getApiErrorMessage } from '../../../lib/errors';

function newId(): string {
  if (typeof crypto !== 'undefined' && 'randomUUID' in crypto) {
    return crypto.randomUUID();
  }
  return Math.random().toString(36).slice(2);
}

export function InterviewPackTab({
  jobId,
  screeningQuestions,
}: {
  jobId: string;
  screeningQuestions: string[];
}) {
  const { data, isLoading, isError, refetch } = useInterviewPack(jobId);
  const generate = useGenerateInterviewPack(jobId);
  const save = useSaveInterviewPack(jobId);
  const { toast } = useToast();

  const [questions, setQuestions] = useState<PackQuestion[]>([]);
  const [rubric, setRubric] = useState<PackRubricItem[]>([]);
  const [provider, setProvider] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!data) return;
    setQuestions(data.questions);
    setRubric(data.rubric);
  }, [data]);

  async function runGenerate() {
    setError(null);
    try {
      const proposal = await generate.mutateAsync();
      setQuestions(proposal.questions);
      setRubric(proposal.rubric);
      setProvider(proposal.provider);
      toast('Proposal generated. Review it and save.', 'success');
    } catch (err) {
      setError(getApiErrorMessage(err));
    }
  }

  async function runSave() {
    setError(null);
    if (questions.length === 0 || rubric.length === 0) {
      setError('Add at least one question and one rubric item.');
      return;
    }
    try {
      await save.mutateAsync({ questions, rubric });
      toast('Interview pack saved.', 'success');
      setProvider(null);
    } catch (err) {
      setError(getApiErrorMessage(err));
    }
  }

  function move<T>(list: T[], index: number, delta: number): T[] {
    const target = index + delta;
    if (target < 0 || target >= list.length) return list;
    const copy = [...list];
    [copy[index], copy[target]] = [copy[target], copy[index]];
    return copy;
  }

  if (isLoading) return <Skeleton className="h-64 w-full rounded-xl" />;
  if (isError) {
    return (
      <Banner tone="error">
        Could not load the interview pack.{' '}
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
          <h2 className="text-xl font-semibold">Interview pack</h2>
          <p className="text-sm text-text-secondary">
            Six to ten questions and a scoring rubric. Generated drafts are not saved until you
            save them.
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          <Button variant="secondary" loading={generate.isPending} onClick={runGenerate}>
            <Icon icon={Wand2} size={16} />
            Generate with AI
          </Button>
          <Button loading={save.isPending} onClick={runSave}>
            Save pack
          </Button>
        </div>
      </div>

      {provider === 'rule_based' && (
        <Banner tone="warning">
          AI was unavailable, so the fallback template was used. Review and edit every question and
          rubric item before saving.
        </Banner>
      )}
      {error && <Banner tone="error">{error}</Banner>}

      {screeningQuestions.length > 0 && (
        <section className="rounded-xl border border-border bg-surface p-5 shadow-sm">
          <h3 className="text-base font-semibold">Saved screening questions (reference)</h3>
          <ol className="ml-5 mt-2 list-decimal space-y-1 text-sm text-text-secondary">
            {screeningQuestions.map((question) => (
              <li key={question}>{question}</li>
            ))}
          </ol>
        </section>
      )}

      <section className="rounded-xl border border-border bg-surface p-5 shadow-sm">
        <div className="mb-3 flex items-center justify-between">
          <h3 className="text-base font-semibold">Questions ({questions.length})</h3>
          <Button
            variant="secondary"
            onClick={() => setQuestions((current) => [...current, { id: newId(), text: '' }])}
          >
            <Icon icon={Plus} size={16} />
            Add question
          </Button>
        </div>
        {questions.length === 0 && (
          <p className="text-sm text-text-subtle">No questions yet. Generate a proposal or add one.</p>
        )}
        <div className="space-y-2">
          {questions.map((question, index) => (
            <div key={question.id} className="flex items-start gap-2">
              <span className="mt-2.5 w-6 shrink-0 text-xs tabular-nums text-text-secondary">
                {index + 1}.
              </span>
              <Input
                value={question.text}
                onChange={(e) =>
                  setQuestions((current) =>
                    current.map((row, i) => (i === index ? { ...row, text: e.target.value } : row)),
                  )
                }
                maxLength={500}
              />
              <div className="flex gap-1">
                <Button
                  variant="ghost"
                  className="h-10 px-2"
                  aria-label="Move question up"
                  disabled={index === 0}
                  onClick={() => setQuestions((current) => move(current, index, -1))}
                >
                  <Icon icon={ArrowUp} size={16} />
                </Button>
                <Button
                  variant="ghost"
                  className="h-10 px-2"
                  aria-label="Move question down"
                  disabled={index === questions.length - 1}
                  onClick={() => setQuestions((current) => move(current, index, 1))}
                >
                  <Icon icon={ArrowDown} size={16} />
                </Button>
                <Button
                  variant="ghost"
                  className="h-10 px-2"
                  aria-label="Remove question"
                  onClick={() => setQuestions((current) => current.filter((_, i) => i !== index))}
                >
                  <Icon icon={Trash2} size={16} />
                </Button>
              </div>
            </div>
          ))}
        </div>
      </section>

      <section className="rounded-xl border border-border bg-surface p-5 shadow-sm">
        <div className="mb-3 flex items-center justify-between">
          <h3 className="text-base font-semibold">Rubric ({rubric.length})</h3>
          <Button
            variant="secondary"
            onClick={() =>
              setRubric((current) => [
                ...current,
                { id: newId(), criterion: '', description: '' },
              ])
            }
          >
            <Icon icon={Plus} size={16} />
            Add rubric item
          </Button>
        </div>
        {rubric.length === 0 && (
          <p className="text-sm text-text-subtle">No rubric items yet.</p>
        )}
        <div className="space-y-3">
          {rubric.map((item, index) => (
            <div key={item.id} className="rounded-lg border border-border bg-bg p-3">
              <div className="flex items-start gap-2">
                <div className="flex-1 space-y-2">
                  <Input
                    value={item.criterion}
                    onChange={(e) =>
                      setRubric((current) =>
                        current.map((row, i) =>
                          i === index ? { ...row, criterion: e.target.value } : row,
                        ),
                      )
                    }
                    placeholder="Criterion (for example, Technical skills)"
                    maxLength={100}
                  />
                  <Textarea
                    rows={2}
                    value={item.description}
                    onChange={(e) =>
                      setRubric((current) =>
                        current.map((row, i) =>
                          i === index ? { ...row, description: e.target.value } : row,
                        ),
                      )
                    }
                    placeholder="What strong evidence looks like"
                    maxLength={500}
                  />
                </div>
                <div className="flex gap-1">
                  <Button
                    variant="ghost"
                    className="h-8 px-2"
                    aria-label="Move rubric item up"
                    disabled={index === 0}
                    onClick={() => setRubric((current) => move(current, index, -1))}
                  >
                    <Icon icon={ArrowUp} size={16} />
                  </Button>
                  <Button
                    variant="ghost"
                    className="h-8 px-2"
                    aria-label="Move rubric item down"
                    disabled={index === rubric.length - 1}
                    onClick={() => setRubric((current) => move(current, index, 1))}
                  >
                    <Icon icon={ArrowDown} size={16} />
                  </Button>
                  <Button
                    variant="ghost"
                    className="h-8 px-2"
                    aria-label="Remove rubric item"
                    onClick={() => setRubric((current) => current.filter((_, i) => i !== index))}
                  >
                    <Icon icon={Trash2} size={16} />
                  </Button>
                </div>
              </div>
            </div>
          ))}
        </div>
      </section>
    </div>
  );
}
