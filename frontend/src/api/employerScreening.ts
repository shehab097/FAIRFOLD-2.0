import { api } from './client';
import type { PageResponse } from './types';
import type {
  ApplicantFilters,
  EmployerApplicantListItem,
  EmployerDashboard,
  EmployerReview,
  ScreeningEstimate,
  ScreeningProgress,
  ScreeningQuestions,
} from '../features/jobs/types';

export async function listApplicants(
  jobId: string,
  filters: ApplicantFilters,
): Promise<PageResponse<EmployerApplicantListItem>> {
  const { data } = await api.get<PageResponse<EmployerApplicantListItem>>(
    `/api/employer/jobs/${jobId}/applications`,
    { params: filters },
  );
  return data;
}

export async function getEmployerReview(id: string): Promise<EmployerReview> {
  const { data } = await api.get<EmployerReview>(`/api/employer/applications/${id}`);
  return data;
}

export async function shortlistApplication(id: string, reason?: string): Promise<EmployerReview> {
  const { data } = await api.post<EmployerReview>(`/api/employer/applications/${id}/shortlist`, {
    reason: reason ?? null,
  });
  return data;
}

export async function rejectApplication(id: string, reason?: string): Promise<void> {
  await api.post(`/api/employer/applications/${id}/reject`, { reason: reason ?? null });
}

export async function pullBackApplication(id: string): Promise<{ runId: string }> {
  const { data } = await api.post<{ runId: string }>(`/api/employer/applications/${id}/pull-back`);
  return data;
}

export async function estimateScreening(jobId: string): Promise<ScreeningEstimate> {
  const { data } = await api.post<ScreeningEstimate>(
    `/api/employer/jobs/${jobId}/screening/estimate`,
  );
  return data;
}

export async function startScreening(jobId: string): Promise<{ runId: string }> {
  const { data } = await api.post<{ runId: string }>(`/api/employer/jobs/${jobId}/screening`);
  return data;
}

export async function getScreeningProgress(jobId: string): Promise<ScreeningProgress> {
  const { data } = await api.get<ScreeningProgress>(`/api/employer/jobs/${jobId}/screening`);
  return data;
}

export async function retryScreening(jobId: string): Promise<{ requeued: number }> {
  const { data } = await api.post<{ requeued: number }>(
    `/api/employer/jobs/${jobId}/screening/retry`,
  );
  return data;
}

export async function getEmployerDashboard(): Promise<EmployerDashboard> {
  const { data } = await api.get<EmployerDashboard>('/api/employer/dashboard');
  return data;
}

export async function suggestScreeningQuestions(jobId: string): Promise<ScreeningQuestions> {
  const { data } = await api.post<ScreeningQuestions>(
    `/api/employer/jobs/${jobId}/suggest-questions`,
  );
  return data;
}

export async function saveScreeningQuestions(
  jobId: string,
  questions: string[],
): Promise<ScreeningQuestions> {
  const { data } = await api.put<ScreeningQuestions>(
    `/api/employer/jobs/${jobId}/screening-questions`,
    { questions },
  );
  return data;
}
