import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { getCompany, updateCompany } from '../../api/company';
import {
  createJob,
  deleteJob,
  getEmployerJob,
  jobAction,
  listEmployerJobs,
  updateJob,
  type JobAction,
} from '../../api/employerJobs';
import type { JobStatus } from '../jobs/types';

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
