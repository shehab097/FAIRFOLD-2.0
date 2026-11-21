import { useEffect, useState, type FormEvent } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import { Briefcase, Building2, MapPin, RotateCcw, Search, SlidersHorizontal } from 'lucide-react';
import { useJobs } from '../queries';
import { EXPERIENCE_LEVELS, type ExperienceLevel } from '../types';
import { Banner } from '../../../components/ui/Banner';
import { Button } from '../../../components/ui/Button';
import { Icon } from '../../../components/ui/Icon';
import { Input } from '../../../components/ui/Input';
import { Select } from '../../../components/ui/Select';
import { Skeleton } from '../../../components/ui/Skeleton';
import { formatDate } from '../../../lib/format';

export function JobBoardPage() {
  const [params, setParams] = useSearchParams();
  const qParam = params.get('q') ?? '';
  const locationParam = params.get('location') ?? '';
  const remoteParam = params.get('remote') ?? '';
  const levelParam = (params.get('level') ?? '') as ExperienceLevel | '';
  const page = Math.max(0, Number(params.get('page') ?? '0') || 0);

  const [qInput, setQInput] = useState(qParam);
  const [locationInput, setLocationInput] = useState(locationParam);

  useEffect(() => {
    setQInput(qParam);
    setLocationInput(locationParam);
  }, [qParam, locationParam]);

  const { data, isLoading, isError, refetch } = useJobs({
    q: qParam || undefined,
    location: locationParam || undefined,
    remote: remoteParam === '' ? undefined : remoteParam === 'true',
    level: levelParam || undefined,
    page,
  });

  function applyParams(overrides: { q?: string; location?: string; remote?: string; level?: string; page?: number }) {
    const next = new URLSearchParams();
    const nextQ = (overrides.q ?? qInput).trim();
    const nextLoc = (overrides.location ?? locationInput).trim();
    const nextRemote = overrides.remote ?? remoteParam;
    const nextLevel = overrides.level ?? levelParam;
    const nextPage = overrides.page ?? 0;

    if (nextQ) next.set('q', nextQ);
    if (nextLoc) next.set('location', nextLoc);
    if (nextRemote) next.set('remote', nextRemote);
    if (nextLevel) next.set('level', nextLevel);
    if (nextPage > 0) next.set('page', String(nextPage));
    setParams(next);
  }

  function onSubmit(e: FormEvent) {
    e.preventDefault();
    applyParams({ page: 0 });
  }

  function resetFilters() {
    setQInput('');
    setLocationInput('');
    setParams(new URLSearchParams());
  }

  const hasFilters = Boolean(qParam || locationParam || remoteParam || levelParam);
  const totalPages = data?.totalPages ?? 0;

  return (
    <div className="space-y-6">
      <div className="flex flex-col justify-between gap-2 border-b border-border pb-4 sm:flex-row sm:items-end">
        <div>
          <p className="text-xs font-semibold uppercase tracking-[0.06em] text-primary-text">Job Board</p>
          <h1 className="mt-1 text-[28px] font-bold leading-9">Browse jobs</h1>
          <p className="text-text-secondary">Explore open roles with transparent, evidence-based evaluation.</p>
        </div>
        {data && (
          <div className="text-xs font-semibold uppercase tracking-[0.06em] text-text-secondary">
            {data.totalItems} {data.totalItems === 1 ? 'role' : 'roles'} found
          </div>
        )}
      </div>

      <div className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_300px]">
        {/* Left column: Job results */}
        <div className="order-2 space-y-4 lg:order-1">
          {hasFilters && (
            <div className="flex flex-wrap items-center justify-between gap-2 border border-border bg-surface px-4 py-2.5 text-xs">
              <div className="flex flex-wrap items-center gap-2">
                <span className="font-semibold uppercase tracking-[0.06em] text-text-secondary">Active filters:</span>
                {qParam && <span className="border border-border bg-raised px-2 py-0.5 text-text">Keyword: “{qParam}”</span>}
                {locationParam && <span className="border border-border bg-raised px-2 py-0.5 text-text">Location: {locationParam}</span>}
                {remoteParam && (
                  <span className="border border-border bg-raised px-2 py-0.5 text-text">
                    {remoteParam === 'true' ? 'Remote' : 'On-site'}
                  </span>
                )}
                {levelParam && (
                  <span className="border border-border bg-raised px-2 py-0.5 capitalize text-text">Level: {levelParam}</span>
                )}
              </div>
              <button
                type="button"
                onClick={resetFilters}
                className="font-semibold text-primary-text hover:underline"
              >
                Clear all
              </button>
            </div>
          )}

          {isLoading && (
            <div className="space-y-3">
              {[0, 1, 2, 3].map((i) => (
                <Skeleton key={i} className="h-28 w-full" />
              ))}
            </div>
          )}

          {isError && (
            <Banner tone="error">
              <span>Could not load jobs.</span>{' '}
              <button type="button" onClick={() => refetch()} className="text-primary-text hover:underline">
                Retry
              </button>
            </Banner>
          )}

          {!isLoading && !isError && data && data.items.length === 0 && (
            <div className="border border-border bg-surface p-10 text-center">
              <div className="mx-auto mb-3 flex h-12 w-12 items-center justify-center border border-border bg-raised text-text-secondary">
                <Icon icon={Briefcase} size={22} />
              </div>
              <p className="text-sm font-medium text-text">No jobs match your search.</p>
              <p className="mt-1 text-xs text-text-secondary">Try broadening your keywords or clearing active filters.</p>
              {hasFilters && (
                <div className="mt-4">
                  <Button variant="secondary" onClick={resetFilters}>
                    Reset filters
                  </Button>
                </div>
              )}
            </div>
          )}

          {!isLoading && !isError && data && data.items.length > 0 && (
            <div className="divide-y divide-border border border-border bg-surface">
              {data.items.map((job) => (
                <Link
                  key={job.id}
                  to={`/jobs/${job.id}`}
                  className="block border-l-[3px] border-l-transparent p-5 hover:border-l-primary hover:bg-raised"
                >
                  <div className="flex flex-col justify-between gap-2 sm:flex-row sm:items-start">
                    <div className="min-w-0">
                      <h2 className="text-base font-semibold text-text">{job.title}</h2>
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
                      </div>
                    </div>
                    <span className="shrink-0 text-xs text-text-subtle">Posted {formatDate(job.publishedAt)}</span>
                  </div>
                  <div className="mt-3 flex flex-wrap items-center gap-2">
                    <span className="border border-border bg-primary-subtle px-2 py-0.5 text-xs font-semibold uppercase tracking-[0.06em] text-primary-text">
                      {job.experienceLevel}
                    </span>
                    <span className="border border-border bg-bg px-2 py-0.5 text-xs text-text-secondary">
                      {job.remote ? 'Remote' : 'On-site'}
                    </span>
                    {job.topSkills.map((s) => (
                      <span key={s} className="border border-border bg-raised px-2 py-0.5 text-xs text-text">
                        {s}
                      </span>
                    ))}
                  </div>
                </Link>
              ))}
            </div>
          )}

          {totalPages > 1 && (
            <div className="flex items-center justify-between border border-border bg-surface px-4 py-3">
              <Button variant="secondary" disabled={page === 0} onClick={() => applyParams({ page: page - 1 })}>
                Previous
              </Button>
              <span className="text-sm text-text-secondary">
                Page {page + 1} of {totalPages}
              </span>
              <Button variant="secondary" disabled={page >= totalPages - 1} onClick={() => applyParams({ page: page + 1 })}>
                Next
              </Button>
            </div>
          )}
        </div>

        {/* Right column: Sticky Search & Filter Sidebar */}
        <aside className="order-1 lg:order-2 lg:sticky lg:top-20 lg:self-start">
          <form onSubmit={onSubmit} className="border border-border bg-surface">
            <div className="flex items-center justify-between border-b border-border bg-raised px-5 py-3.5">
              <div className="flex items-center gap-2">
                <Icon icon={SlidersHorizontal} size={16} className="text-primary-text" />
                <h2 className="text-xs font-semibold uppercase tracking-[0.06em] text-text">Search & Filters</h2>
              </div>
              {hasFilters && (
                <button
                  type="button"
                  onClick={resetFilters}
                  className="inline-flex items-center gap-1 text-xs font-medium text-primary-text hover:underline"
                >
                  <Icon icon={RotateCcw} size={12} />
                  Reset
                </button>
              )}
            </div>

            <div className="space-y-4 p-5">
              <div>
                <label htmlFor="job-search-q" className="mb-1 block text-xs font-semibold uppercase tracking-[0.06em] text-text-secondary">
                  Keywords
                </label>
                <Input
                  id="job-search-q"
                  name="q"
                  value={qInput}
                  onChange={(e) => setQInput(e.target.value)}
                  placeholder="Title, skill or keyword"
                />
              </div>

              <div>
                <label htmlFor="job-search-loc" className="mb-1 block text-xs font-semibold uppercase tracking-[0.06em] text-text-secondary">
                  Location
                </label>
                <Input
                  id="job-search-loc"
                  name="location"
                  value={locationInput}
                  onChange={(e) => setLocationInput(e.target.value)}
                  placeholder="City or country"
                />
              </div>

              <div>
                <label htmlFor="job-search-remote" className="mb-1 block text-xs font-semibold uppercase tracking-[0.06em] text-text-secondary">
                  Work type
                </label>
                <Select
                  id="job-search-remote"
                  value={remoteParam}
                  onChange={(e) => applyParams({ remote: e.target.value, page: 0 })}
                >
                  <option value="">Any work type</option>
                  <option value="true">Remote only</option>
                  <option value="false">On-site / Hybrid</option>
                </Select>
              </div>

              <div>
                <label htmlFor="job-search-level" className="mb-1 block text-xs font-semibold uppercase tracking-[0.06em] text-text-secondary">
                  Experience level
                </label>
                <Select
                  id="job-search-level"
                  value={levelParam}
                  onChange={(e) => applyParams({ level: e.target.value, page: 0 })}
                >
                  <option value="">Any experience level</option>
                  {EXPERIENCE_LEVELS.map((l) => (
                    <option key={l} value={l}>
                      {l}
                    </option>
                  ))}
                </Select>
              </div>

              <div className="pt-1">
                <Button type="submit" className="w-full">
                  <Icon icon={Search} size={16} />
                  Search jobs
                </Button>
              </div>
            </div>
          </form>
        </aside>
      </div>
    </div>
  );
}
