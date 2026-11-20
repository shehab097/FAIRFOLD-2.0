import type { Role } from '../features/auth/types';

/** Where a user lands after login, by role. */
export function homePathForRole(role: Role): string {
  switch (role) {
    case 'CANDIDATE':
      return '/candidate';
    case 'EMPLOYER_HR':
    case 'EMPLOYER_MANAGER':
      return '/employer';
    case 'INTERVIEWER':
      return '/employer/interviews';
    case 'ADMIN':
      return '/admin';
    default:
      return '/';
  }
}

/** True when the user's role is in the allowed list (route-guard decision). */
export function canAccessRole(userRole: Role | undefined, allowed: Role[]): boolean {
  if (!userRole) return false;
  return allowed.includes(userRole);
}
