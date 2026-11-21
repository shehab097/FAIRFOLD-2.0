import { api } from './client';
import type { PageResponse } from './types';
import type {
  EmployerApplicantListItem,
  EmployerJobDetail,
  EmployerJobListItem,
  JobInput,
  JobStatus,
} from '../features/jobs/types';

export type JobAction = 'activate' | 'pause' | 'resume' | 'close' | 'duplicate';

export async function listEmployerJobs(params: {
  status?: JobStatus;
  page?: number;
  size?: number;
}): Promise<PageResponse<EmployerJobListItem>> {
  const { data } = await api.get<PageResponse<EmployerJobListItem>>('/api/employer/jobs', { params });
  return data;
}

export async function getEmployerJob(id: string): Promise<EmployerJobDetail> {
  const { data } = await api.get<EmployerJobDetail>(`/api/employer/jobs/${id}`);
  return data;
}

export async function createJob(input: JobInput): Promise<EmployerJobDetail> {
  const { data } = await api.post<EmployerJobDetail>('/api/employer/jobs', input);
  return data;
}

export async function updateJob(id: string, input: JobInput): Promise<EmployerJobDetail> {
  const { data } = await api.put<EmployerJobDetail>(`/api/employer/jobs/${id}`, input);
  return data;
}

export async function deleteJob(id: string): Promise<void> {
  await api.delete(`/api/employer/jobs/${id}`);
}

export async function jobAction(id: string, action: JobAction): Promise<EmployerJobDetail> {
  const { data } = await api.post<EmployerJobDetail>(`/api/employer/jobs/${id}/${action}`);
  return data;
}

export async function listApplicants(
  jobId: string,
  page = 0,
): Promise<PageResponse<EmployerApplicantListItem>> {
  const { data } = await api.get<PageResponse<EmployerApplicantListItem>>(
    `/api/employer/jobs/${jobId}/applications`,
    { params: { page, size: 20 } },
  );
  return data;
}
