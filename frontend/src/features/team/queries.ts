import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  cancelTeamInvite,
  changeTeamRole,
  getInterviewers,
  getTeam,
  inviteTeamMember,
  removeTeamMember,
} from '../../api/team';
import type { TeamRole } from './types';

export function useTeam() {
  return useQuery({ queryKey: ['employer', 'team'], queryFn: getTeam });
}

export function useInterviewers() {
  return useQuery({ queryKey: ['employer', 'team', 'interviewers'], queryFn: getInterviewers });
}

function invalidateTeam(qc: ReturnType<typeof useQueryClient>) {
  qc.invalidateQueries({ queryKey: ['employer', 'team'] });
}

export function useInviteTeamMember() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: inviteTeamMember,
    onSuccess: () => invalidateTeam(qc),
  });
}

export function useCancelTeamInvite() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: cancelTeamInvite,
    onSuccess: () => invalidateTeam(qc),
  });
}

export function useChangeTeamRole() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, role }: { id: string; role: TeamRole }) => changeTeamRole(id, role),
    onSuccess: () => invalidateTeam(qc),
  });
}

export function useRemoveTeamMember() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: removeTeamMember,
    onSuccess: () => invalidateTeam(qc),
  });
}
