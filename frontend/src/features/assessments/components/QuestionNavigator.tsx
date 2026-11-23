import { cn } from '../../../lib/cn';
import type { AttemptQuestion } from '../types';

export function QuestionNavigator({
  questions,
  current,
  answers,
  onSelect,
}: {
  questions: AttemptQuestion[];
  current: number;
  answers: Record<string, string>;
  onSelect: (index: number) => void;
}) {
  return (
    <nav aria-label="Questions" className="flex flex-wrap gap-2">
      {questions.map((question, index) => {
        const answered = Boolean(answers[question.id]);
        return (
          <button
            key={question.id}
            type="button"
            onClick={() => onSelect(index)}
            aria-current={index === current ? 'step' : undefined}
            aria-label={`Question ${index + 1}${answered ? ', answered' : ', not answered'}`}
            className={cn(
              'flex h-10 w-10 items-center justify-center rounded-lg border text-sm font-semibold tabular-nums transition-colors',
              index === current
                ? 'border-primary bg-primary text-white'
                : answered
                  ? 'border-primary bg-primary-subtle text-primary-text'
                  : 'border-border bg-surface text-text-secondary hover:bg-raised',
            )}
          >
            {index + 1}
          </button>
        );
      })}
    </nav>
  );
}
