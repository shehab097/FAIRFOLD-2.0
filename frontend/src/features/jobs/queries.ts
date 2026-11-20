import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { getJob, listJobs } from '../../api/jobs';
import type { JobFilters } from './types';

export function useJobs(filters: JobFilters) {
  return useQuery({
    queryKey: ['jobs', filters],
    queryFn: () => listJobs(filters),
    placeholderData: keepPreviousData,
  });
}

export function useJob(id: string | undefined) {
  return useQuery({
    queryKey: ['jobs', id],
    queryFn: () => getJob(id as string),
    enabled: Boolean(id),
  });
}
