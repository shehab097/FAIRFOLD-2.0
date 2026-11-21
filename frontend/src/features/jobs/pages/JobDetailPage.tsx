import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { Building2, Link2, MapPin } from 'lucide-react';
import { useJob } from '../queries';
import { useApply } from '../../candidate/queries';
import { useAuth } from '../../../app/auth/AuthContext';
import { Banner } from '../../../components/ui/Banner';
import { Button } from '../../../components/ui/Button';
import { Dialog } from '../../../components/ui/Dialog';
import { Icon } from '../../../components/ui/Icon';
import { Markdown } from '../../../components/ui/Markdown';
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
    <div className="space-y-6">
      <div className="border-b border-border pb-4">
        <Link to="/" className="text-xs font-semibold uppercase tracking-[0.06em] text-primary-text hover:underline">
          ← All open roles
        </Link>
        <h1 className="mt-2 text-[28px] font-bold leading-9">{job.title}</h1>
        <div className="mt-2 flex flex-wrap items-center gap-3 text-sm text-text-secondary">
          <span className="inline-flex items-center gap-1.5">
            <Icon icon={Building2} size={15} />
            {job.companyName ?? 'Confidential company'}
          </span>
          <span>·</span>
          <span className="inline-flex items-center gap-1.5">
            <Icon icon={MapPin} size={15} />
            {job.city ? `${job.city}, ` : ''}
            {job.country}
          </span>
          <span>·</span>
          <span className="border border-border bg-raised px-2 py-0.5 text-xs font-medium text-text">
            {job.remote ? 'Remote' : 'On-site'}
          </span>
          <span className="border border-border bg-primary-subtle px-2 py-0.5 text-xs font-semibold uppercase tracking-[0.06em] text-primary-text">
            {job.experienceLevel}
          </span>
        </div>
      </div>

      <div className="grid gap-6 lg:grid-cols-3">
        <div className="space-y-6 lg:col-span-2">
          <section className="border border-border bg-surface">
            <div className="border-b border-border bg-raised px-6 py-3.5">
              <h2 className="text-base font-semibold">Description</h2>
            </div>
            <div className="p-6">
              <Markdown source={job.description} />
            </div>
          </section>

          {job.requirements && (
            <section className="border border-border bg-surface">
              <div className="border-b border-border bg-raised px-6 py-3.5">
                <h2 className="text-base font-semibold">Requirements</h2>
              </div>
              <div className="p-6">
                <Markdown source={job.requirements} />
              </div>
            </section>
          )}

          <section className="border border-border bg-surface">
            <div className="border-b border-border bg-raised px-6 py-3.5">
              <h2 className="text-base font-semibold">Skills</h2>
            </div>
            <div className="space-y-4 p-6">
              <div>
                <h3 className="mb-2 text-xs font-semibold uppercase tracking-[0.06em] text-text-secondary">Required</h3>
                <div className="flex flex-wrap gap-2">
                  {job.requiredSkills.length === 0 && <span className="text-sm text-text-subtle">None listed</span>}
                  {job.requiredSkills.map((s) => (
                    <span key={s} className="border border-border bg-raised px-2.5 py-1 text-sm text-text">
                      {s}
                    </span>
                  ))}
                </div>
              </div>
              {job.niceToHaveSkills.length > 0 && (
                <div>
                  <h3 className="mb-2 text-xs font-semibold uppercase tracking-[0.06em] text-text-secondary">Nice to have</h3>
                  <div className="flex flex-wrap gap-2">
                    {job.niceToHaveSkills.map((s) => (
                      <span key={s} className="border border-border bg-bg px-2.5 py-1 text-sm text-text-secondary">
                        {s}
                      </span>
                    ))}
                  </div>
                </div>
              )}
            </div>
          </section>
        </div>

        <aside className="lg:sticky lg:top-6 lg:self-start">
          <div className="border border-border border-t-2 border-t-primary bg-surface">
            <div className="border-b border-border bg-raised px-6 py-3.5">
              <h2 className="text-xs font-semibold uppercase tracking-[0.06em] text-text-secondary">Role summary</h2>
            </div>
            <div className="space-y-4 p-6">
              <dl className="divide-y divide-border text-sm">
                <div className="flex justify-between py-2">
                  <dt className="text-text-secondary">Company</dt>
                  <dd className="font-medium text-text">{job.companyName ?? 'Confidential'}</dd>
                </div>
                <div className="flex justify-between py-2">
                  <dt className="text-text-secondary">Location</dt>
                  <dd className="font-medium text-text">
                    {job.city ? `${job.city}, ` : ''}
                    {job.country}
                  </dd>
                </div>
                <div className="flex justify-between py-2">
                  <dt className="text-text-secondary">Work mode</dt>
                  <dd className="font-medium text-text">{job.remote ? 'Remote' : 'On-site'}</dd>
                </div>
                <div className="flex justify-between py-2">
                  <dt className="text-text-secondary">Experience</dt>
                  <dd className="font-medium capitalize text-text">{job.experienceLevel}</dd>
                </div>
                <div className="flex justify-between py-2">
                  <dt className="text-text-secondary">Posted</dt>
                  <dd className="font-medium text-text">{formatDate(job.publishedAt)}</dd>
                </div>
              </dl>

              <div className="space-y-2 pt-2">
                {user?.role === 'CANDIDATE' ? (
                  <Button className="w-full" onClick={() => setConfirming(true)}>
                    Apply
                  </Button>
                ) : !user ? (
                  <Button className="w-full" onClick={() => navigate(`/login?next=${encodeURIComponent(window.location.pathname)}`)}>
                    Log in to apply
                  </Button>
                ) : null}
                <Button variant="secondary" onClick={copyLink} className="w-full">
                  <Icon icon={Link2} size={15} />
                  Copy link
                </Button>
              </div>
            </div>
          </div>
        </aside>
      </div>

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
