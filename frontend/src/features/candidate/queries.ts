import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  addSkill,
  applyToJob,
  deleteSkill,
  getApplication,
  getDashboard,
  getProfile,
  getResume,
  listApplications,
  listSkills,
  updateProfile,
  updateSkill,
  uploadResume,
} from '../../api/candidate';
import type { ApplicationStatus } from './types';

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
  return useQuery({ queryKey: ['candidate', 'resume'], queryFn: getResume, retry: false });
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

export function useApply(jobId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: () => applyToJob(jobId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['candidate', 'applications'] });
      qc.invalidateQueries({ queryKey: ['candidate', 'dashboard'] });
    },
  });
}

export function useDashboard() {
  return useQuery({ queryKey: ['candidate', 'dashboard'], queryFn: getDashboard });
}
