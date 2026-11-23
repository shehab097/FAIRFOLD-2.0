import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  addSkill,
  applyResumeSuggestions,
  applyToJob,
  createWorkHistory,
  deleteWorkHistory,
  deleteSkill,
  getApplication,
  getDashboard,
  getMyApplication,
  getProfile,
  getResume,
  getResumeSuggestions,
  listApplications,
  listSkills,
  listWorkHistory,
  reportWrongStrip,
  updateProfile,
  updateSkill,
  updateWorkHistory,
  uploadResume,
} from '../../api/candidate';
import {
  createCertification,
  deleteCertification,
  listCertifications,
  updateCertification,
} from '../../api/certifications';
import type { ApplicationStatus, CertificationInput, WorkHistoryInput } from './types';

export function useProfile() {
  return useQuery({ queryKey: ['candidate', 'profile'], queryFn: getProfile });
}

export function useUpdateProfile() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: updateProfile,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['candidate', 'profile'] });
      qc.invalidateQueries({ queryKey: ['candidate', 'dashboard'] });
    },
  });
}

export function useSkills() {
  return useQuery({ queryKey: ['candidate', 'skills'], queryFn: listSkills });
}

export function useAddSkill() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: addSkill,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['candidate', 'skills'] });
      qc.invalidateQueries({ queryKey: ['candidate', 'dashboard'] });
    },
  });
}

export function useUpdateSkill() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, level, years }: { id: string; level: string; years: number | null }) =>
      updateSkill(id, { level, years }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['candidate', 'skills'] }),
  });
}

export function useDeleteSkill() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: deleteSkill,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['candidate', 'skills'] });
      qc.invalidateQueries({ queryKey: ['candidate', 'dashboard'] });
    },
  });
}

export function useResume() {
  return useQuery({
    queryKey: ['candidate', 'resume'],
    queryFn: getResume,
    retry: false,
    refetchInterval: (query) =>
      query.state.data?.status === 'processing' ? 2000 : false,
  });
}

export function useUploadResume() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: uploadResume,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['candidate', 'resume'] });
      qc.invalidateQueries({ queryKey: ['candidate', 'dashboard'] });
    },
  });
}

export function useResumeSuggestions(enabled: boolean) {
  return useQuery({
    queryKey: ['candidate', 'resume', 'suggestions'],
    queryFn: getResumeSuggestions,
    enabled,
    retry: false,
  });
}

export function useApplyResumeSuggestions() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: applyResumeSuggestions,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['candidate', 'profile'] });
      qc.invalidateQueries({ queryKey: ['candidate', 'skills'] });
      qc.invalidateQueries({ queryKey: ['candidate', 'work-history'] });
      qc.invalidateQueries({ queryKey: ['candidate', 'dashboard'] });
    },
  });
}

export function useReportWrongStrip() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: reportWrongStrip,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['candidate', 'resume'] }),
  });
}

export function useWorkHistory() {
  return useQuery({ queryKey: ['candidate', 'work-history'], queryFn: listWorkHistory });
}

export function useCreateWorkHistory() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: createWorkHistory,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['candidate', 'work-history'] });
      qc.invalidateQueries({ queryKey: ['candidate', 'profile'] });
    },
  });
}

export function useUpdateWorkHistory() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, input }: { id: string; input: WorkHistoryInput }) =>
      updateWorkHistory(id, input),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['candidate', 'work-history'] });
      qc.invalidateQueries({ queryKey: ['candidate', 'profile'] });
    },
  });
}

export function useDeleteWorkHistory() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: deleteWorkHistory,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['candidate', 'work-history'] });
      qc.invalidateQueries({ queryKey: ['candidate', 'profile'] });
    },
  });
}

export function useApplications(status: ApplicationStatus | undefined, page: number) {
  return useQuery({
    queryKey: ['candidate', 'applications', status, page],
    queryFn: () => listApplications({ status, page }),
    placeholderData: keepPreviousData,
  });
}

export function useApplication(id: string | undefined) {
  return useQuery({
    queryKey: ['candidate', 'application', id],
    queryFn: () => getApplication(id as string),
    enabled: Boolean(id),
  });
}

export function useMyApplication(jobId: string | undefined, enabled: boolean) {
  return useQuery({
    queryKey: ['candidate', 'application-for-job', jobId],
    queryFn: () => getMyApplication(jobId as string),
    enabled: Boolean(jobId) && enabled,
    retry: false,
  });
}

export function useApply(jobId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: () => applyToJob(jobId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['candidate', 'applications'] });
      qc.invalidateQueries({ queryKey: ['candidate', 'application-for-job', jobId] });
      qc.invalidateQueries({ queryKey: ['candidate', 'dashboard'] });
    },
  });
}

export function useDashboard() {
  return useQuery({ queryKey: ['candidate', 'dashboard'], queryFn: getDashboard });
}

export function useCertifications() {
  return useQuery({ queryKey: ['candidate', 'certifications'], queryFn: listCertifications });
}

export function useCreateCertification() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: createCertification,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['candidate', 'certifications'] }),
  });
}

export function useUpdateCertification() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, input }: { id: string; input: CertificationInput }) =>
      updateCertification(id, input),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['candidate', 'certifications'] }),
  });
}

export function useDeleteCertification() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: deleteCertification,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['candidate', 'certifications'] }),
  });
}
