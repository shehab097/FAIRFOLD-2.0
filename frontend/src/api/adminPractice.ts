import { api } from './client';
import type { PageResponse } from './types';
import type { AdminPracticeQuestion } from '../features/practice/types';

export async function listAdminPracticeQuestions(
  page: number,
): Promise<PageResponse<AdminPracticeQuestion>> {
  const { data } = await api.get<PageResponse<AdminPracticeQuestion>>(
    '/api/admin/practice-questions',
    { params: { page } },
  );
  return data;
}

export async function createAdminPracticeQuestion(input: {
  text: string;
  topic: string;
  active: boolean;
}): Promise<AdminPracticeQuestion> {
  const { data } = await api.post<AdminPracticeQuestion>(
    '/api/admin/practice-questions',
    input,
  );
  return data;
}

export async function updateAdminPracticeQuestion(
  id: string,
  input: { text: string; topic: string; active: boolean },
): Promise<AdminPracticeQuestion> {
  const { data } = await api.put<AdminPracticeQuestion>(
    `/api/admin/practice-questions/${id}`,
    input,
  );
  return data;
}

export async function setAdminPracticeQuestionActive(
  id: string,
  active: boolean,
): Promise<AdminPracticeQuestion> {
  const { data } = await api.patch<AdminPracticeQuestion>(
    `/api/admin/practice-questions/${id}/active`,
    { active },
  );
  return data;
}
