import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ExternalLink } from 'lucide-react';
import { useInterview, useSubmitInterviewFeedback } from '../queries';
import type { Recommendation } from '../types';
import { useAuth } from '../../../app/auth/AuthContext';
import { Badge } from '../../../components/ui/Badge';
import { Banner } from '../../../components/ui/Banner';
import { Button } from '../../../components/ui/Button';
import { Card } from '../../../components/ui/Card';
import { Icon } from '../../../components/ui/Icon';
import { Select } from '../../../components/ui/Select';
import { Skeleton } from '../../../components/ui/Skeleton';
import { Textarea } from '../../../components/ui/Textarea';
import { useToast } from '../../../components/ui/Toast';
import { getApiErrorMessage } from '../../../lib/errors';
import { formatDate } from '../../../lib/format';

const RECOMMENDATIONS: { value: Recommendation; label: string }[] = [
  { value: 'strong_yes', label: 'Strong yes' },
  { value: 'yes', label: 'Yes' },
  { value: 'no', label: 'No' },
  { value: 'strong_no', label: 'Strong no' },
];

function recommendationLabel(value: string): string {
  return RECOMMENDATIONS.find((r) => r.value === value)?.label ?? value;
}

export function InterviewDetailPage() {
  const { interviewId } = useParams<{ interviewId: string }>();
  const { user } = useAuth();
  const { data, isLoading, isError, refetch } = useInterview(interviewId);
  const feedback = useSubmitInterviewFeedback(interviewId ?? '');
  const { toast } = useToast();

  const [scores, setScores] = useState<Record<string, number>>({});
  const [comments, setComments] = useState('');
  const [recommendation, setRecommendation] = useState<Recommendation>('yes');
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!data?.myFeedback) return;
    setScores(data.myFeedback.scores ?? {});
    setComments(data.myFeedback.comments ?? '');
    setRecommendation(data.myFeedback.recommendation);
  }, [data?.myFeedback]);

  if (isLoading) return <Skeleton className="h-96 w-full rounded-xl" />;
  if (isError || !data) return <Banner tone="error">Interview not found.</Banner>;

  const assigned = data.interviewers.some((person) => person.id === user?.id);
  const candidateName = data.candidate
    ? `${data.candidate.firstName} ${data.candidate.lastName}`.trim()
    : 'Candidate';

  async function submit() {
    setError(null);
    const rubric = data?.pack.rubric ?? [];
    if (rubric.length > 0) {
      const missing = rubric.some((item) => !scores[item.id]);
      if (missing) {
        setError('Score every rubric item before submitting.');
        return;
      }
    }
    try {
      await feedback.mutateAsync({ scores, comments: comments.trim() || null, recommendation });
      toast('Feedback saved.', 'success');
    } catch (err) {
      setError(getApiErrorMessage(err));
    }
  }

  return (
    <div className="space-y-5">
      <div>
        <Link to="/employer/interviews" className="text-sm text-text-secondary hover:text-primary-text">
          ← All interviews
        </Link>
        <div className="mt-1 flex flex-wrap items-center gap-3">
          <h1 className="text-[28px] font-bold leading-9">{candidateName}</h1>
          {data.myFeedback ? (
            <Badge tone="success">Feedback submitted</Badge>
          ) : (
            <Badge tone="neutral">Feedback pending</Badge>
          )}
        </div>
        <p className="text-text-secondary">
          {data.jobTitle} · {formatDate(data.scheduledAt)} · {data.durationMinutes} minutes
        </p>
      </div>

      <Card>
        <div className="grid gap-4 sm:grid-cols-2">
          <div>
            <div className="text-xs text-text-secondary">Meeting link</div>
            <a
              href={data.meetingLink}
              target="_blank"
              rel="noreferrer"
              className="mt-1 inline-flex items-center gap-1.5 text-sm font-medium text-primary-text hover:underline"
            >
              Open meeting <Icon icon={ExternalLink} size={14} />
            </a>
          </div>
          <div>
            <div className="text-xs text-text-secondary">Interviewers</div>
            <div className="mt-1 text-sm text-text">
              {data.interviewers
                .map((person) => `${person.name} (${person.role.replace('_', ' ')})`)
                .join(', ')}
            </div>
          </div>
        </div>
        <div className="mt-4">
          <Link
            to={`/employer/jobs/${data.jobId}/applications/${data.applicationId}`}
            className="text-sm text-primary-text hover:underline"
          >
            Application review
          </Link>
        </div>
      </Card>

      {data.pack.questions.length > 0 && (
        <Card>
          <h2 className="mb-3 text-xl font-semibold">Question pack</h2>
          <ol className="ml-5 list-decimal space-y-2 text-sm text-text">
            {data.pack.questions.map((question) => (
              <li key={question.id}>{question.text}</li>
            ))}
          </ol>
          <h3 className="mb-2 mt-5 text-xs font-semibold uppercase tracking-[0.06em] text-text-secondary">
            Rubric
          </h3>
          <ul className="space-y-2">
            {data.pack.rubric.map((item) => (
              <li key={item.id} className="rounded-lg border border-border bg-bg p-3">
                <div className="text-sm font-semibold text-text">{item.criterion}</div>
                <div className="text-sm text-text-secondary">{item.description}</div>
              </li>
            ))}
          </ul>
        </Card>
      )}

      {assigned && (
        <Card>
          <h2 className="mb-1 text-xl font-semibold">Your feedback</h2>
          <p className="mb-4 text-sm text-text-secondary">
            Scores are 1 to 5 per rubric item. Submit again to update your feedback.
          </p>
          <div className="space-y-4">
            {data.pack.rubric.map((item) => (
              <label key={item.id} className="block">
                <span className="mb-1 block text-sm font-medium text-text">{item.criterion}</span>
                <Select
                  value={scores[item.id] ? String(scores[item.id]) : ''}
                  onChange={(e) =>
                    setScores((current) => ({
                      ...current,
                      [item.id]: Number(e.target.value),
                    }))
                  }
                >
                  <option value="">Choose a score…</option>
                  {[1, 2, 3, 4, 5].map((value) => (
                    <option key={value} value={value}>
                      {value}
                    </option>
                  ))}
                </Select>
              </label>
            ))}
            <label className="block">
              <span className="mb-1 block text-sm font-medium text-text">Comments</span>
              <Textarea
                rows={4}
                value={comments}
                onChange={(e) => setComments(e.target.value.slice(0, 2000))}
                placeholder="Evidence from the interview"
                maxLength={2000}
              />
            </label>
            <label className="block">
              <span className="mb-1 block text-sm font-medium text-text">Recommendation</span>
              <Select
                value={recommendation}
                onChange={(e) => setRecommendation(e.target.value as Recommendation)}
              >
                {RECOMMENDATIONS.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </Select>
            </label>
            {error && <p className="text-sm text-accent">{error}</p>}
            <Button loading={feedback.isPending} onClick={submit}>
              Save feedback
            </Button>
          </div>
        </Card>
      )}

      {!assigned && (
        <Banner tone="info">
          Only assigned interviewers can submit feedback for this interview.
        </Banner>
      )}

      {data.allFeedback && data.allFeedback.length > 0 && (
        <Card>
          <h2 className="mb-3 text-xl font-semibold">All feedback</h2>
          <ul className="divide-y divide-border">
            {data.allFeedback.map((entry, index) => (
              <li key={entry.interviewerId ?? index} className="py-3">
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <span className="font-semibold text-text">
                    {entry.interviewerName ?? 'Former interviewer'}
                  </span>
                  <Badge tone="neutral">{recommendationLabel(entry.recommendation)}</Badge>
                </div>
                {data.pack.rubric.length > 0 && (
                  <div className="mt-1 flex flex-wrap gap-3 text-xs text-text-secondary">
                    {data.pack.rubric.map((item) => (
                      <span key={item.id}>
                        {item.criterion}: {entry.scores[item.id] ?? '—'}/5
                      </span>
                    ))}
                  </div>
                )}
                {entry.comments && <p className="mt-1 text-sm text-text">{entry.comments}</p>}
              </li>
            ))}
          </ul>
        </Card>
      )}

      <Button variant="secondary" onClick={() => refetch()}>
        Refresh
      </Button>
    </div>
  );
}
