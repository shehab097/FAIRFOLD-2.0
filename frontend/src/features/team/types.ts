export type TeamRole = 'employer_hr' | 'employer_manager' | 'interviewer';

export interface TeamMember {
  id: string;
  name: string;
  email: string;
  role: TeamRole;
}

export interface TeamInvite {
  id: string;
  email: string;
  role: TeamRole;
  expiresAt: string;
}

export interface TeamResponse {
  members: TeamMember[];
  invites: TeamInvite[];
}

export const TEAM_ROLES: { value: TeamRole; label: string }[] = [
  { value: 'employer_hr', label: 'HR' },
  { value: 'employer_manager', label: 'Manager' },
  { value: 'interviewer', label: 'Interviewer' },
];

export function teamRoleLabel(role: string): string {
  return TEAM_ROLES.find((r) => r.value === role)?.label ?? role;
}
