import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { Check, Clock, GraduationCap, ListChecks } from 'lucide-react';
import { useCandidateAssessments, useAttemptHistory } from '../queries';
import { useSkills } from '../../candidate/queries';
import { Badge } from '../../../components/ui/Badge';
import { Banner } from '../../../components/ui/Banner';
import { Button } from '../../../components/ui/Button';
import { Icon } from '../../../components/ui/Icon';
import { Skeleton } from '../../../components/ui/Skeleton';
import { Table, type Column } from '../../../components/ui/Table';
import { formatDate } from '../../../lib/format';
import type { AttemptHistoryItem, CandidateAssessment } from '../types';

function cooldownActive(assessment: CandidateAssessment): boolean {
  return Boolean(
    assessment.retryAvailableAt && new Date(assessment.retryAvailableAt) > new Date(),
  );
}

export function AssessmentsPage() {
  const [params, setParams] = useSearchParams();
  const page = Number(params.get('page') ?? '0');
  const assessments = useCandidateAssessments();
  const history = useAttemptHistory(page);
  const { data: skills } = useSkills();
  const navigate = useNavigate();

  const verifiedSkills = (skills ?? []).filter((skill) => skill.verified);
  const profileSkillIds = new Set((skills ?? []).map((skill) => skill.skillId));

  function setPage(next: number) {
    const copy = new URLSearchParams(params);
    if (next === 0) copy.delete('page');
    else copy.set('page', String(next));
    setParams(copy);
  }

  function actionLabel(assessment: CandidateAssessment): string {
    if (assessment.inProgressAttemptId) return 'Resume';
    if (assessment.lastAttemptAt) return 'Retake';
    return 'Start';
  }

  const columns: Column<AttemptHistoryItem>[] = [
    {
      header: 'Assessment',
      render: (row) => <span className="font-medium text-text">{row.assessmentTitle}</span>,
    },
    {
      header: 'Score',
      render: (row) =>
        row.score === null ? <span className="text-text-subtle">—</span> : `${row.score}%`,
    },
    {
      header: 'Result',
      render: (row) =>
        row.passed === null ? (
          <span className="text-text-subtle">—</span>
        ) : row.passed ? (
          <Badge status="verified" />
        ) : (
          <Badge status="failed" />
        ),
    },
    {
      header: 'Status',
      render: (row) =>
        row.status === 'auto_submitted' ? 'Auto-submitted' : 'Submitted',
    },
    { header: 'Submitted', render: (row) => formatDate(row.submittedAt) },
  ];

  const totalPages = history.data?.totalPages ?? 0;

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-[28px] font-bold leading-9">Assessments</h1>
        <p className="text-text-secondary">
          Prove a skill with a timed multiple-choice test. A pass adds a Verified badge to your
          profile.
        </p>
      </div>

      {verifiedSkills.length > 0 && (
        <div className="rounded-xl border border-border bg-surface p-4 shadow-sm">
          <h2 className="mb-2 text-xs font-semibold uppercase tracking-[0.06em] text-text-secondary">
            Verified skills
          </h2>
          <div className="flex flex-wrap gap-2">
            {verifiedSkills.map((skill) => (
              <span
                key={skill.id}
                className="inline-flex items-center gap-1.5 rounded-full bg-primary-subtle px-3 py-1 text-sm font-medium text-primary-text"
              >
                <Icon icon={Check} size={14} />
                {skill.name}
              </span>
            ))}
          </div>
        </div>
      )}

      {assessments.isLoading && <Skeleton className="h-48 w-full rounded-xl" />}
      {assessments.isError && (
        <Banner tone="error">
          Could not load assessments.{' '}
          <button type="button" className="underline" onClick={() => assessments.refetch()}>
            Retry
          </button>
        </Banner>
      )}

      {!assessments.isLoading && !assessments.isError && (
        <div className="grid gap-4 md:grid-cols-2">
          {assessments.data?.length === 0 && (
            <div className="rounded-xl border border-border bg-surface p-8 text-center text-sm text-text-secondary md:col-span-2">
              No assessments are available right now.
            </div>
          )}
          {assessments.data?.map((assessment) => {
            const blocked = cooldownActive(assessment);
            const recommended = profileSkillIds.has(assessment.skillId);
            return (
              <div
                key={assessment.id}
                className={
                  recommended
                    ? 'flex flex-col justify-between rounded-xl border border-primary bg-surface p-5 shadow-sm'
                    : 'flex flex-col justify-between rounded-xl border border-border bg-surface p-5 shadow-sm'
                }
              >
                <div>
                  <div className="flex items-start justify-between gap-3">
                    <div>
                      <h2 className="text-lg font-semibold text-text">{assessment.title}</h2>
                      <p className="text-sm text-text-secondary">{assessment.skillName ?? 'Skill'}</p>
                      {recommended && (
                        <div className="mt-1.5">
                          <Badge tone="progress">Recommended from your CV</Badge>
                        </div>
                      )}
                    </div>
                    {assessment.inProgressAttemptId ? (
                      <Badge tone="progress">In progress</Badge>
                    ) : assessment.passed ? (
                      <Badge status="verified" />
                    ) : assessment.lastAttemptAt ? (
                      <Badge status="failed" />
                    ) : (
                      <Badge tone="neutral">Not started</Badge>
                    )}
                  </div>

                  <dl className="mt-4 grid grid-cols-3 gap-2 text-sm">
                    <div>
                      <dt className="text-xs text-text-secondary">Questions</dt>
                      <dd className="font-medium text-text">{assessment.questionCount}</dd>
                    </div>
                    <div>
                      <dt className="text-xs text-text-secondary">Duration</dt>
                      <dd className="font-medium text-text">{assessment.durationMinutes} min</dd>
                    </div>
                    <div>
                      <dt className="text-xs text-text-secondary">Pass mark</dt>
                      <dd className="font-medium text-text">{assessment.passMark}%</dd>
                    </div>
                  </dl>

                  <div className="mt-4 flex items-center gap-4 text-sm text-text-secondary">
                    <span className="inline-flex items-center gap-1.5">
                      <Icon icon={ListChecks} size={15} />
                      Best score: {assessment.bestScore === null ? '—' : `${assessment.bestScore}%`}
                    </span>
                    {assessment.inProgressAttemptId && (
                      <span className="inline-flex items-center gap-1.5 text-primary-text">
                        <Icon icon={Clock} size={15} />
                        In progress
                      </span>
                    )}
                  </div>

                  {blocked && (
                    <p className="mt-3 text-sm text-accent">
                      Retake available {formatDate(assessment.retryAvailableAt)}
                    </p>
                  )}
                </div>

                <div className="mt-5 flex items-center justify-between gap-3">
                  <span className="text-xs text-text-secondary">
                    {assessment.lastAttemptAt
                      ? `Last attempt ${formatDate(assessment.lastAttemptAt)}`
                      : 'Not attempted yet'}
                  </span>
                  <Button
                    onClick={() =>
                      navigate(`/candidate/assessments/${assessment.id}/attempt`)
                    }
                    disabled={blocked}
                  >
                    {blocked ? 'On cooldown' : actionLabel(assessment)}
                  </Button>
                </div>
              </div>
            );
          })}
        </div>
      )}

      <section>
        <div className="mb-3 flex items-center justify-between">
          <h2 className="text-xl font-semibold">Attempt history</h2>
          <Link
            to="/candidate/profile"
            className="inline-flex items-center gap-1.5 text-sm text-primary-text hover:underline"
          >
            <Icon icon={GraduationCap} size={15} /> View profile skills
          </Link>
        </div>
        {history.isLoading && <Skeleton className="h-40 w-full rounded-xl" />}
        {history.isError && <Banner tone="error">Could not load your attempt history.</Banner>}
        {!history.isLoading && !history.isError && (
          <Table
            columns={columns}
            rows={history.data?.items ?? []}
            rowKey={(row) => row.id}
            empty={
              <div className="rounded-xl border border-border bg-surface p-8 text-center text-sm text-text-secondary">
                No attempts yet. Start an assessment to see your scores here.
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
