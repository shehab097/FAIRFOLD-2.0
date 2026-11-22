import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { TriangleAlert } from 'lucide-react';
import {
  useEmployerReview,
  usePullBackApplication,
  useRejectApplication,
  useShortlistApplication,
} from '../queries';
import { StrippedResume } from '../components/StrippedResume';
import type { EmployerIdentity } from '../../jobs/types';
import { Badge } from '../../../components/ui/Badge';
import { Banner } from '../../../components/ui/Banner';
import { Button } from '../../../components/ui/Button';
import { Dialog } from '../../../components/ui/Dialog';
import { ScoreMeter } from '../../../components/ui/ScoreMeter';
import { Skeleton } from '../../../components/ui/Skeleton';
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

export function ApplicationReviewPage() {
  const { jobId, applicationId } = useParams<{ jobId: string; applicationId: string }>();
  const { data: review, isLoading, isError } = useEmployerReview(applicationId);
  const shortlist = useShortlistApplication();
  const reject = useRejectApplication();
  const pullBack = usePullBackApplication();
  const { toast } = useToast();
  const [activeClaim, setActiveClaim] = useState<number | null>(null);
  const [shortlistOpen, setShortlistOpen] = useState(false);
  const [rejectOpen, setRejectOpen] = useState(false);
  const [reason, setReason] = useState('');
  const [identity, setIdentity] = useState<EmployerIdentity | null>(null);

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
  const canReject = review.status === 'screened' || review.status === 'not_matched';
  const canPullBack = review.status === 'not_matched';
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
    if ((aboveCutoff || reason.trim().length > 0) && reason.trim().length < 10) {
      toast('Write a reason of at least 10 characters.', 'error');
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
          {aboveCutoff && (
            <div className="rounded-lg bg-accent-subtle p-3 text-sm text-accent">
              This score ({review.score}) is at or above the cut-off ({review.cutoffScore}). A written
              reason of at least 10 characters is required.
            </div>
          )}
          <div>
            <label className="mb-1 block text-xs text-text-secondary">
              {aboveCutoff ? 'Reason (required)' : 'Reason (optional)'}
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
    </div>
  );
}
