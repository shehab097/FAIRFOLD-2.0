import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  checkResourceLinks,
  createAssessment,
  createResource,
  getAssessment,
  listAssessments,
  listResources,
  setAssessmentActive,
  setResourceActive,
  updateAssessment,
  updateResource,
} from '../../api/adminContent';

export function useAssessments(page: number) {
  return useQuery({
    queryKey: ['admin', 'assessments', page],
    queryFn: () => listAssessments(page),
    placeholderData: keepPreviousData,
  });
}

export function useAssessment(id: string | undefined) {
  return useQuery({
    queryKey: ['admin', 'assessment', id],
    queryFn: () => getAssessment(id as string),
    enabled: Boolean(id),
  });
}

export function useCreateAssessment() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: createAssessment,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['admin', 'assessments'] }),
  });
}

export function useUpdateAssessment(id: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: Parameters<typeof updateAssessment>[1]) => updateAssessment(id, input),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['admin', 'assessments'] });
      qc.invalidateQueries({ queryKey: ['admin', 'assessment', id] });
    },
  });
}

export function useSetAssessmentActive() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, active }: { id: string; active: boolean }) =>
      setAssessmentActive(id, active),
    onSuccess: (_data, vars) => {
      qc.invalidateQueries({ queryKey: ['admin', 'assessments'] });
      qc.invalidateQueries({ queryKey: ['admin', 'assessment', vars.id] });
    },
  });
}

export function useResources(page: number, skillId?: string) {
  return useQuery({
    queryKey: ['admin', 'resources', page, skillId ?? 'all'],
    queryFn: () => listResources(page, skillId),
    placeholderData: keepPreviousData,
  });
}

export function useCreateResource() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: createResource,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['admin', 'resources'] }),
  });
}

export function useUpdateResource() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, input }: { id: string; input: Parameters<typeof updateResource>[1] }) =>
      updateResource(id, input),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['admin', 'resources'] }),
  });
}

export function useSetResourceActive() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, active }: { id: string; active: boolean }) =>
      setResourceActive(id, active),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['admin', 'resources'] }),
  });
}

export function useCheckResourceLinks() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: checkResourceLinks,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['admin', 'resources'] }),
  });
}
