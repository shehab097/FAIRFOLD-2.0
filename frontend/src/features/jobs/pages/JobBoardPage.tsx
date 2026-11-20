import { useSearchParams, Link } from 'react-router-dom';
import { useJobs } from '../queries';
import { EXPERIENCE_LEVELS, type ExperienceLevel } from '../types';
import { Banner } from '../../../components/ui/Banner';
import { Button } from '../../../components/ui/Button';
import { Input } from '../../../components/ui/Input';
import { Select } from '../../../components/ui/Select';
import { Skeleton } from '../../../components/ui/Skeleton';
import { formatDate } from '../../../lib/format';

export function JobBoardPage() {
  const [params, setParams] = useSearchParams();
  const q = params.get('q') ?? '';
  const location = params.get('location') ?? '';
  const remote = params.get('remote');
  const level = (params.get('level') ?? '') as ExperienceLevel | '';
  const page = Number(params.get('page') ?? '0');

  const { data, isLoading, isError, refetch } = useJobs({
    q: q || undefined,
    location: location || undefined,
    remote: remote === null ? undefined : remote === 'true',
    level: level || undefined,
    page,
  });

  function setFilter(key: string, value: string) {
    const next = new URLSearchParams(params);
    if (value === '') next.delete(key);
    else next.set(key, value);
    next.delete('page');
    setParams(next);
  }

  function setPage(p: number) {
    const next = new URLSearchParams(params);
    next.set('page', String(p));
    setParams(next);
  }

  const totalPages = data?.totalPages ?? 0;

  return (
    <div>
      <h1 className="text-[28px] font-bold leading-9">Jobs</h1>
      <p className="mb-6 text-text-secondary">Browse open roles.</p>

      <form
        className="mb-6 flex flex-col gap-3 border border-border bg-surface p-4 sm:flex-row"
        onSubmit={(e) => {
          e.preventDefault();
          const form = new FormData(e.currentTarget);
          const next = new URLSearchParams(params);
          const nq = String(form.get('q') ?? '');
          const nloc = String(form.get('location') ?? '');
          if (nq) next.set('q', nq);
          else next.delete('q');
          if (nloc) next.set('location', nloc);
          else next.delete('location');
          next.delete('page');
          setParams(next);
        }}
      >
        <Input name="q" defaultValue={q} placeholder="Search title, description or skills" className="flex-1" />
        <Input name="location" defaultValue={location} placeholder="Location" />
        <div className="flex gap-3">
          <Select
            value={remote ?? ''}
            onChange={(e) => setFilter('remote', e.target.value)}
            aria-label="Remote"
          >
            <option value="">Any work type</option>
            <option value="true">Remote</option>
            <option value="false">On-site</option>
          </Select>
          <Select value={level} onChange={(e) => setFilter('level', e.target.value)} aria-label="Experience level">
            <option value="">Any level</option>
            {EXPERIENCE_LEVELS.map((l) => (
              <option key={l} value={l}>
                {l}
              </option>
            ))}
          </Select>
          <Button type="submit" variant="secondary">
            Search
          </Button>
        </div>
      </form>

      {isLoading && (
        <div className="space-y-3">
          {[0, 1, 2, 3].map((i) => (
            <Skeleton key={i} className="h-20 w-full" />
          ))}
        </div>
      )}

      {isError && (
        <Banner tone="error">
          <span>Could not load jobs.</span>{' '}
          <button onClick={() => refetch()} className="text-primary-text hover:underline">
            Retry
          </button>
        </Banner>
      )}

      {!isLoading && !isError && data && data.items.length === 0 && (
        <div className="border border-border bg-surface p-8 text-center text-text-secondary">
          No jobs match your search.
        </div>
      )}

      {!isLoading && !isError && data && data.items.length > 0 && (
        <div className="divide-y divide-border border border-border bg-surface">
          {data.items.map((job) => (
            <Link key={job.id} to={`/jobs/${job.id}`} className="block p-4 hover:bg-raised">
              <div className="flex items-start justify-between gap-4">
                <div>
                  <h2 className="text-base font-semibold text-text">{job.title}</h2>
                  <p className="text-sm text-text-secondary">
                    {job.companyName ?? 'Confidential company'} · {job.city ? `${job.city}, ` : ''}
                    {job.country}
                  </p>
                </div>
                <span className="text-xs text-text-subtle">{formatDate(job.publishedAt)}</span>
              </div>
              <div className="mt-2 flex flex-wrap gap-2">
                {job.topSkills.map((s) => (
                  <span key={s} className="border border-border bg-raised px-2 py-0.5 text-xs text-text-secondary">
                    {s}
                  </span>
                ))}
                {job.remote && (
                  <span className="border border-border bg-raised px-2 py-0.5 text-xs text-text-secondary">Remote</span>
                )}
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
    </div>
  );
}
