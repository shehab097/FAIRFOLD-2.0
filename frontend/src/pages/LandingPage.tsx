import { Link } from 'react-router-dom';
import {
  ArrowRight,
  BarChart3,
  Briefcase,
  Building2,
  Check,
  CircleDollarSign,
  FileText,
  LayoutDashboard,
  MapPin,
  Search,
  ShieldCheck,
  Sparkles,
  Users,
  type LucideIcon,
} from 'lucide-react';
import { useJobs } from '../features/jobs/queries';
import { EXPERIENCE_LABELS } from '../features/jobs/types';
import { Button } from '../components/ui/Button';
import { CompanyLogo } from '../components/ui/CompanyLogo';
import { Icon } from '../components/ui/Icon';
import { Skeleton } from '../components/ui/Skeleton';
import { formatSalary } from '../lib/salary';

const FEATURES: { icon: LucideIcon; title: string; description: string }[] = [
  {
    icon: ShieldCheck,
    title: 'Anonymised by default',
    description:
      'Names, contact details and identity signals are hidden during screening. Candidates are judged on their experience, not their background.',
  },
  {
    icon: BarChart3,
    title: 'Scores you can explain',
    description:
      'Every match comes with quoted evidence and clearly listed missing skills. No black-box rankings, no unexplained rejections.',
  },
  {
    icon: Users,
    title: 'People make the call',
    description:
      'Screening informs the shortlist, but humans decide. Every reveal and override is visible and auditable.',
  },
];

const PRODUCT_FEATURES: { icon: LucideIcon; title: string; description: string }[] = [
  {
    icon: Search,
    title: 'Search that respects your time',
    description: 'Find roles by keyword or skill, country, salary range, remote/on-site and experience level.',
  },
  {
    icon: CircleDollarSign,
    title: 'Salary transparency',
    description: 'Every job shows a fixed salary, a range or negotiable — with currency and pay period.',
  },
  {
    icon: Building2,
    title: 'Company profiles',
    description: 'Open the company behind a role, read their story and see every active job they have.',
  },
  {
    icon: FileText,
    title: 'Candidate workspace',
    description: 'Build a profile from your resume, add skills, upload a photo and track every application.',
  },
  {
    icon: LayoutDashboard,
    title: 'Employer workspace',
    description: 'Manage your company profile, post roles and review applicants from one dashboard.',
  },
  {
    icon: ShieldCheck,
    title: 'Fairness built in',
    description: 'Applicant lists are anonymised by default; identity is never sent with the initial review.',
  },
];

const CANDIDATE_STEPS = [
  {
    title: 'Build your profile',
    description: 'Import your resume, add skills and set your location and preferences.',
  },
  {
    title: 'Apply privately',
    description: 'Send your profile with an uploaded resume; identity stays out of the initial review.',
  },
  {
    title: 'Track every step',
    description: 'Follow application statuses and updates from your candidate dashboard.',
  },
];

const EMPLOYER_STEPS = [
  {
    title: 'Set up your company',
    description: 'Add your company story, logo, links and headquarters.',
  },
  {
    title: 'Post a role',
    description: 'Publish salary, skills, location and screening filters in one job editor.',
  },
  {
    title: 'Review and hire',
    description: 'Work from an anonymised applicant list and make the final call as a team.',
  },
];

export function LandingPage() {
  const { data: jobs, isLoading: jobsLoading } = useJobs({ size: 3 });

  return (
    <div className="pb-4">
      {/* Hero */}
      <section className="relative overflow-hidden rounded-2xl border border-border bg-surface p-6 shadow-sm sm:p-10 lg:p-12">
        <div className="grid gap-10 lg:grid-cols-[1.08fr_0.92fr] lg:items-center">
          <div>
            <span className="inline-flex items-center gap-2 rounded-full bg-primary-subtle px-3 py-1 text-xs font-semibold uppercase tracking-[0.08em] text-primary-text">
              <Icon icon={Sparkles} size={14} />
              Evidence-based hiring
            </span>
            <h1 className="mt-5 text-[36px] font-bold leading-[1.08] tracking-tight sm:text-[48px]">
              Hire on evidence,
              <br />
              <span className="text-primary-text">not assumptions.</span>
            </h1>
            <p className="mt-5 max-w-xl text-base text-text-secondary sm:text-lg">
              FairFold anonymises applications, explains every match with quoted evidence, and keeps people in control of the final decision.
            </p>
            <div className="mt-7 flex flex-wrap gap-3">
              <Link to="/jobs">
                <Button>
                  Browse open roles
                  <Icon icon={ArrowRight} size={16} />
                </Button>
              </Link>
              <Link to="/register">
                <Button variant="secondary">Create an account</Button>
              </Link>
            </div>
            <div className="mt-6 flex flex-wrap gap-x-5 gap-y-2 text-sm text-text-secondary">
              {jobs && (
                <span className="inline-flex items-center gap-1.5">
                  <Icon icon={Briefcase} size={15} className="text-primary-text" />
                  {jobs.totalItems} open role{jobs.totalItems === 1 ? '' : 's'}
                </span>
              )}
              <span className="inline-flex items-center gap-1.5">
                <Icon icon={Check} size={15} className="text-success" />
                Identity hidden
              </span>
              <span className="inline-flex items-center gap-1.5">
                <Icon icon={Check} size={15} className="text-success" />
                Every score explained
              </span>
              <span className="inline-flex items-center gap-1.5">
                <Icon icon={Check} size={15} className="text-success" />
                Human decisions
              </span>
            </div>
          </div>

          <div className="relative mx-auto w-full max-w-sm lg:max-w-none">
            <div className="rounded-2xl border border-border bg-bg p-5 shadow-lg">
              <div className="flex items-start justify-between gap-4">
                <div>
                  <div className="text-xs font-semibold uppercase tracking-[0.08em] text-text-secondary">
                    Screening result
                  </div>
                  <div className="mt-1 text-sm font-semibold text-text">Senior Backend Engineer</div>
                </div>
                <span className="rounded-full bg-success-subtle px-2.5 py-1 text-sm font-bold text-success">
                  86 / 100
                </span>
              </div>

              <div className="mt-5 h-2 overflow-hidden rounded-full bg-raised">
                <div className="h-full w-[86%] rounded-full bg-primary" />
              </div>

              <div className="mt-5 rounded-lg border-l-4 border-primary bg-primary-subtle p-3">
                <div className="text-xs font-semibold uppercase tracking-[0.06em] text-primary-text">Evidence</div>
                <p className="mt-1 text-sm text-text">
                  “Built a real-time pipeline handling 40k events/sec with Kafka and Flink.”
                </p>
              </div>

              <div className="mt-4 flex flex-wrap gap-2">
                <span className="rounded-md bg-success-subtle px-2 py-1 text-xs font-medium text-success">Kafka</span>
                <span className="rounded-md bg-success-subtle px-2 py-1 text-xs font-medium text-success">Flink</span>
                <span className="rounded-md bg-warning-subtle px-2 py-1 text-xs font-medium text-warning">
                  Missing: Kubernetes
                </span>
              </div>

              <div className="mt-4 flex items-center gap-2 border-t border-border pt-3 text-xs text-text-secondary">
                <Icon icon={ShieldCheck} size={14} />
                Identity hidden · Evidence linked
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Features */}
      <section className="mt-14">
        <div className="max-w-2xl">
          <h2 className="text-[28px] font-bold leading-tight">Fair from first look to final hire.</h2>
          <p className="mt-2 text-text-secondary">
            A hiring workflow designed to reduce bias without making the decision for you.
          </p>
        </div>
        <div className="mt-6 grid gap-4 md:grid-cols-3">
          {FEATURES.map((feature) => (
            <div key={feature.title} className="rounded-xl border border-border bg-surface p-6 shadow-sm">
              <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-primary-subtle text-primary-text">
                <Icon icon={feature.icon} size={20} />
              </div>
              <h3 className="mt-4 text-base font-semibold">{feature.title}</h3>
              <p className="mt-2 text-sm text-text-secondary">{feature.description}</p>
            </div>
          ))}
        </div>
      </section>

      {/* Product */}
      <section className="mt-14">
        <div className="max-w-2xl">
          <span className="inline-flex items-center gap-2 rounded-full bg-primary-subtle px-3 py-1 text-xs font-semibold uppercase tracking-[0.08em] text-primary-text">
            <Icon icon={Sparkles} size={14} />
            Inside FairFold
          </span>
          <h2 className="mt-3 text-[28px] font-bold leading-tight">Everything the hiring journey needs.</h2>
          <p className="mt-2 text-text-secondary">
            From the first search to the final decision, the workflow is built into one place.
          </p>
        </div>
        <div className="mt-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {PRODUCT_FEATURES.map((feature) => (
            <div key={feature.title} className="rounded-xl border border-border bg-surface p-5 shadow-sm">
              <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-primary-subtle text-primary-text">
                <Icon icon={feature.icon} size={18} />
              </div>
              <h3 className="mt-3 text-sm font-semibold">{feature.title}</h3>
              <p className="mt-1.5 text-sm text-text-secondary">{feature.description}</p>
            </div>
          ))}
        </div>
      </section>

      {/* Latest jobs */}
      <section className="mt-14">
        <div className="flex flex-wrap items-end justify-between gap-3">
          <div>
            <h2 className="text-[28px] font-bold leading-tight">Latest opportunities</h2>
            <p className="mt-2 text-text-secondary">Fresh roles from companies hiring on evidence.</p>
          </div>
          <div className="flex flex-wrap items-center gap-3">
            {jobs && (
              <span className="rounded-full bg-success-subtle px-3 py-1 text-xs font-semibold text-success">
                {jobs.totalItems} open role{jobs.totalItems === 1 ? '' : 's'}
              </span>
            )}
            <Link to="/jobs" className="inline-flex items-center gap-1.5 text-sm font-semibold text-primary-text hover:underline">
              Browse all jobs
              <Icon icon={ArrowRight} size={15} />
            </Link>
          </div>
        </div>

        {jobsLoading && (
          <div className="mt-5 space-y-2">
            {[0, 1, 2].map((i) => (
              <Skeleton key={i} className="h-20 w-full" />
            ))}
          </div>
        )}

        {!jobsLoading && jobs && jobs.items.length > 0 && (
          <div className="mt-5 divide-y divide-border overflow-hidden rounded-xl border border-border bg-surface shadow-sm">
            {jobs.items.map((job) => (
              <Link key={job.id} to={`/jobs/${job.id}`} className="block p-4 transition-colors hover:bg-raised sm:p-5">
                <div className="flex items-start gap-3">
                  <CompanyLogo name={job.companyName} logoUrl={job.companyLogoUrl} size="md" />
                  <div className="min-w-0 flex-1">
                    <div className="flex flex-wrap items-center justify-between gap-2">
                      <h3 className="text-base font-semibold text-text">{job.title}</h3>
                      <span className="whitespace-nowrap text-xs text-text-subtle">{formatSalary(job)}</span>
                    </div>
                    <div className="mt-1 flex flex-wrap items-center gap-3 text-sm text-text-secondary">
                      <span className="inline-flex items-center gap-1.5">
                        <Icon icon={Building2} size={14} />
                        {job.companyName ?? 'Confidential company'}
                      </span>
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
                </div>
              </Link>
            ))}
          </div>
        )}

        {!jobsLoading && (!jobs || jobs.items.length === 0) && (
          <div className="mt-5 rounded-xl border border-border bg-surface p-8 text-center text-sm text-text-secondary shadow-sm">
            No open roles right now.{' '}
            <Link to="/jobs" className="text-primary-text hover:underline">
              Check back soon
            </Link>
          </div>
        )}
      </section>

      {/* How it works */}
      <section className="mt-14 grid gap-6 lg:grid-cols-2">
        <div className="rounded-2xl border border-border bg-surface p-6 shadow-sm sm:p-8">
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-primary-subtle text-primary-text">
              <Icon icon={Search} size={20} />
            </div>
            <h2 className="text-xl font-semibold">For candidates</h2>
          </div>
          <p className="mt-3 text-sm text-text-secondary">
            Find roles where your experience speaks first, and keep control of your identity while you apply.
          </p>
          <ol className="mt-6 space-y-5">
            {CANDIDATE_STEPS.map((step, index) => (
              <li key={step.title} className="flex gap-3">
                <span className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-primary-subtle text-xs font-bold text-primary-text">
                  {index + 1}
                </span>
                <div>
                  <h3 className="text-sm font-semibold">{step.title}</h3>
                  <p className="mt-0.5 text-sm text-text-secondary">{step.description}</p>
                </div>
              </li>
            ))}
          </ol>
          <Link to="/jobs" className="mt-6 inline-flex items-center gap-1.5 text-sm font-semibold text-primary-text hover:underline">
            Start with open roles
            <Icon icon={ArrowRight} size={15} />
          </Link>
        </div>

        <div className="rounded-2xl border border-border bg-surface p-6 shadow-sm sm:p-8">
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-primary-subtle text-primary-text">
              <Icon icon={Building2} size={20} />
            </div>
            <h2 className="text-xl font-semibold">For employers</h2>
          </div>
          <p className="mt-3 text-sm text-text-secondary">
            Screen faster, explain every decision, and give your team a fairer shortlist to work from.
          </p>
          <ol className="mt-6 space-y-5">
            {EMPLOYER_STEPS.map((step, index) => (
              <li key={step.title} className="flex gap-3">
                <span className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-primary-subtle text-xs font-bold text-primary-text">
                  {index + 1}
                </span>
                <div>
                  <h3 className="text-sm font-semibold">{step.title}</h3>
                  <p className="mt-0.5 text-sm text-text-secondary">{step.description}</p>
                </div>
              </li>
            ))}
          </ol>
          <Link to="/register" className="mt-6 inline-flex items-center gap-1.5 text-sm font-semibold text-primary-text hover:underline">
            Create an employer account
            <Icon icon={ArrowRight} size={15} />
          </Link>
        </div>
      </section>

      {/* Final CTA */}
      <section className="mt-14 rounded-2xl border border-primary bg-primary-subtle p-8 text-center shadow-sm sm:p-10">
        <h2 className="text-2xl font-bold">Ready to hire on evidence?</h2>
        <p className="mx-auto mt-2 max-w-xl text-sm text-text-secondary">
          Create an account to post a role or apply with confidence. Guests can browse every open job without signing in.
        </p>
        <div className="mt-6 flex flex-wrap justify-center gap-3">
          <Link to="/register">
            <Button>Get started</Button>
          </Link>
          <Link to="/jobs">
            <Button variant="secondary">Browse jobs</Button>
          </Link>
        </div>
      </section>
    </div>
  );
}
