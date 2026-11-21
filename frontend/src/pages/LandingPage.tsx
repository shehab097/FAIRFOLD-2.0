import { useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import {
  ArrowRight,
  BarChart3,
  Building2,
  Check,
  EyeOff,
  MapPin,
  Search,
  ShieldCheck,
  UserCheck,
  type LucideIcon,
} from 'lucide-react';
import { useJobs } from '../features/jobs/queries';
import { EXPERIENCE_LABELS } from '../features/jobs/types';
import { Button } from '../components/ui/Button';
import { CompanyLogo } from '../components/ui/CompanyLogo';
import { Icon } from '../components/ui/Icon';
import { Skeleton } from '../components/ui/Skeleton';
import { formatSalary } from '../lib/salary';

const PILLARS: { icon: LucideIcon; title: string; text: string }[] = [
  { icon: EyeOff, title: 'Anonymised', text: 'Identity hidden during screening.' },
  { icon: BarChart3, title: 'Explained', text: 'Every score backed by quoted evidence.' },
  { icon: UserCheck, title: 'Human-led', text: 'People make the final call.' },
];

const AUDIENCES = [
  {
    icon: Search,
    title: 'Candidates',
    points: ['Apply without bias', 'Salary shown on every role', 'Track every application'],
    cta: { to: '/register', label: 'Find a job' },
  },
  {
    icon: Building2,
    title: 'Employers',
    points: ['Anonymised applicant list', 'Explainable match scores', 'Company profile & job posts'],
    cta: { to: '/register', label: 'Start hiring' },
  },
];

const POPULAR = ['Java', 'React', 'Python', 'Remote'];

export function LandingPage() {
  const navigate = useNavigate();
  const [q, setQ] = useState('');
  const { data: jobs, isLoading } = useJobs({ size: 6 });
  const { data: remoteJobs } = useJobs({ size: 1, remote: true });

  function search(e: FormEvent) {
    e.preventDefault();
    const term = q.trim();
    navigate(term ? `/jobs?q=${encodeURIComponent(term)}` : '/jobs');
  }

  const stats = [
    { value: jobs?.totalItems ?? '—', label: 'Open roles' },
    { value: remoteJobs?.totalItems ?? '—', label: 'Remote roles' },
    { value: '100%', label: 'Anonymised screening' },
  ];

  return (
    <div className="space-y-16 pb-6">
      {/* Hero */}
      <section className="grid gap-10 pt-6 lg:grid-cols-[1.1fr_0.9fr] lg:items-center lg:pt-10">
        <div>
          <h1 className="text-[36px] font-bold leading-[1.1] tracking-tight sm:text-[48px]">
            Hire on evidence,
            <br />
            <span className="text-primary-text">not assumptions.</span>
          </h1>
          <p className="mt-4 max-w-md text-base text-text-secondary sm:text-lg">
            Fair, anonymised and explainable hiring.
          </p>

          <form onSubmit={search} className="mt-7 flex max-w-lg gap-2 rounded-xl bg-surface p-1.5 shadow-sm ring-1 ring-border">
            <div className="flex flex-1 items-center gap-2 px-2.5">
              <Icon icon={Search} size={18} className="text-text-subtle" />
              <input
                value={q}
                onChange={(e) => setQ(e.target.value)}
                placeholder="Job title, skill or keyword"
                aria-label="Search jobs"
                className="w-full bg-transparent text-sm text-text placeholder:text-text-subtle focus:outline-none"
              />
            </div>
            <Button type="submit">Search</Button>
          </form>

          <div className="mt-3 flex flex-wrap items-center gap-2 text-xs text-text-secondary">
            <span>Popular:</span>
            {POPULAR.map((term) => (
              <Link
                key={term}
                to={term === 'Remote' ? '/jobs?remote=true' : `/jobs?q=${encodeURIComponent(term)}`}
                className="rounded-full bg-raised px-2.5 py-1 transition-colors hover:text-primary-text"
              >
                {term}
              </Link>
            ))}
          </div>
        </div>

        {/* Product preview */}
        <div className="mx-auto w-full max-w-sm rounded-2xl bg-surface p-5 shadow-lg ring-1 ring-border lg:max-w-none">
          <div className="flex items-start justify-between gap-4">
            <div>
              <div className="text-xs font-medium text-text-secondary">Screening result</div>
              <div className="mt-0.5 text-sm font-semibold text-text">Senior Backend Engineer</div>
            </div>
            <span className="rounded-full bg-success-subtle px-2.5 py-1 text-sm font-bold text-success">86</span>
          </div>
          <div className="mt-4 h-1.5 overflow-hidden rounded-full bg-raised">
            <div className="h-full w-[86%] rounded-full bg-primary" />
          </div>
          <p className="mt-4 rounded-lg bg-primary-subtle p-3 text-sm text-text">
            “Built a real-time pipeline handling 40k events/sec with Kafka.”
          </p>
          <div className="mt-3 flex flex-wrap gap-1.5">
            <span className="rounded-md bg-success-subtle px-2 py-0.5 text-xs font-medium text-success">Kafka</span>
            <span className="rounded-md bg-success-subtle px-2 py-0.5 text-xs font-medium text-success">Java</span>
            <span className="rounded-md bg-warning-subtle px-2 py-0.5 text-xs font-medium text-warning">Missing: Kubernetes</span>
          </div>
          <div className="mt-4 flex items-center gap-1.5 text-xs text-text-secondary">
            <Icon icon={ShieldCheck} size={14} />
            Candidate identity hidden
          </div>
        </div>
      </section>

      {/* Stats + pillars */}
      <section className="grid gap-6 rounded-2xl bg-surface p-6 shadow-sm sm:p-8 lg:grid-cols-[auto_1fr] lg:gap-12">
        <div className="grid grid-cols-3 gap-6 lg:gap-10">
          {stats.map((s) => (
            <div key={s.label}>
              <div className="text-2xl font-bold text-text sm:text-3xl">{s.value}</div>
              <div className="mt-0.5 text-xs text-text-secondary">{s.label}</div>
            </div>
          ))}
        </div>
        <div className="grid gap-5 border-t border-border pt-6 sm:grid-cols-3 lg:border-l lg:border-t-0 lg:pl-12 lg:pt-0">
          {PILLARS.map((p) => (
            <div key={p.title} className="flex items-start gap-3">
              <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-primary-subtle text-primary-text">
                <Icon icon={p.icon} size={18} />
              </span>
              <div>
                <div className="text-sm font-semibold text-text">{p.title}</div>
                <div className="text-xs text-text-secondary">{p.text}</div>
              </div>
            </div>
          ))}
        </div>
      </section>

      {/* Latest jobs */}
      <section>
        <div className="flex items-end justify-between gap-3">
          <h2 className="text-2xl font-bold">Latest roles</h2>
          <Link to="/jobs" className="inline-flex items-center gap-1 text-sm font-semibold text-primary-text hover:underline">
            View all
            <Icon icon={ArrowRight} size={15} />
          </Link>
        </div>

        {isLoading && (
          <div className="mt-5 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
            {[0, 1, 2].map((i) => (
              <Skeleton key={i} className="h-32 w-full" />
            ))}
          </div>
        )}

        {!isLoading && jobs && jobs.items.length > 0 && (
          <div className="mt-5 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
            {jobs.items.map((job) => (
              <Link
                key={job.id}
                to={`/jobs/${job.id}`}
                className="flex flex-col rounded-xl bg-surface p-4 shadow-sm ring-1 ring-border transition-shadow hover:shadow-md"
              >
                <div className="flex items-start gap-3">
                  <CompanyLogo name={job.companyName} logoUrl={job.companyLogoUrl} size="md" />
                  <div className="min-w-0">
                    <h3 className="truncate text-sm font-semibold text-text">{job.title}</h3>
                    <p className="truncate text-xs text-text-secondary">{job.companyName ?? 'Confidential company'}</p>
                  </div>
                </div>
                <div className="mt-3 flex items-center gap-1 text-xs text-text-secondary">
                  <Icon icon={MapPin} size={13} />
                  <span className="truncate">
                    {job.city ? `${job.city}, ` : ''}
                    {job.country}
                  </span>
                </div>
                <div className="mt-3 flex flex-wrap gap-1.5">
                  <span className="rounded-md bg-success-subtle px-2 py-0.5 text-xs font-medium text-success">
                    {formatSalary(job)}
                  </span>
                  <span className="rounded-md bg-info-subtle px-2 py-0.5 text-xs font-medium text-info">
                    {EXPERIENCE_LABELS[job.experienceLevel]}
                  </span>
                  <span className="rounded-md bg-raised px-2 py-0.5 text-xs text-text-secondary">
                    {job.remote ? 'Remote' : 'On-site'}
                  </span>
                </div>
              </Link>
            ))}
          </div>
        )}

        {!isLoading && (!jobs || jobs.items.length === 0) && (
          <p className="mt-5 rounded-xl bg-surface p-8 text-center text-sm text-text-secondary shadow-sm">
            No open roles right now. Check back soon.
          </p>
        )}
      </section>

      {/* Audiences */}
      <section className="grid gap-4 md:grid-cols-2">
        {AUDIENCES.map((a) => (
          <div key={a.title} className="flex flex-col rounded-2xl bg-surface p-6 shadow-sm sm:p-8">
            <div className="flex items-center gap-3">
              <span className="flex h-10 w-10 items-center justify-center rounded-lg bg-primary-subtle text-primary-text">
                <Icon icon={a.icon} size={20} />
              </span>
              <h2 className="text-lg font-semibold">For {a.title.toLowerCase()}</h2>
            </div>
            <ul className="mt-5 flex-1 space-y-2.5">
              {a.points.map((p) => (
                <li key={p} className="flex items-center gap-2 text-sm text-text">
                  <Icon icon={Check} size={16} className="text-success" />
                  {p}
                </li>
              ))}
            </ul>
            <Link to={a.cta.to} className="mt-6">
              <Button variant="secondary" className="w-full sm:w-auto">
                {a.cta.label}
                <Icon icon={ArrowRight} size={15} />
              </Button>
            </Link>
          </div>
        ))}
      </section>
    </div>
  );
}
