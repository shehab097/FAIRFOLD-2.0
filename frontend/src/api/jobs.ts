import { api } from './client';
import type { PageResponse } from './types';
import type { JobFilters, PublicJobDetail, PublicJobListItem } from '../features/jobs/types';

export async function listJobs(filters: JobFilters): Promise<PageResponse<PublicJobListItem>> {
  const { data } = await api.get<PageResponse<PublicJobListItem>>('/api/jobs', { params: filters });
  return data;
}

export async function getJob(id: string): Promise<PublicJobDetail> {
  const { data } = await api.get<PublicJobDetail>(`/api/jobs/${id}`);
  return data;
}
