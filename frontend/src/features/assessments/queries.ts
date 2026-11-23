import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  listAssessments,
  listAttempts,
  saveAnswers,
  startAttempt,
  submitAttempt,
} from '../../api/assessments';

export function useCandidateAssessments() {
  return useQuery({ queryKey: ['assessments'], queryFn: listAssessments });
}

export function useAttemptHistory(page: number) {
  return useQuery({
    queryKey: ['assessments', 'attempts', page],
    queryFn: () => listAttempts(page),
    placeholderData: keepPreviousData,
  });
}

export function useStartAttempt() {
  return useMutation({ mutationFn: startAttempt });
}

export function useSaveAnswers() {
  return useMutation({
    mutationFn: ({ attemptId, answers }: { attemptId: string; answers: Record<string, string> }) =>
      saveAnswers(attemptId, answers),
  });
}

export function useSubmitAttempt() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: submitAttempt,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['assessments'] });
      qc.invalidateQueries({ queryKey: ['assessments', 'attempts'] });
      qc.invalidateQueries({ queryKey: ['candidate', 'skills'] });
      qc.invalidateQueries({ queryKey: ['candidate', 'dashboard'] });
    },
  });
}
