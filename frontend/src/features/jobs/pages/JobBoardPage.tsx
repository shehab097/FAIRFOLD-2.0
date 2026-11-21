import { useEffect, useState, type FormEvent } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import { Briefcase, Building2, MapPin, RotateCcw, Search } from 'lucide-react';
import { useJobs } from '../queries';
import { EXPERIENCE_LEVELS, type ExperienceLevel } from '../types';
import { Banner } from '../../../components/ui/Banner';
import { Button } from '../../../components/ui/Button';
import { CompanyLogo } from '../../../components/ui/CompanyLogo';
import { Icon } from '../../../components/ui/Icon';
import { Input } from '../../../components/ui/Input';
import { Select } from '../../../components/ui/Select';
import { Skeleton } from '../../../components/ui/Skeleton';
import { formatDate } from '../../../lib/format';
import { COUNTRIES } from '../../../lib/locations';

export function JobBoardPage() {
  const [params, setParams] = useSearchParams();
  const qParam = params.get('q') ?? '';
  const locationParam = params.get('location') ?? '';
  const countryParam = params.get('country') ?? '';
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
    country: countryParam || undefined,
    remote: remoteParam === '' ? undefined : remoteParam === 'true',
    level: levelParam || undefined,
    page,
  });

  function applyParams(overrides: {
    q?: string;
    location?: string;
    country?: string;
    remote?: string;
    level?: string;
    page?: number;
  }) {
    const next = new URLSearchParams();
    const nextQ = (overrides.q ?? qInput).trim();
    const nextLoc = (overrides.location ?? locationInput).trim();
    const nextCountry = overrides.country ?? countryParam;
    const nextRemote = overrides.remote ?? remoteParam;
    const nextLevel = overrides.level ?? levelParam;
    const nextPage = overrides.page ?? 0;

    if (nextQ) next.set('q', nextQ);
    if (nextLoc) next.set('location', nextLoc);
    if (nextCountry) next.set('country', nextCountry);
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

  const hasFilters = Boolean(qParam || locationParam || countryParam || remoteParam || levelParam);
  const totalPages = data?.totalPages ?? 0;

  return (
    <div className="space-y-6">
      <div className="flex flex-col justify-between gap-1 sm:flex-row sm:items-end">
        <div>
          <h1 className="text-[28px] font-bold leading-9">Browse jobs</h1>
          <p className="text-sm text-text-secondary">Explore open roles with transparent, evidence-based evaluation.</p>
        </div>
        {data && (
          <div className="text-sm text-text-secondary">
            {data.totalItems} {data.totalItems === 1 ? 'role' : 'roles'} available
          </div>
        )}
      </div>

      <div className="grid gap-8 lg:grid-cols-[minmax(0,1fr)_280px]">
        {/* Left column: Job results */}
        <div className="order-2 space-y-4 lg:order-1">
          {hasFilters && (
            <div className="flex flex-wrap items-center justify-between gap-2 text-xs text-text-secondary">
              <div className="flex flex-wrap items-center gap-1.5">
                <span>Filtered by:</span>
                {qParam && <span className="rounded-md bg-surface px-2 py-1 text-text shadow-sm">“{qParam}”</span>}
                {locationParam && <span className="rounded-md bg-surface px-2 py-1 text-text shadow-sm">{locationParam}</span>}
                {countryParam && <span className="rounded-md bg-surface px-2 py-1 text-text shadow-sm">{countryParam}</span>}
                {remoteParam && (
                  <span className="rounded-md bg-surface px-2 py-1 text-text shadow-sm">
                    {remoteParam === 'true' ? 'Remote' : 'On-site'}
                  </span>
                )}
                {levelParam && (
                  <span className="rounded-md bg-surface px-2 py-1 capitalize text-text shadow-sm">{levelParam}</span>
                )}
              </div>
              <button
                type="button"
                onClick={resetFilters}
                className="font-medium text-primary-text hover:underline"
              >
                Clear filters
              </button>
            </div>
          )}

          {isLoading && (
            <div className="space-y-3">
              {[0, 1, 2, 3].map((i) => (
                <Skeleton key={i} className="h-24 w-full" />
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
            <div className="rounded-xl border border-border bg-surface p-10 text-center shadow-sm">
              <Icon icon={Briefcase} size={24} className="mx-auto mb-2 text-text-subtle" />
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
            <div className="divide-y divide-border overflow-hidden rounded-xl border border-border bg-surface shadow-sm">
              {data.items.map((job) => (
                <Link
                  key={job.id}
                  to={`/jobs/${job.id}`}
                  className="block p-5 transition-colors hover:bg-raised"
                >
                  <div className="flex flex-col justify-between gap-2 sm:flex-row sm:items-start">
                    <div className="flex min-w-0 items-start gap-3">
                      <CompanyLogo name={job.companyName} logoUrl={job.companyLogoUrl} size="md" />
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
                    </div>
                    <span className="shrink-0 text-xs text-text-subtle">{formatDate(job.publishedAt)}</span>
                  </div>
                  <div className="mt-3 flex flex-wrap items-center gap-2 pl-[52px]">
                    <span className="rounded-md bg-primary-subtle px-2.5 py-0.5 text-xs font-medium capitalize text-primary-text">
                      {job.experienceLevel}
                    </span>
                    <span className="rounded-md bg-bg px-2.5 py-0.5 text-xs text-text-secondary">
                      {job.remote ? 'Remote' : 'On-site'}
                    </span>
                    {job.topSkills.map((s) => (
                      <span key={s} className="rounded-md bg-bg px-2.5 py-0.5 text-xs text-text-secondary">
                        {s}
                      </span>
                    ))}
                  </div>
                </Link>
              ))}
            </div>
          )}

          {totalPages > 1 && (
            <div className="flex items-center justify-between pt-2">
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
          <form onSubmit={onSubmit} className="space-y-4 rounded-xl border border-border bg-surface p-5 shadow-sm">
            <div className="flex items-center justify-between">
              <h2 className="text-sm font-semibold text-text">Filter roles</h2>
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

            <div>
              <label htmlFor="job-search-q" className="mb-1 block text-xs text-text-secondary">
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
              <label htmlFor="job-search-loc" className="mb-1 block text-xs text-text-secondary">
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
              <label htmlFor="job-search-country" className="mb-1 block text-xs text-text-secondary">
                Country
              </label>
              <Select
                id="job-search-country"
                value={countryParam}
                onChange={(e) => applyParams({ country: e.target.value, page: 0 })}
              >
                <option value="">Any country</option>
                {COUNTRIES.map((c) => (
                  <option key={c.isoCode} value={c.name}>
                    {c.name}
                  </option>
                ))}
              </Select>
            </div>

            <div>
              <label htmlFor="job-search-remote" className="mb-1 block text-xs text-text-secondary">
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
              <label htmlFor="job-search-level" className="mb-1 block text-xs text-text-secondary">
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

            <Button type="submit" className="w-full">
              <Icon icon={Search} size={16} />
              Search
            </Button>
          </form>
        </aside>
      </div>
    </div>
  );
}
