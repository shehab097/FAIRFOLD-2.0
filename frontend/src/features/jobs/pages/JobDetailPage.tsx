import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { Building2, Link2, MapPin } from 'lucide-react';
import { useJob } from '../queries';
import { useApply } from '../../candidate/queries';
import { useAuth } from '../../../app/auth/AuthContext';
import { Banner } from '../../../components/ui/Banner';
import { Button } from '../../../components/ui/Button';
import { CompanyLogo } from '../../../components/ui/CompanyLogo';
import { Dialog } from '../../../components/ui/Dialog';
import { Icon } from '../../../components/ui/Icon';
import { Markdown } from '../../../components/ui/Markdown';
import { Skeleton } from '../../../components/ui/Skeleton';
import { useToast } from '../../../components/ui/Toast';
import { getApiErrorCode, getApiErrorMessage } from '../../../lib/errors';
import { formatDate } from '../../../lib/format';
import { formatSalary } from '../../../lib/salary';

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
        <Link to="/jobs" className="text-primary-text hover:underline">
          Back to jobs
        </Link>
      </Banner>
    );
  }

  return (
    <div className="space-y-6">
      <div>
        <Link to="/jobs" className="text-sm text-text-secondary hover:text-primary-text">
          ← Browse jobs
        </Link>
        <div className="mt-2 flex items-start gap-3">
          <CompanyLogo name={job.companyName} logoUrl={job.companyLogoUrl} size="lg" />
          <div className="min-w-0">
            <h1 className="text-[28px] font-bold leading-9">{job.title}</h1>
            <div className="mt-1.5 flex flex-wrap items-center gap-3 text-sm text-text-secondary">
              <span className="inline-flex items-center gap-1.5">
                <Icon icon={Building2} size={15} />
                {job.companyName ?? 'Confidential company'}
              </span>
              <span>·</span>
              <span className="inline-flex items-center gap-1.5">
                <Icon icon={MapPin} size={15} />
                {job.address ? `${job.address}, ` : ''}
                {job.city ? `${job.city}, ` : ''}
                {job.country}
              </span>
              <span>·</span>
              <span>{job.remote ? 'Remote' : 'On-site'}</span>
              <span>·</span>
              <span className="capitalize">{job.experienceLevel}</span>
            </div>
          </div>
        </div>
      </div>

      <div className="grid gap-8 lg:grid-cols-[minmax(0,1fr)_280px]">
        <div className="divide-y divide-border rounded-xl border border-border bg-surface p-6 shadow-sm sm:p-8">
          <section className="pb-6">
            <h2 className="mb-3 text-lg font-semibold">About the role</h2>
            <Markdown source={job.description} />
          </section>

          {job.requirements && (
            <section className="py-6">
              <h2 className="mb-3 text-lg font-semibold">Requirements</h2>
              <Markdown source={job.requirements} />
            </section>
          )}

          <section className="pt-6">
            <h2 className="mb-3 text-lg font-semibold">Skills</h2>
            <div className="space-y-3">
              <div>
                <div className="mb-1.5 text-xs text-text-secondary">Required</div>
                <div className="flex flex-wrap gap-2">
                  {job.requiredSkills.length === 0 && <span className="text-sm text-text-subtle">None listed</span>}
                  {job.requiredSkills.map((s) => (
                    <span key={s} className="rounded-md bg-raised px-2.5 py-1 text-xs font-medium text-text">
                      {s}
                    </span>
                  ))}
                </div>
              </div>
              {job.niceToHaveSkills.length > 0 && (
                <div>
                  <div className="mb-1.5 text-xs text-text-secondary">Nice to have</div>
                  <div className="flex flex-wrap gap-2">
                    {job.niceToHaveSkills.map((s) => (
                      <span key={s} className="rounded-md bg-bg px-2.5 py-1 text-xs text-text-secondary">
                        {s}
                      </span>
                    ))}
                  </div>
                </div>
              )}
            </div>
          </section>
        </div>

        <aside className="lg:sticky lg:top-20 lg:self-start">
          <div className="space-y-4 rounded-xl border border-border bg-surface p-5 shadow-sm">
            <dl className="space-y-2.5 text-sm">
              <div className="flex justify-between">
                <dt className="text-text-secondary">Company</dt>
                <dd className="font-medium text-text">{job.companyName ?? 'Confidential'}</dd>
              </div>
              <div className="flex justify-between">
                <dt className="text-text-secondary">Location</dt>
                <dd className="max-w-[65%] break-words text-right font-medium text-text">
                  {job.address ? `${job.address}, ` : ''}
                  {job.city ? `${job.city}, ` : ''}
                  {job.country}
                </dd>
              </div>
              <div className="flex justify-between gap-4">
                <dt className="shrink-0 text-text-secondary">Salary</dt>
                <dd className="text-right font-medium text-text">{formatSalary(job)}</dd>
              </div>
              <div className="flex justify-between">
                <dt className="text-text-secondary">Work mode</dt>
                <dd className="font-medium text-text">{job.remote ? 'Remote' : 'On-site'}</dd>
              </div>
              <div className="flex justify-between">
                <dt className="text-text-secondary">Level</dt>
                <dd className="font-medium capitalize text-text">{job.experienceLevel}</dd>
              </div>
              <div className="flex justify-between">
                <dt className="text-text-secondary">Posted</dt>
                <dd className="font-medium text-text">{formatDate(job.publishedAt)}</dd>
              </div>
            </dl>

            <div className="space-y-2 border-t border-border pt-4">
              {user?.role === 'CANDIDATE' ? (
                <Button className="w-full" onClick={() => setConfirming(true)}>
                  Apply now
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
