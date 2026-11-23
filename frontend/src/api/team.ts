import { api } from './client';
import type {
  TeamMember,
  TeamResponse,
  TeamRole,
} from '../features/team/types';

export async function getTeam(): Promise<TeamResponse> {
  const { data } = await api.get<TeamResponse>('/api/employer/team');
  return data;
}

export async function inviteTeamMember(input: {
  email: string;
  role: TeamRole;
}): Promise<TeamResponse['invites'][number]> {
  const { data } = await api.post<TeamResponse['invites'][number]>(
    '/api/employer/team/invites',
    input,
  );
  return data;
}

export async function cancelTeamInvite(id: string): Promise<void> {
  await api.delete(`/api/employer/team/invites/${id}`);
}

export async function changeTeamRole(id: string, role: TeamRole): Promise<TeamMember> {
  const { data } = await api.patch<TeamMember>(`/api/employer/team/members/${id}`, { role });
  return data;
}

export async function removeTeamMember(id: string): Promise<void> {
  await api.delete(`/api/employer/team/members/${id}`);
}

export async function getInterviewers(): Promise<TeamMember[]> {
  const { data } = await api.get<TeamMember[]>('/api/employer/team/interviewers');
  return data;
}
