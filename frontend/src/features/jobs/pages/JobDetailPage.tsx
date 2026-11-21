import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { useJob } from '../queries';
import { useApply } from '../../candidate/queries';
import { useAuth } from '../../../app/auth/AuthContext';
import { Markdown } from '../../../components/ui/Markdown';
import { Banner } from '../../../components/ui/Banner';
import { Button } from '../../../components/ui/Button';
import { Dialog } from '../../../components/ui/Dialog';
import { Skeleton } from '../../../components/ui/Skeleton';
import { useToast } from '../../../components/ui/Toast';
import { getApiErrorCode, getApiErrorMessage } from '../../../lib/errors';
import { formatDate } from '../../../lib/format';

export function JobDetailPage() {
  const { jobId } = useParams<{ jobId: string }>();
  const { data: job, isLoading, isError } = useJob(jobId);
  const { user } = useAuth();
  const { toast } = useToast();
  const navigate = useNavigate();
  const apply = useApply(jobId as string);
  const [confirming, setConfirming] = useState(false);

  function copyLink() {
    navigator.clipboard.writeText(window.location.href);
    toast('Link copied.');
  }

  async function confirmApply() {
    try {
      const application = await apply.mutateAsync();
      setConfirming(false);
      toast('Application submitted.', 'success');
      navigate(`/candidate/applications/${application.id}`);
    } catch (err) {
      setConfirming(false);
      const code = getApiErrorCode(err);
      if (code === 'RESUME_REQUIRED') {
        toast('Upload a resume before applying.', 'error');
        navigate('/candidate/resume');
      } else if (code === 'ALREADY_APPLIED') {
        toast('You have already applied to this job.', 'info');
        navigate('/candidate/applications');
      } else if (code === 'JOB_NOT_ACTIVE') {
        toast('This job is no longer accepting applications.', 'error');
      } else {
        toast(getApiErrorMessage(err), 'error');
      }
    }
  }

  if (isLoading) {
    return <Skeleton className="h-64 w-full" />;
  }

  if (isError || !job) {
    return (
      <Banner tone="error">
        Job not found or no longer available.{' '}
        <Link to="/" className="text-primary-text hover:underline">
          Back to jobs
        </Link>
      </Banner>
    );
  }

  return (
    <div className="grid gap-6 lg:grid-cols-3">
      <div className="lg:col-span-2">
        <h1 className="text-[28px] font-bold leading-9">{job.title}</h1>
        <p className="mt-1 text-text-secondary">
          {job.companyName ?? 'Confidential company'} · {job.city ? `${job.city}, ` : ''}
          {job.country} · {job.remote ? 'Remote' : 'On-site'} · {job.experienceLevel}
        </p>

        <section className="mt-6">
          <h2 className="text-xl font-semibold">Description</h2>
          <Markdown source={job.description} className="mt-2" />
        </section>

        {job.requirements && (
          <section className="mt-6">
            <h2 className="text-xl font-semibold">Requirements</h2>
            <Markdown source={job.requirements} className="mt-2" />
          </section>
        )}

        <section className="mt-6">
          <h2 className="text-xl font-semibold">Skills</h2>
          <div className="mt-3 space-y-4">
            <div>
              <h3 className="mb-2 text-xs font-semibold uppercase tracking-[0.06em] text-text-secondary">Required</h3>
              <div className="flex flex-wrap gap-2">
                {job.requiredSkills.length === 0 && <span className="text-sm text-text-subtle">None listed</span>}
                {job.requiredSkills.map((s) => (
                  <span key={s} className="border border-border bg-raised px-2 py-1 text-sm text-text">{s}</span>
                ))}
              </div>
            </div>
            {job.niceToHaveSkills.length > 0 && (
              <div>
                <h3 className="mb-2 text-xs font-semibold uppercase tracking-[0.06em] text-text-secondary">Nice to have</h3>
                <div className="flex flex-wrap gap-2">
                  {job.niceToHaveSkills.map((s) => (
                    <span key={s} className="border border-border bg-raised px-2 py-1 text-sm text-text-secondary">{s}</span>
                  ))}
                </div>
              </div>
            )}
          </div>
        </section>
      </div>

      <aside className="lg:sticky lg:top-6 lg:self-start">
        <div className="border border-border bg-surface p-6">
          <p className="text-sm text-text-secondary">Posted {formatDate(job.publishedAt)}</p>
          <div className="mt-4 space-y-2">
            <Button variant="secondary" onClick={copyLink} className="w-full">
              Copy link
            </Button>
            {user?.role === 'CANDIDATE' ? (
              <Button className="w-full" onClick={() => setConfirming(true)}>
                Apply
              </Button>
            ) : !user ? (
              <Button className="w-full" onClick={() => window.location.assign(`/login?next=${encodeURIComponent(window.location.pathname)}`)}>
                Log in to apply
              </Button>
            ) : null}
          </div>
        </div>
      </aside>

      <Dialog open={confirming} title="Apply to this job" onClose={() => setConfirming(false)}>
        <p className="text-text">
          Apply to <span className="font-semibold">{job.title}</span> with your current resume?
        </p>
        <div className="mt-6 flex justify-end gap-3">
          <Button variant="secondary" onClick={() => setConfirming(false)}>
            Cancel
          </Button>
          <Button loading={apply.isPending} onClick={confirmApply}>
            Confirm
          </Button>
        </div>
      </Dialog>
    </div>
  );
}
