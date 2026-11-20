import { Link, useParams } from 'react-router-dom';
import { useJob } from '../queries';
import { useAuth } from '../../../app/auth/AuthContext';
import { Banner } from '../../../components/ui/Banner';
import { Button } from '../../../components/ui/Button';
import { Skeleton } from '../../../components/ui/Skeleton';
import { useToast } from '../../../components/ui/Toast';
import { formatDate } from '../../../lib/format';

export function JobDetailPage() {
  const { jobId } = useParams<{ jobId: string }>();
  const { data: job, isLoading, isError } = useJob(jobId);
  const { user } = useAuth();
  const { toast } = useToast();

  function copyLink() {
    navigator.clipboard.writeText(window.location.href);
    toast('Link copied.');
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
          <p className="mt-2 whitespace-pre-line text-text">{job.description}</p>
        </section>

        {job.requirements && (
          <section className="mt-6">
            <h2 className="text-xl font-semibold">Requirements</h2>
            <p className="mt-2 whitespace-pre-line text-text">{job.requirements}</p>
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
              <Button className="w-full" onClick={() => toast('Applying opens in Part 1.3.')}>
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
    </div>
  );
}
