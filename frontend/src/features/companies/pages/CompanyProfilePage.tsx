import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { Briefcase, Building2, Calendar, Globe, Linkedin, MapPin, Users } from 'lucide-react';
import { usePublicCompany } from '../queries';
import { useJobs } from '../../jobs/queries';
import { EXPERIENCE_LABELS } from '../../jobs/types';
import { Banner } from '../../../components/ui/Banner';
import { Button } from '../../../components/ui/Button';
import { CompanyLogo } from '../../../components/ui/CompanyLogo';
import { Icon } from '../../../components/ui/Icon';
import { Markdown } from '../../../components/ui/Markdown';
import { Skeleton } from '../../../components/ui/Skeleton';
import { formatDate } from '../../../lib/format';
import { formatSalary } from '../../../lib/salary';

export function CompanyProfilePage() {
  const { companyId } = useParams<{ companyId: string }>();
  const { data: company, isLoading, isError } = usePublicCompany(companyId);
  const [page, setPage] = useState(0);
  const { data: jobs, isLoading: jobsLoading } = useJobs({ companyId, page, size: 10 });

  if (isLoading) return <Skeleton className="h-64 w-full" />;

  if (isError || !company) {
    return (
      <Banner tone="error">
        Company not found or no longer public.{' '}
        <Link to="/jobs" className="text-primary-text hover:underline">
          Back to jobs
        </Link>
      </Banner>
    );
  }

  const totalPages = jobs?.totalPages ?? 0;
  const jobCount = jobs?.totalItems ?? 0;

  return (
    <div className="space-y-6">
      <Link to="/jobs" className="text-sm text-text-secondary hover:text-primary-text">
        ← Browse jobs
      </Link>

      <section className="rounded-xl border border-border bg-surface p-6 shadow-sm sm:p-8">
        <div className="flex flex-col gap-5 sm:flex-row sm:items-start">
          <CompanyLogo name={company.name} logoUrl={company.logoUrl} size="xl" />
          <div className="min-w-0 flex-1">
            <h1 className="text-[28px] font-bold leading-9">{company.name ?? 'Company'}</h1>
            {company.tagline && <p className="mt-1 text-text-secondary">{company.tagline}</p>}

            <div className="mt-4 flex flex-wrap items-center gap-x-5 gap-y-2 text-sm text-text-secondary">
              {company.industry && (
                <span className="inline-flex items-center gap-1.5">
                  <Icon icon={Building2} size={15} />
                  {company.industry}
                </span>
              )}
              {company.size && (
                <span className="inline-flex items-center gap-1.5">
                  <Icon icon={Users} size={15} />
                  {company.size} employees
                </span>
              )}
              {company.headquarters && (
                <span className="inline-flex items-center gap-1.5">
                  <Icon icon={MapPin} size={15} />
                  {company.headquarters}
                </span>
              )}
              {company.foundedYear && (
                <span className="inline-flex items-center gap-1.5">
                  <Icon icon={Calendar} size={15} />
                  Founded {company.foundedYear}
                </span>
              )}
              {company.website && (
                <a
                  href={company.website}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="inline-flex items-center gap-1.5 text-primary-text hover:underline"
                >
                  <Icon icon={Globe} size={15} />
                  Website
                </a>
              )}
              {company.linkedinUrl && (
                <a
                  href={company.linkedinUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="inline-flex items-center gap-1.5 text-primary-text hover:underline"
                >
                  <Icon icon={Linkedin} size={15} />
                  LinkedIn
                </a>
              )}
            </div>
          </div>
        </div>
      </section>

      {(company.about || company.benefits) && (
        <section className="grid gap-6 lg:grid-cols-2">
          {company.about && (
            <div className="rounded-xl border border-border bg-surface p-6 shadow-sm">
              <h2 className="mb-3 text-lg font-semibold">About</h2>
              <Markdown source={company.about} />
            </div>
          )}
          {company.benefits && (
            <div className="rounded-xl border border-border bg-surface p-6 shadow-sm">
              <h2 className="mb-3 text-lg font-semibold">Benefits and perks</h2>
              <Markdown source={company.benefits} />
            </div>
          )}
        </section>
      )}

      <section>
        <div className="mb-3 flex flex-wrap items-end justify-between gap-2">
          <div>
            <h2 className="text-xl font-semibold">Open jobs</h2>
            <p className="text-sm text-text-secondary">
              Roles currently hiring at {company.name ?? 'this company'}.
            </p>
          </div>
          {jobs && (
            <span className="text-sm text-text-secondary">
              {jobCount} open role{jobCount === 1 ? '' : 's'}
            </span>
          )}
        </div>

        {jobsLoading && <Skeleton className="h-40 w-full" />}

        {!jobsLoading && jobs && jobs.items.length === 0 && (
          <div className="rounded-xl border border-border bg-surface p-8 text-center text-sm text-text-secondary shadow-sm">
            No open jobs right now.{' '}
            <Link to="/jobs" className="text-primary-text hover:underline">
              Browse all jobs
            </Link>
          </div>
        )}

        {!jobsLoading && jobs && jobs.items.length > 0 && (
          <div className="divide-y divide-border overflow-hidden rounded-xl border border-border bg-surface shadow-sm">
            {jobs.items.map((job) => (
              <Link key={job.id} to={`/jobs/${job.id}`} className="block p-5 transition-colors hover:bg-raised">
                <div className="flex flex-col justify-between gap-2 sm:flex-row sm:items-start">
                  <div className="min-w-0">
                    <h3 className="text-base font-semibold text-text">{job.title}</h3>
                    <div className="mt-1 flex flex-wrap items-center gap-3 text-sm text-text-secondary">
                      <span className="inline-flex items-center gap-1.5">
                        <Icon icon={MapPin} size={14} />
                        {job.city ? `${job.city}, ` : ''}
                        {job.country}
                      </span>
                      <span className="inline-flex items-center gap-1.5">
                        <Icon icon={Briefcase} size={14} />
                        {job.remote ? 'Remote' : 'On-site'}
                      </span>
                      <span>{EXPERIENCE_LABELS[job.experienceLevel]}</span>
                    </div>
                  </div>
                  <span className="shrink-0 text-xs text-text-subtle">{formatDate(job.publishedAt)}</span>
                </div>
                <div className="mt-3 flex flex-wrap items-center gap-2">
                  <span className="rounded-md bg-success-subtle px-2.5 py-0.5 text-xs font-medium text-success">
                    {formatSalary(job)}
                  </span>
                  {job.topSkills.slice(0, 2).map((skill) => (
                    <span key={skill} className="rounded-md bg-raised px-2.5 py-0.5 text-xs text-text-secondary">
                      {skill}
                    </span>
                  ))}
                </div>
              </Link>
            ))}
          </div>
        )}

        {totalPages > 1 && (
          <div className="mt-4 flex items-center justify-between">
            <Button variant="secondary" disabled={page === 0} onClick={() => setPage(page - 1)}>
              Previous
            </Button>
            <span className="text-sm text-text-secondary">
              Page {page + 1} of {totalPages}
            </span>
            <Button variant="secondary" disabled={page >= totalPages - 1} onClick={() => setPage(page + 1)}>
              Next
            </Button>
          </div>
        )}
      </section>
    </div>
  );
}
