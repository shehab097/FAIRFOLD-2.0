import { api } from './client';
import type { PageResponse } from './types';
import type {
  AttemptHistoryItem,
  AttemptView,
  CandidateAssessment,
  CandidateResource,
} from '../features/assessments/types';

export async function listAssessments(): Promise<CandidateAssessment[]> {
  const { data } = await api.get<CandidateAssessment[]>('/api/assessments');
  return data;
}

export async function listAttempts(page: number): Promise<PageResponse<AttemptHistoryItem>> {
  const { data } = await api.get<PageResponse<AttemptHistoryItem>>('/api/candidate/attempts', {
    params: { page },
  });
  return data;
}

export async function startAttempt(assessmentId: string): Promise<AttemptView> {
  const { data } = await api.post<AttemptView>(`/api/assessments/${assessmentId}/attempts`);
  return data;
}

export async function saveAnswers(
  attemptId: string,
  answers: Record<string, string>,
): Promise<Record<string, string>> {
  const { data } = await api.put<{ savedAt: string; answers: Record<string, string> }>(
    `/api/attempts/${attemptId}/answers`,
    { answers },
  );
  return data.answers;
}

export async function submitAttempt(attemptId: string): Promise<AttemptView> {
  const { data } = await api.post<AttemptView>(`/api/attempts/${attemptId}/submit`);
  return data;
}

export async function getAttempt(attemptId: string): Promise<AttemptView> {
  const { data } = await api.get<AttemptView>(`/api/attempts/${attemptId}`);
  return data;
}

export async function listResources(skillId: string): Promise<CandidateResource[]> {
  const { data } = await api.get<CandidateResource[]>('/api/resources', { params: { skillId } });
  return data;
}
