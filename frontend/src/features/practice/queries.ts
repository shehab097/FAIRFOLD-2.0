import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  createPracticeSession,
  getPracticeSession,
  listPracticeQuestions,
  listPracticeSessions,
  ratePracticeSession,
} from '../../api/practice';
import {
  createAdminPracticeQuestion,
  listAdminPracticeQuestions,
  setAdminPracticeQuestionActive,
  updateAdminPracticeQuestion,
} from '../../api/adminPractice';

export function usePracticeQuestions(topic?: string) {
  return useQuery({
    queryKey: ['practice', 'questions', topic ?? 'all'],
    queryFn: () => listPracticeQuestions(topic),
    placeholderData: keepPreviousData,
  });
}

export function useCreatePracticeSession() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ questionId, answer }: { questionId: string; answer: string }) =>
      createPracticeSession(questionId, answer),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['practice', 'sessions'] }),
  });
}

export function usePracticeSessions(page: number) {
  return useQuery({
    queryKey: ['practice', 'sessions', page],
    queryFn: () => listPracticeSessions(page),
    placeholderData: keepPreviousData,
  });
}

export function usePracticeSession(id: string | null) {
  return useQuery({
    queryKey: ['practice', 'session', id],
    queryFn: () => getPracticeSession(id as string),
    enabled: Boolean(id),
  });
}

export function useRatePracticeSession() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, rating }: { id: string; rating: number }) =>
      ratePracticeSession(id, rating),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['practice', 'sessions'] });
      qc.invalidateQueries({ queryKey: ['practice', 'session'] });
    },
  });
}

export function useAdminPracticeQuestions(page: number) {
  return useQuery({
    queryKey: ['admin', 'practice-questions', page],
    queryFn: () => listAdminPracticeQuestions(page),
    placeholderData: keepPreviousData,
  });
}

function invalidateAdmin(qc: ReturnType<typeof useQueryClient>) {
  qc.invalidateQueries({ queryKey: ['admin', 'practice-questions'] });
  qc.invalidateQueries({ queryKey: ['practice', 'questions'] });
}

export function useCreateAdminPracticeQuestion() {
  const qc = useQueryClient();
  return useMutation({ mutationFn: createAdminPracticeQuestion, onSuccess: () => invalidateAdmin(qc) });
}

export function useUpdateAdminPracticeQuestion() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, input }: { id: string; input: { text: string; topic: string; active: boolean } }) =>
      updateAdminPracticeQuestion(id, input),
    onSuccess: () => invalidateAdmin(qc),
  });
}

export function useSetAdminPracticeQuestionActive() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, active }: { id: string; active: boolean }) =>
      setAdminPracticeQuestionActive(id, active),
    onSuccess: () => invalidateAdmin(qc),
  });
}
