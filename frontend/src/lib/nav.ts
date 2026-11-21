import type { Role } from '../features/auth/types';

export interface NavItem {
  label: string;
  to: string;
  enabled: boolean;
}

/**
 * Sidebar config per role. Items with `enabled: false` exist in the product
 * plan but have no page yet; the PortalShell only renders enabled items.
 */
export const SIDEBAR_ITEMS: Record<Role, NavItem[]> = {
  CANDIDATE: [
    { label: 'Dashboard', to: '/candidate', enabled: true },
    { label: 'Profile', to: '/candidate/profile', enabled: true },
    { label: 'Journey', to: '/candidate/journey', enabled: false },
    { label: 'Resume', to: '/candidate/resume', enabled: true },
    { label: 'Applications', to: '/candidate/applications', enabled: true },
    { label: 'Assessments', to: '/candidate/assessments', enabled: false },
    { label: 'Practice', to: '/candidate/practice', enabled: false },
    { label: 'Jobs', to: '/', enabled: true },
  ],
  EMPLOYER_HR: [
    { label: 'Dashboard', to: '/employer', enabled: false },
    { label: 'Jobs', to: '/employer/jobs', enabled: true },
    { label: 'Interviews', to: '/employer/interviews', enabled: false },
    { label: 'Analytics', to: '/employer/analytics', enabled: false },
    { label: 'Team', to: '/employer/team', enabled: false },
    { label: 'Company', to: '/employer/company', enabled: true },
    { label: 'Billing', to: '/employer/billing', enabled: false },
  ],
  EMPLOYER_MANAGER: [
    { label: 'Dashboard', to: '/employer', enabled: false },
    { label: 'Jobs', to: '/employer/jobs', enabled: true },
    { label: 'Interviews', to: '/employer/interviews', enabled: false },
    { label: 'Analytics', to: '/employer/analytics', enabled: false },
  ],
  INTERVIEWER: [{ label: 'Interviews', to: '/employer/interviews', enabled: true }],
  ADMIN: [
    { label: 'Dashboard', to: '/admin', enabled: true },
    { label: 'Users', to: '/admin/users', enabled: false },
    { label: 'Audit log', to: '/admin/audit', enabled: false },
    { label: 'Content', to: '/admin/content', enabled: false },
  ],
};
