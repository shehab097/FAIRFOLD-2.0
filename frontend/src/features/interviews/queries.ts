import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  generateInterviewPack,
  getInterview,
  getInterviewPack,
  hireApplication,
  listInterviews,
  saveInterviewPack,
  scheduleInterview,
  submitInterviewFeedback,
} from '../../api/interviews';
import type { InterviewFeedbackInput, ScheduleInterviewInput } from './types';

export function useInterviewPack(jobId: string | undefined) {
  return useQuery({
    queryKey: ['employer', 'job', jobId, 'interview-pack'],
    queryFn: () => getInterviewPack(jobId as string),
    enabled: Boolean(jobId),
  });
}

export function useGenerateInterviewPack(jobId: string) {
  return useMutation({
    mutationFn: () => generateInterviewPack(jobId),
  });
}

export function useSaveInterviewPack(jobId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: Parameters<typeof saveInterviewPack>[1]) =>
      saveInterviewPack(jobId, input),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['employer', 'job', jobId, 'interview-pack'] }),
  });
}

export function useInterviews(tab: 'upcoming' | 'past', page: number) {
  return useQuery({
    queryKey: ['interviews', tab, page],
    queryFn: () => listInterviews(tab, page),
    placeholderData: keepPreviousData,
  });
}

export function useInterview(id: string | undefined) {
  return useQuery({
    queryKey: ['interview', id],
    queryFn: () => getInterview(id as string),
    enabled: Boolean(id),
  });
}

export function useScheduleInterview() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ applicationId, input }: { applicationId: string; input: ScheduleInterviewInput }) =>
      scheduleInterview(applicationId, input),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['employer', 'application'] });
      qc.invalidateQueries({ queryKey: ['employer', 'job'] });
      qc.invalidateQueries({ queryKey: ['employer', 'dashboard'] });
      qc.invalidateQueries({ queryKey: ['interviews'] });
      qc.invalidateQueries({ queryKey: ['candidate'] });
    },
  });
}

export function useSubmitInterviewFeedback(id: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: InterviewFeedbackInput) => submitInterviewFeedback(id, input),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['interview', id] });
      qc.invalidateQueries({ queryKey: ['interviews'] });
    },
  });
}

export function useHireApplication() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: hireApplication,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['employer', 'application'] });
      qc.invalidateQueries({ queryKey: ['employer', 'job'] });
      qc.invalidateQueries({ queryKey: ['employer', 'dashboard'] });
      qc.invalidateQueries({ queryKey: ['interviews'] });
    },
  });
}
