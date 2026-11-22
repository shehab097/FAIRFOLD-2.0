import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { deleteCompanyLogo, getCompany, updateCompany, uploadCompanyLogo } from '../../api/company';
import {
  createJob,
  deleteJob,
  getEmployerJob,
  jobAction,
  listEmployerJobs,
  updateJob,
  type JobAction,
} from '../../api/employerJobs';
import {
  estimateScreening,
  getEmployerDashboard,
  getEmployerReview,
  getScreeningProgress,
  listApplicants,
  pullBackApplication,
  rejectApplication,
  retryScreening,
  saveScreeningQuestions,
  shortlistApplication,
  startScreening,
  suggestScreeningQuestions,
} from '../../api/employerScreening';
import type { ApplicantFilters, JobStatus } from '../jobs/types';

export function useCompany() {
  return useQuery({ queryKey: ['employer', 'company'], queryFn: getCompany });
}

export function useUpdateCompany() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: updateCompany,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['employer', 'company'] }),
  });
}

export function useUploadCompanyLogo() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: uploadCompanyLogo,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['employer', 'company'] }),
  });
}

export function useDeleteCompanyLogo() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: deleteCompanyLogo,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['employer', 'company'] }),
  });
}

export function useEmployerJobs(status: JobStatus | undefined, page: number) {
  return useQuery({
    queryKey: ['employer', 'jobs', status, page],
    queryFn: () => listEmployerJobs({ status, page }),
    placeholderData: keepPreviousData,
  });
}

export function useEmployerJob(id: string | undefined) {
  return useQuery({
    queryKey: ['employer', 'job', id],
    queryFn: () => getEmployerJob(id as string),
    enabled: Boolean(id),
  });
}

function invalidateJobs(qc: ReturnType<typeof useQueryClient>) {
  qc.invalidateQueries({ queryKey: ['employer', 'jobs'] });
  qc.invalidateQueries({ queryKey: ['employer', 'dashboard'] });
  qc.invalidateQueries({ queryKey: ['jobs'] });
}

export function useCreateJob() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: createJob,
    onSuccess: () => invalidateJobs(qc),
  });
}

export function useUpdateJob(id: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: Parameters<typeof updateJob>[1]) => updateJob(id, input),
    onSuccess: () => {
      invalidateJobs(qc);
      qc.invalidateQueries({ queryKey: ['employer', 'job', id] });
    },
  });
}

export function useDeleteJob() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: deleteJob,
    onSuccess: () => invalidateJobs(qc),
  });
}

export function useJobAction() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, action }: { id: string; action: JobAction }) => jobAction(id, action),
    onSuccess: (_data, vars) => {
      invalidateJobs(qc);
      qc.invalidateQueries({ queryKey: ['employer', 'job', vars.id] });
    },
  });
}

export function useApplicants(jobId: string | undefined, filters: ApplicantFilters) {
  return useQuery({
    queryKey: ['employer', 'job', jobId, 'applications', filters],
    queryFn: () => listApplicants(jobId as string, filters),
    enabled: Boolean(jobId),
    placeholderData: keepPreviousData,
  });
}

export function useEmployerReview(applicationId: string | undefined) {
  return useQuery({
    queryKey: ['employer', 'application', applicationId],
    queryFn: () => getEmployerReview(applicationId as string),
    enabled: Boolean(applicationId),
  });
}

function invalidateReview(qc: ReturnType<typeof useQueryClient>) {
  qc.invalidateQueries({ queryKey: ['employer', 'application'] });
  qc.invalidateQueries({ queryKey: ['employer', 'job'] });
  qc.invalidateQueries({ queryKey: ['employer', 'dashboard'] });
}

export function useShortlistApplication() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, reason }: { id: string; reason?: string }) =>
      shortlistApplication(id, reason),
    onSuccess: () => invalidateReview(qc),
  });
}

export function useRejectApplication() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, reason }: { id: string; reason?: string }) =>
      rejectApplication(id, reason),
    onSuccess: () => invalidateReview(qc),
  });
}

export function usePullBackApplication() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => pullBackApplication(id),
    onSuccess: () => invalidateReview(qc),
  });
}

export function useScreeningEstimate(jobId: string | undefined, enabled: boolean) {
  return useQuery({
    queryKey: ['employer', 'job', jobId, 'screening', 'estimate'],
    queryFn: () => estimateScreening(jobId as string),
    enabled: Boolean(jobId) && enabled,
  });
}

export function useScreeningProgress(jobId: string | undefined) {
  return useQuery({
    queryKey: ['employer', 'job', jobId, 'screening'],
    queryFn: () => getScreeningProgress(jobId as string),
    enabled: Boolean(jobId),
    retry: false,
    refetchInterval: (query) =>
      query.state.data?.status === 'running' ? 3000 : false,
  });
}

export function useStartScreening() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (jobId: string) => startScreening(jobId),
    onSuccess: (_data, jobId) => {
      qc.invalidateQueries({ queryKey: ['employer', 'job', jobId, 'screening'] });
      qc.invalidateQueries({ queryKey: ['employer', 'job', jobId, 'applications'] });
      qc.invalidateQueries({ queryKey: ['employer', 'dashboard'] });
    },
  });
}

export function useRetryScreening() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (jobId: string) => retryScreening(jobId),
    onSuccess: (_data, jobId) => {
      qc.invalidateQueries({ queryKey: ['employer', 'job', jobId, 'screening'] });
      qc.invalidateQueries({ queryKey: ['employer', 'job', jobId, 'applications'] });
      qc.invalidateQueries({ queryKey: ['employer', 'dashboard'] });
    },
  });
}

export function useEmployerDashboard() {
  return useQuery({
    queryKey: ['employer', 'dashboard'],
    queryFn: getEmployerDashboard,
    refetchInterval: 15_000,
  });
}

export function useSuggestScreeningQuestions() {
  return useMutation({
    mutationFn: (jobId: string) => suggestScreeningQuestions(jobId),
  });
}

export function useSaveScreeningQuestions() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ jobId, questions }: { jobId: string; questions: string[] }) =>
      saveScreeningQuestions(jobId, questions),
    onSuccess: (_data, vars) =>
      qc.invalidateQueries({ queryKey: ['employer', 'job', vars.jobId] }),
  });
}
