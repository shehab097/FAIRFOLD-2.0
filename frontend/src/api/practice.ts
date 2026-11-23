import { api } from './client';
import type { PageResponse } from './types';
import type {
  PracticeQuestionOption,
  PracticeSession,
  PracticeSessionListItem,
} from '../features/practice/types';

export async function listPracticeQuestions(topic?: string): Promise<PracticeQuestionOption[]> {
  const { data } = await api.get<PracticeQuestionOption[]>('/api/practice/questions', {
    params: topic ? { topic } : undefined,
  });
  return data;
}

export async function createPracticeSession(
  questionId: string,
  answer: string,
): Promise<PracticeSession> {
  const { data } = await api.post<PracticeSession>('/api/practice/sessions', {
    questionId,
    answer,
  });
  return data;
}

export async function listPracticeSessions(page: number): Promise<PageResponse<PracticeSessionListItem>> {
  const { data } = await api.get<PageResponse<PracticeSessionListItem>>('/api/practice/sessions', {
    params: { page },
  });
  return data;
}

export async function getPracticeSession(id: string): Promise<PracticeSession> {
  const { data } = await api.get<PracticeSession>(`/api/practice/sessions/${id}`);
  return data;
}

export async function ratePracticeSession(id: string, rating: number): Promise<PracticeSession> {
  const { data } = await api.post<PracticeSession>(`/api/practice/sessions/${id}/rating`, {
    rating,
  });
  return data;
}
