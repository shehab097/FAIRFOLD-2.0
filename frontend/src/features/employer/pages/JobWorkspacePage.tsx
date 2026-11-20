import { useState } from 'react';
import { useParams } from 'react-router-dom';
import { useEmployerJob, useJobAction } from '../queries';
import type { JobAction } from '../../../api/employerJobs';
import { useAuth } from '../../../app/auth/AuthContext';
import { Badge } from '../../../components/ui/Badge';
import { Banner } from '../../../components/ui/Banner';
import { Button } from '../../../components/ui/Button';
import { Skeleton } from '../../../components/ui/Skeleton';
import { Tabs } from '../../../components/ui/Tabs';
import { useToast } from '../../../components/ui/Toast';
import { getApiErrorMessage } from '../../../lib/errors';
import { formatDate } from '../../../lib/format';
import type { JobStatus } from '../../jobs/types';

function actionsFor(status: JobStatus): { label: string; value: JobAction }[] {
  switch (status) {
    case 'draft':
      return [{ label: 'Activate', value: 'activate' }];
    case 'active':
      return [
        { label: 'Pause', value: 'pause' },
        { label: 'Close', value: 'close' },
      ];
    case 'paused':
      return [
        { label: 'Resume', value: 'resume' },
        { label: 'Close', value: 'close' },
      ];
    case 'closed':
      return [];
  }
}

export function JobWorkspacePage() {
  const { jobId } = useParams<{ jobId: string }>();
  const { data: job, isLoading, isError } = useEmployerJob(jobId);
  const jobAction = useJobAction();
  const { user } = useAuth();
  const { toast } = useToast();
  const [tab, setTab] = useState('applicants');
  const [busy, setBusy] = useState<string | null>(null);

  const isHr = user?.role === 'EMPLOYER_HR';

  async function run(action: JobAction) {
    setBusy(action);
    try {
      await jobAction.mutateAsync({ id: jobId as string, action });
      toast('Job updated.', 'success');
    } catch (err) {
      toast(getApiErrorMessage(err), 'error');
    } finally {
      setBusy(null);
    }
  }

  function copyLink() {
    navigator.clipboard.writeText(`${window.location.origin}/jobs/${jobId}`);
    toast('Public link copied.');
  }

  if (isLoading) {
    return <Skeleton className="h-64 w-full" />;
  }
  if (isError || !job) {
    return <Banner tone="error">Job not found.</Banner>;
  }

  return (
    <div>
      <div className="mb-4 flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <div className="flex items-center gap-3">
            <h1 className="text-[28px] font-bold leading-9">{job.title}</h1>
            <Badge status={job.status} />
          </div>
          <p className="text-text-secondary">
            {job.city ? `${job.city}, ` : ''}
            {job.country} · {job.remote ? 'Remote' : 'On-site'} · {job.experienceLevel}
          </p>
          {job.publishedAt && <p className="text-xs text-text-subtle">Published {formatDate(job.publishedAt)}</p>}
        </div>
        <div className="flex flex-wrap gap-2">
          <Button variant="secondary" onClick={copyLink}>
            Copy public link
          </Button>
          {isHr && job.status === 'draft' && (
            <Button variant="secondary" onClick={() => window.location.assign(`/employer/jobs/${job.id}/edit`)}>
              Edit
            </Button>
          )}
          {isHr &&
            actionsFor(job.status).map((a) => (
              <Button
                key={a.value}
                variant={a.value === 'close' ? 'danger' : 'secondary'}
                loading={busy === a.value}
                onClick={() => run(a.value)}
              >
                {a.label}
              </Button>
            ))}
        </div>
      </div>

      <Tabs
        tabs={[
          { key: 'applicants', label: 'Applicants' },
          { key: 'details', label: 'Job details' },
        ]}
        active={tab}
        onChange={setTab}
      />

      {tab === 'applicants' && (
        <div className="mt-4 border border-border bg-surface p-8 text-center text-text-secondary">
          No applicants yet.
        </div>
      )}

      {tab === 'details' && (
        <div className="mt-4 space-y-6 border border-border bg-surface p-6">
          <section>
            <h2 className="mb-2 text-xl font-semibold">Description</h2>
            <p className="whitespace-pre-line text-text">{job.description}</p>
          </section>
          {job.requirements && (
            <section>
              <h2 className="mb-2 text-xl font-semibold">Requirements</h2>
              <p className="whitespace-pre-line text-text">{job.requirements}</p>
            </section>
          )}
          <section>
            <h2 className="mb-2 text-xl font-semibold">Skills</h2>
            <div className="mb-3">
              <h3 className="mb-1 text-xs font-semibold uppercase tracking-[0.06em] text-text-secondary">Required</h3>
              <div className="flex flex-wrap gap-2">
                {job.requiredSkills.length === 0 && <span className="text-sm text-text-subtle">None</span>}
                {job.requiredSkills.map((s) => (
                  <span key={s.id} className="border border-border bg-raised px-2 py-1 text-sm text-text">
                    {s.name}
                    {s.mustHave && <span className="ml-1 text-xs text-accent">(must)</span>}
                  </span>
                ))}
              </div>
            </div>
            {job.niceToHaveSkills.length > 0 && (
              <div>
                <h3 className="mb-1 text-xs font-semibold uppercase tracking-[0.06em] text-text-secondary">Nice to have</h3>
                <div className="flex flex-wrap gap-2">
                  {job.niceToHaveSkills.map((s) => (
                    <span key={s.id} className="border border-border bg-raised px-2 py-1 text-sm text-text-secondary">
                      {s.name}
                    </span>
                  ))}
                </div>
              </div>
            )}
          </section>
          {(job.hfMinYears !== null || job.hfCountry || job.cutoffScore !== null) && (
            <section>
              <h2 className="mb-2 text-xl font-semibold">Hard filters and cut-off</h2>
              <div className="flex flex-wrap gap-4 text-sm text-text">
                {job.hfMinYears !== null && <span>Minimum years: {job.hfMinYears}</span>}
                {job.hfCountry && <span>Required country: {job.hfCountry}</span>}
                {job.cutoffScore !== null && <span>Cut-off score: {job.cutoffScore}</span>}
              </div>
            </section>
          )}
        </div>
      )}
    </div>
  );
}
