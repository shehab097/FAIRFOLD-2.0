import {
  BarChart3,
  Briefcase,
  Building2,
  CreditCard,
  FileText,
  FolderKanban,
  GraduationCap,
  LayoutDashboard,
  Search,
  Shield,
  Sparkles,
  User,
  UserCheck,
  Users,
  Video,
  type LucideIcon,
} from 'lucide-react';
import type { Role } from '../features/auth/types';

export interface NavItem {
  label: string;
  to: string;
  enabled: boolean;
  icon?: LucideIcon;
  end?: boolean;
}

/**
 * Sidebar config per role. Items with `enabled: false` exist in the product
 * plan but have no page yet; the PortalShell only renders enabled items.
 */
export const SIDEBAR_ITEMS: Record<Role, NavItem[]> = {
  CANDIDATE: [
    { label: 'Dashboard', to: '/candidate', enabled: true, icon: LayoutDashboard, end: true },
    { label: 'Browse jobs', to: '/jobs', enabled: true, icon: Search, end: true },
    { label: 'Applications', to: '/candidate/applications', enabled: true, icon: FolderKanban },
    { label: 'Resume', to: '/candidate/resume', enabled: true, icon: FileText },
    { label: 'Profile', to: '/candidate/profile', enabled: true, icon: User },
    { label: 'Journey', to: '/candidate/journey', enabled: true, icon: Sparkles },
    { label: 'Assessments', to: '/candidate/assessments', enabled: true, icon: GraduationCap },
    { label: 'Practice', to: '/candidate/practice', enabled: true, icon: Sparkles },
  ],
  EMPLOYER_HR: [
    { label: 'Dashboard', to: '/employer', enabled: true, icon: LayoutDashboard, end: true },
    { label: 'Jobs', to: '/employer/jobs', enabled: true, icon: Briefcase },
    { label: 'Shortlisted', to: '/employer/shortlisted', enabled: true, icon: UserCheck },
    { label: 'Company', to: '/employer/company', enabled: true, icon: Building2 },
    { label: 'Browse jobs', to: '/jobs', enabled: true, icon: Search, end: true },
    { label: 'Interviews', to: '/employer/interviews', enabled: true, icon: Video },
    { label: 'Analytics', to: '/employer/analytics', enabled: true, icon: BarChart3 },
    { label: 'Team', to: '/employer/team', enabled: true, icon: Users },
    { label: 'Billing', to: '/employer/billing', enabled: true, icon: CreditCard },
  ],
  EMPLOYER_MANAGER: [
    { label: 'Dashboard', to: '/employer', enabled: true, icon: LayoutDashboard, end: true },
    { label: 'Jobs', to: '/employer/jobs', enabled: true, icon: Briefcase },
    { label: 'Shortlisted', to: '/employer/shortlisted', enabled: true, icon: UserCheck },
    { label: 'Browse jobs', to: '/jobs', enabled: true, icon: Search, end: true },
    { label: 'Interviews', to: '/employer/interviews', enabled: true, icon: Video },
    { label: 'Analytics', to: '/employer/analytics', enabled: true, icon: BarChart3 },
  ],
  INTERVIEWER: [
    { label: 'Interviews', to: '/employer/interviews', enabled: true, icon: Video },
  ],
  ADMIN: [
    { label: 'Dashboard', to: '/admin', enabled: true, icon: LayoutDashboard, end: true },
    { label: 'Content', to: '/admin/content', enabled: true, icon: FileText },
    { label: 'Browse jobs', to: '/jobs', enabled: true, icon: Search, end: true },
    { label: 'Users', to: '/admin/users', enabled: true, icon: Users },
    { label: 'Audit log', to: '/admin/audit', enabled: true, icon: Shield },
  ],
};
