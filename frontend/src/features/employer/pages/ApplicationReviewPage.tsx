import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { TriangleAlert, Check } from 'lucide-react';
import {
  useEmployerReview,
  usePullBackApplication,
  useRejectApplication,
  useShortlistApplication,
} from '../queries';
import { useHireApplication, useScheduleInterview } from '../../interviews/queries';
import { useInterviewers } from '../../team/queries';
import { teamRoleLabel } from '../../team/types';
import { MessageThread } from '../../messages/components/MessageThread';
import { StrippedResume } from '../components/StrippedResume';
import type { EmployerIdentity } from '../../jobs/types';
import { Badge } from '../../../components/ui/Badge';
import { Banner } from '../../../components/ui/Banner';
import { Button } from '../../../components/ui/Button';
import { Checkbox } from '../../../components/ui/Checkbox';
import { Dialog } from '../../../components/ui/Dialog';
import { Input } from '../../../components/ui/Input';
import { ScoreMeter } from '../../../components/ui/ScoreMeter';
import { Skeleton } from '../../../components/ui/Skeleton';
import { Tabs } from '../../../components/ui/Tabs';
import { useToast } from '../../../components/ui/Toast';
import { getApiErrorMessage } from '../../../lib/errors';
import { formatDate } from '../../../lib/format';

function ModeBadge({ mode }: { mode: string | null }) {
  if (!mode) return null;
  if (mode === 'rule_based') {
    return (
      <Badge tone="warning" icon={TriangleAlert}>
        Rule-based
      </Badge>
    );
  }
  return <Badge tone="neutral">AI</Badge>;
}

function focusDuration(seconds: number): string {
  if (seconds < 60) return `${seconds} s`;
  const minutes = Math.floor(seconds / 60);
  const rest = seconds % 60;
  return rest === 0 ? `${minutes} min` : `${minutes} min ${rest} s`;
}

export function ApplicationReviewPage() {
  const { jobId, applicationId } = useParams<{ jobId: string; applicationId: string }>();
  const { data: review, isLoading, isError } = useEmployerReview(applicationId);
  const shortlist = useShortlistApplication();
  const reject = useRejectApplication();
  const pullBack = usePullBackApplication();
  const schedule = useScheduleInterview();
  const hire = useHireApplication();
  const { data: interviewers } = useInterviewers();
  const { toast } = useToast();
  const [activeClaim, setActiveClaim] = useState<number | null>(null);
  const [tab, setTab] = useState<'review' | 'messages'>('review');
  const [shortlistOpen, setShortlistOpen] = useState(false);
  const [rejectOpen, setRejectOpen] = useState(false);
  const [scheduleOpen, setScheduleOpen] = useState(false);
  const [hireOpen, setHireOpen] = useState(false);
  const [reason, setReason] = useState('');
  const [identity, setIdentity] = useState<EmployerIdentity | null>(null);
  const [scheduledAt, setScheduledAt] = useState('');
  const [duration, setDuration] = useState('45');
  const [meetingLink, setMeetingLink] = useState('');
  const [selectedInterviewers, setSelectedInterviewers] = useState<string[]>([]);
  const [scheduleError, setScheduleError] = useState<string | null>(null);

  useEffect(() => {
    if (review?.identity) setIdentity(review.identity);
  }, [review?.identity]);

  if (isLoading) return <Skeleton className="h-96 w-full" />;
  if (isError || !review) return <Banner tone="error">Application not found.</Banner>;

  const belowCutoff =
    review.score !== null && review.cutoffScore !== null && review.score < review.cutoffScore;
  const aboveCutoff =
    review.score !== null && review.cutoffScore !== null && review.score >= review.cutoffScore;
  const gateBlocked = review.gateStatus === 'pending' || review.gateStatus === 'failed';
  const canShortlist = review.status === 'screened' && !gateBlocked;
  const canReject =
    review.status === 'screened' ||
    review.status === 'not_matched' ||
    review.status === 'shortlisted' ||
    review.status === 'interview';
  const canPullBack = review.status === 'not_matched';
  const canSchedule = review.status === 'shortlisted' || review.status === 'interview';
  const canHire = review.status === 'interview';
  const cutoffReasonRules =
    review.status === 'screened' || review.status === 'not_matched';
  const reviewId = review.id;

  function revealClaim(index: number) {
    setActiveClaim(index);
    document.getElementById(`claim-${index}`)?.scrollIntoView({ behavior: 'smooth', block: 'center' });
  }

  function clickQuote(index: number) {
    setActiveClaim(index);
    document.getElementById(`claim-${index}`)?.scrollIntoView({ behavior: 'smooth', block: 'center' });
  }

  async function confirmShortlist() {
    if ((belowCutoff || reason.trim().length > 0) && reason.trim().length < 10) {
      toast('Write a reason of at least 10 characters.', 'error');
      return;
    }
    try {
      const updated = await shortlist.mutateAsync({
        id: reviewId,
        reason: reason.trim() || undefined,
      });
      setIdentity(updated.identity);
      setShortlistOpen(false);
      setReason('');
      toast('Candidate shortlisted. Identity revealed and logged.', 'success');
    } catch (err) {
      toast(getApiErrorMessage(err), 'error');
    }
  }

  async function confirmReject() {
    if (cutoffReasonRules && (aboveCutoff || reason.trim().length > 0) && reason.trim().length < 10) {
      toast('Write a reason of at least 10 characters.', 'error');
      return;
    }
    if (!cutoffReasonRules && reason.trim().length > 0 && reason.trim().length < 10) {
      toast('Write a reason of at least 10 characters, or leave it empty.', 'error');
      return;
    }
    try {
      await reject.mutateAsync({ id: reviewId, reason: reason.trim() || undefined });
      setRejectOpen(false);
      setReason('');
      toast('Application rejected.', 'success');
    } catch (err) {
      toast(getApiErrorMessage(err), 'error');
    }
  }

  async function confirmSchedule() {
    setScheduleError(null);
    if (selectedInterviewers.length === 0) {
      setScheduleError('Choose at least one interviewer.');
      return;
    }
    if (!scheduledAt) {
      setScheduleError('Choose a date and time.');
      return;
    }
    if (!meetingLink.trim().startsWith('https://')) {
      setScheduleError('The meeting link must start with https://.');
      return;
    }
    try {
      await schedule.mutateAsync({
        applicationId: reviewId,
        input: {
          scheduledAt: new Date(scheduledAt).toISOString(),
          durationMinutes: Number(duration),
          meetingLink: meetingLink.trim(),
          interviewerIds: selectedInterviewers,
        },
      });
      setScheduleOpen(false);
      setScheduledAt('');
      setMeetingLink('');
      setSelectedInterviewers([]);
      toast('Interview scheduled. The candidate and interviewers were notified.', 'success');
    } catch (err) {
      setScheduleError(getApiErrorMessage(err));
    }
  }

  async function confirmHire() {
    try {
      await hire.mutateAsync(reviewId);
      setHireOpen(false);
      toast('Candidate marked as hired.', 'success');
    } catch (err) {
      toast(getApiErrorMessage(err), 'error');
    }
  }

  async function confirmPullBack() {
    try {
      await pullBack.mutateAsync(reviewId);
      toast('Candidate pulled back into screening.', 'success');
    } catch (err) {
      toast(getApiErrorMessage(err), 'error');
    }
  }

  const name = identity ? `${identity.firstName} ${identity.lastName}`.trim() : null;

  return (
    <div className="space-y-5">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <Link
            to={`/employer/jobs/${jobId}`}
            className="text-sm text-text-secondary hover:text-primary-text"
          >
            ← Back to applicants
          </Link>
          <div className="mt-1 flex flex-wrap items-center gap-2.5">
            <h1 className="text-[28px] font-bold leading-9">{review.jobTitle}</h1>
            <span className="rounded-full border border-border bg-raised px-3 py-0.5 text-sm font-semibold text-text">
              {review.alias}
            </span>
            <Badge status={review.status} />
          </div>
        </div>
        <ModeBadge mode={review.scoringMode} />
      </div>

      <Banner tone="info">
        Scores are advisory, you decide. Identity is revealed only when you shortlist, and the reveal
        is logged.
      </Banner>

      {review.identity && (
        <Tabs
          tabs={[
            { key: 'review', label: 'Review' },
            { key: 'messages', label: 'Messages' },
          ]}
          active={tab}
          onChange={(key) => setTab(key as 'review' | 'messages')}
        />
      )}

      {tab === 'messages' && review.identity ? (
        <MessageThread applicationId={review.id} />
      ) : (
      <div className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_390px]">
        <aside className="space-y-4 lg:order-2 lg:sticky lg:top-20 lg:self-start">
          <div className="rounded-xl border border-border bg-surface p-5 shadow-sm">
            <div className="flex items-start justify-between gap-3">
              <div>
                <div className="text-xs text-text-secondary">Alias</div>
                <div className="mt-0.5 text-lg font-bold text-text">{review.alias}</div>
              </div>
              <Badge status={review.status} />
            </div>
            <div className="mt-4">
              {review.status === 'not_matched' ? (
                <div className="rounded-lg border border-border bg-accent-subtle p-3 text-sm text-accent">
                  Not matched: {review.notMatchedRule ?? 'A hard filter excluded this candidate.'}
                </div>
              ) : (
                <ScoreMeter score={review.score} cutoff={review.cutoffScore} />
              )}
            </div>
            <dl className="mt-4 grid grid-cols-2 gap-3 text-sm">
              <div>
                <dt className="text-xs text-text-secondary">Gate</dt>
                <dd className="mt-1"><Badge status={review.gateStatus} /></dd>
              </div>
              <div>
                <dt className="text-xs text-text-secondary">Bias</dt>
                <dd className="mt-1">
                  {review.biasStatus ? <Badge status={review.biasStatus} /> : <span className="text-text-subtle">—</span>}
                </dd>
              </div>
              <div>
                <dt className="text-xs text-text-secondary">Cut-off</dt>
                <dd className="mt-1 font-medium">{review.cutoffScore ?? '—'}</dd>
              </div>
              <div>
                <dt className="text-xs text-text-secondary">Mode</dt>
                <dd className="mt-1"><ModeBadge mode={review.scoringMode} /></dd>
              </div>
            </dl>
            {review.biasNote && (
              <p className="mt-3 rounded-lg bg-accent-subtle p-3 text-xs text-accent">{review.biasNote}</p>
            )}
            {identity && (
              <div className="mt-4 rounded-xl border border-border bg-primary-subtle p-4">
                <div className="text-xs font-semibold uppercase tracking-[0.06em] text-primary-text">
                  Identity revealed
                </div>
                <div className="mt-1 font-semibold text-text">{name}</div>
                <div className="text-sm text-text">{identity.email}</div>
                {identity.phone && <div className="text-sm text-text">{identity.phone}</div>}
              </div>
            )}
            <div className="mt-4 flex flex-wrap gap-2">
              {canShortlist && (
                <Button onClick={() => { setReason(''); setShortlistOpen(true); }}>
                  Shortlist
                </Button>
              )}
              {review.status === 'screened' && gateBlocked && (
                <Button disabled title="The assessment gate has not passed.">
                  Shortlist
                </Button>
              )}
              {canSchedule && (
                <Button variant="secondary" onClick={() => setScheduleOpen(true)}>
                  Schedule interview
                </Button>
              )}
              {canHire && <Button onClick={() => setHireOpen(true)}>Mark hired</Button>}
              {canReject && (
                <Button variant="danger" onClick={() => { setReason(''); setRejectOpen(true); }}>
                  Reject
                </Button>
              )}
              {canPullBack && (
                <Button variant="secondary" loading={pullBack.isPending} onClick={confirmPullBack}>
                  Pull back
                </Button>
              )}
            </div>
          </div>

          <div className="rounded-xl border border-border bg-surface p-5 shadow-sm">
            <h2 className="mb-3 text-base font-semibold">Claims</h2>
            {review.claims.length === 0 && (
              <p className="text-sm text-text-subtle">No claims for this application.</p>
            )}
            <div className="space-y-3">
              {review.claims.map((claim, index) => (
                <button
                  key={`${claim.requirement}-${index}`}
                  id={`claim-${index}`}
                  type="button"
                  onClick={() => revealClaim(index)}
                  className={
                    'w-full rounded-xl border p-3 text-left transition-colors ' +
                    (activeClaim === index
                      ? 'border-primary-text bg-primary-subtle'
                      : 'border-border bg-bg hover:bg-raised')
                  }
                >
                  <div className="flex items-start justify-between gap-2">
                    <span className="text-sm font-semibold text-text">{claim.requirement}</span>
                    <Badge status={claim.verdict} />
                  </div>
                  {claim.quote && (
                    <p className="mt-2 rounded-md bg-highlight px-2 py-1 text-xs text-text">
                      “{claim.quote}”
                    </p>
                  )}
                  {claim.source === 'rule_based' && (
                    <div className="mt-2">
                      <Badge tone="warning" icon={TriangleAlert}>Rule-based</Badge>
                    </div>
                  )}
                </button>
              ))}
            </div>
          </div>

          {review.missingSkills.length > 0 && (
            <div className="rounded-xl border border-border bg-surface p-5 shadow-sm">
              <h2 className="mb-2 text-base font-semibold">Missing skills</h2>
              <div className="flex flex-wrap gap-1.5">
                {review.missingSkills.map((skill) => (
                  <span
                    key={skill}
                    className="rounded-full border border-border bg-raised px-2.5 py-0.5 text-xs text-text-secondary"
                  >
                    {skill}
                  </span>
                ))}
              </div>
            </div>
          )}

          {review.assessments.length > 0 && (
            <div className="rounded-xl border border-border bg-surface p-5 shadow-sm">
              <h2 className="mb-1 text-base font-semibold">Assessments</h2>
              <p className="mb-3 text-xs text-text-secondary">
                Focus signals are neutral observations. Does not affect the score.
              </p>
              <div className="space-y-4">
                {review.assessments.map((assessment, index) => (
                  <div key={`${assessment.assessmentTitle}-${index}`} className="rounded-lg border border-border bg-bg p-3">
                    <div className="flex items-start justify-between gap-2">
                      <span className="text-sm font-semibold text-text">
                        {assessment.assessmentTitle ?? 'Assessment'}
                      </span>
                      <Badge
                        status={
                          assessment.passed
                            ? 'passed'
                            : assessment.bestScore === null
                              ? 'pending'
                              : 'failed'
                        }
                      />
                    </div>
                    <div className="mt-1 text-xs text-text-secondary">
                      Best {assessment.bestScore === null ? '—' : `${assessment.bestScore}%`} ·
                      minimum {assessment.minScore}%
                    </div>
                    <div className="mt-2 flex flex-wrap items-center gap-1.5">
                      <span className="inline-flex items-center gap-1 rounded-full border border-border bg-raised px-2.5 py-0.5 text-xs text-text-secondary">
                        {assessment.focus.recorded
                          ? `Left the tab ${assessment.focus.leftCount} time${assessment.focus.leftCount === 1 ? '' : 's'}, ${focusDuration(assessment.focus.totalSeconds)} in total`
                          : 'Not recorded'}
                      </span>
                      <span className="text-[11px] text-text-subtle">
                        Does not affect the score
                      </span>
                    </div>
                    {assessment.focus.explanation && (
                      <p className="mt-2 rounded-md bg-highlight px-2 py-1 text-xs text-text">
                        “{assessment.focus.explanation}”
                      </p>
                    )}
                  </div>
                ))}
              </div>
            </div>
          )}

          {review.verifiedSkills.length > 0 && (
            <div className="rounded-xl border border-border bg-surface p-5 shadow-sm">
              <h2 className="mb-2 text-base font-semibold">Verified skills</h2>
              <div className="flex flex-wrap gap-1.5">
                {review.verifiedSkills.map((skill) => (
                  <span
                    key={skill}
                    className="inline-flex items-center gap-1 rounded-full bg-primary-subtle px-2.5 py-0.5 text-xs font-medium text-primary-text"
                  >
                    <Check size={12} />
                    {skill}
                  </span>
                ))}
              </div>
            </div>
          )}

          {canSchedule && (
            <div className="rounded-xl border border-border bg-surface p-5 shadow-sm">
              <h2 className="mb-3 text-base font-semibold">Interviews</h2>
              {review.interviews.length === 0 ? (
                <p className="text-sm text-text-subtle">No interview scheduled yet.</p>
              ) : (
                <ul className="space-y-3">
                  {review.interviews.map((interview) => (
                    <li key={interview.id} className="rounded-lg border border-border bg-bg p-3">
                      <div className="flex flex-wrap items-start justify-between gap-2">
                        <div>
                          <div className="text-sm font-semibold text-text">
                            {formatDate(interview.scheduledAt)}
                          </div>
                          <div className="text-xs text-text-secondary">
                            {interview.durationMinutes} minutes ·{' '}
                            {interview.interviewers.map((person) => person.name).join(', ')}
                          </div>
                          <div className="mt-1 text-xs text-text-secondary">
                            Feedback: {interview.feedbackCount} of{' '}
                            {interview.interviewers.length}
                          </div>
                        </div>
                        <Link
                          to={`/employer/interviews/${interview.id}`}
                          className="text-sm font-semibold text-primary-text hover:underline"
                        >
                          Open
                        </Link>
                      </div>
                    </li>
                  ))}
                </ul>
              )}
            </div>
          )}

          <div className="rounded-xl border border-border bg-surface p-5 shadow-sm">
            <h2 className="mb-3 text-base font-semibold">History</h2>
            <ol className="space-y-2">
              {review.history.map((entry, index) => (
                <li key={index} className="flex items-center justify-between gap-2 text-sm">
                  <Badge status={entry.status} />
                  <span className="text-xs text-text-secondary">{formatDate(entry.at)}</span>
                </li>
              ))}
            </ol>
          </div>
        </aside>

        <article className="rounded-xl border border-border bg-surface p-5 shadow-sm lg:order-1">
          <h2 className="mb-3 text-base font-semibold">Stripped resume</h2>
          <p className="mb-4 text-xs text-text-secondary">
            Personal details are removed. Highlighted quotes support the claims on the right — click a
            quote or a claim to jump between them.
          </p>
          {review.strippedText ? (
            <StrippedResume
              text={review.strippedText}
              claims={review.claims}
              activeClaim={activeClaim}
              onSelectClaim={clickQuote}
            />
          ) : (
            <p className="text-sm text-text-subtle">No stripped resume text.</p>
          )}
        </article>
      </div>
      )}

      <Dialog
        open={shortlistOpen}
        title="Shortlist candidate"
        onClose={() => setShortlistOpen(false)}
      >
        <div className="space-y-4">
          <p className="text-sm text-text">
            Shortlisting reveals this candidate&apos;s name, email and phone to your company, and
            writes an <code>identity_reveal</code> audit entry. This cannot be undone.
          </p>
          {belowCutoff && (
            <div className="rounded-lg bg-accent-subtle p-3 text-sm text-accent">
              This score ({review.score}) is below the cut-off ({review.cutoffScore}). A written
              reason of at least 10 characters is required.
            </div>
          )}
          <div>
            <label className="mb-1 block text-xs text-text-secondary">
              {belowCutoff ? 'Reason (required)' : 'Reason (optional)'}
            </label>
            <textarea
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              rows={3}
              className="w-full rounded-md border border-border bg-bg px-3 py-2 text-sm text-text placeholder:text-text-subtle focus:border-primary focus:outline-none"
              placeholder="Why are you shortlisting this candidate?"
            />
          </div>
          <div className="flex justify-end gap-3">
            <Button variant="secondary" onClick={() => setShortlistOpen(false)}>
              Cancel
            </Button>
            <Button loading={shortlist.isPending} onClick={confirmShortlist}>
              Reveal and shortlist
            </Button>
          </div>
        </div>
      </Dialog>

      <Dialog open={rejectOpen} title="Reject application" onClose={() => setRejectOpen(false)}>
        <div className="space-y-4">
          {cutoffReasonRules && aboveCutoff && (
            <div className="rounded-lg bg-accent-subtle p-3 text-sm text-accent">
              This score ({review.score}) is at or above the cut-off ({review.cutoffScore}). A written
              reason of at least 10 characters is required.
            </div>
          )}
          <div>
            <label className="mb-1 block text-xs text-text-secondary">
              {cutoffReasonRules && aboveCutoff ? 'Reason (required)' : 'Reason (optional)'}
            </label>
            <textarea
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              rows={3}
              className="w-full rounded-md border border-border bg-bg px-3 py-2 text-sm text-text placeholder:text-text-subtle focus:border-primary focus:outline-none"
              placeholder="Why are you rejecting this candidate?"
            />
          </div>
          <div className="flex justify-end gap-3">
            <Button variant="secondary" onClick={() => setRejectOpen(false)}>
              Cancel
            </Button>
            <Button variant="danger" loading={reject.isPending} onClick={confirmReject}>
              Reject
            </Button>
          </div>
        </div>
      </Dialog>

      <Dialog
        open={scheduleOpen}
        title="Schedule interview"
        onClose={() => setScheduleOpen(false)}
      >
        <div className="space-y-4">
          <div className="grid gap-3 sm:grid-cols-2">
            <label className="block">
              <span className="mb-1 block text-xs text-text-secondary">Date and time</span>
              <Input
                type="datetime-local"
                value={scheduledAt}
                onChange={(e) => setScheduledAt(e.target.value)}
              />
            </label>
            <label className="block">
              <span className="mb-1 block text-xs text-text-secondary">Duration (minutes)</span>
              <Input
                type="number"
                min={15}
                max={240}
                value={duration}
                onChange={(e) => setDuration(e.target.value)}
              />
            </label>
          </div>
          <label className="block">
            <span className="mb-1 block text-xs text-text-secondary">Meeting link</span>
            <Input
              type="url"
              value={meetingLink}
              onChange={(e) => setMeetingLink(e.target.value)}
              placeholder="https://meet.example.com/…"
            />
          </label>
          <div>
            <div className="mb-1 text-xs text-text-secondary">Interviewers</div>
            <div className="space-y-2 rounded-lg border border-border bg-bg p-3">
              {(interviewers ?? []).length === 0 && (
                <p className="text-sm text-text-subtle">
                  No active team members. Invite an interviewer first.
                </p>
              )}
              {(interviewers ?? []).map((person) => {
                const checked = selectedInterviewers.includes(person.id);
                return (
                  <Checkbox
                    key={person.id}
                    checked={checked}
                    onChange={(next) =>
                      setSelectedInterviewers((current) =>
                        next
                          ? [...current, person.id]
                          : current.filter((id) => id !== person.id),
                      )
                    }
                    label={`${person.name} · ${teamRoleLabel(person.role)}`}
                  />
                );
              })}
            </div>
          </div>
          {scheduleError && <p className="text-sm text-accent">{scheduleError}</p>}
          <p className="text-xs text-text-secondary">
            The candidate and every interviewer get an in-app notification and an email.
          </p>
          <div className="flex justify-end gap-2">
            <Button variant="secondary" onClick={() => setScheduleOpen(false)}>
              Cancel
            </Button>
            <Button loading={schedule.isPending} onClick={confirmSchedule}>
              Schedule
            </Button>
          </div>
        </div>
      </Dialog>

      <Dialog open={hireOpen} title="Mark candidate as hired" onClose={() => setHireOpen(false)}>
        <p className="text-sm text-text">
          This moves the application to <strong>hired</strong> and notifies the candidate. The
          status is final.
        </p>
        <div className="mt-5 flex justify-end gap-2">
          <Button variant="secondary" onClick={() => setHireOpen(false)}>
            Cancel
          </Button>
          <Button loading={hire.isPending} onClick={confirmHire}>
            Mark hired
          </Button>
        </div>
      </Dialog>
    </div>
  );
}
