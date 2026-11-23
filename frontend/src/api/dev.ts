import { api } from './client';
import type { AttemptView } from '../features/assessments/types';

/** Development-only: fill and submit an attempt with N correct answers. */
export async function devCompleteAttempt(
  attemptId: string,
  correctCount?: number,
): Promise<AttemptView> {
  const { data } = await api.post<AttemptView>(`/api/dev/attempts/${attemptId}/complete`, {
    correctCount: correctCount ?? null,
  });
  return data;
}
