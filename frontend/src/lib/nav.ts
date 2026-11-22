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
    { label: 'Journey', to: '/candidate/journey', enabled: false, icon: Sparkles },
    { label: 'Assessments', to: '/candidate/assessments', enabled: false, icon: GraduationCap },
    { label: 'Practice', to: '/candidate/practice', enabled: false, icon: Sparkles },
  ],
  EMPLOYER_HR: [
    { label: 'Dashboard', to: '/employer', enabled: true, icon: LayoutDashboard, end: true },
    { label: 'Jobs', to: '/employer/jobs', enabled: true, icon: Briefcase },
    { label: 'Company', to: '/employer/company', enabled: true, icon: Building2 },
    { label: 'Browse jobs', to: '/jobs', enabled: true, icon: Search, end: true },
    { label: 'Interviews', to: '/employer/interviews', enabled: false, icon: Video },
    { label: 'Analytics', to: '/employer/analytics', enabled: false, icon: BarChart3 },
    { label: 'Team', to: '/employer/team', enabled: false, icon: Users },
    { label: 'Billing', to: '/employer/billing', enabled: false, icon: CreditCard },
  ],
  EMPLOYER_MANAGER: [
    { label: 'Dashboard', to: '/employer', enabled: true, icon: LayoutDashboard, end: true },
    { label: 'Jobs', to: '/employer/jobs', enabled: true, icon: Briefcase },
    { label: 'Browse jobs', to: '/jobs', enabled: true, icon: Search, end: true },
    { label: 'Interviews', to: '/employer/interviews', enabled: false, icon: Video },
    { label: 'Analytics', to: '/employer/analytics', enabled: false, icon: BarChart3 },
  ],
  INTERVIEWER: [
    { label: 'Interviews', to: '/employer/interviews', enabled: true, icon: Video },
    { label: 'Browse jobs', to: '/jobs', enabled: true, icon: Search, end: true },
  ],
  ADMIN: [
    { label: 'Dashboard', to: '/admin', enabled: true, icon: LayoutDashboard, end: true },
    { label: 'Browse jobs', to: '/jobs', enabled: true, icon: Search, end: true },
    { label: 'Users', to: '/admin/users', enabled: false, icon: Users },
    { label: 'Audit log', to: '/admin/audit', enabled: false, icon: Shield },
    { label: 'Content', to: '/admin/content', enabled: false, icon: FileText },
  ],
};
