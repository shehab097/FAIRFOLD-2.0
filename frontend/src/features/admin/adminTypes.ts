export interface AdminStats {
  usersByRole: Record<string, number>;
  jobsByStatus: Record<string, number>;
  applicationsTotal: number;
  aiCallsThisMonth: number;
  aiCostThisMonthUsd: number;
}

export interface AdminUser {
  id: string;
  name: string;
  email: string;
  role: string;
  companyId: string | null;
  locked: boolean;
  disabled: boolean;
  mfaEnabled: boolean;
  createdAt: string;
}

export interface AuditEntry {
  id: string;
  createdAt: string;
  actorId: string | null;
  actorEmail: string | null;
  action: string;
  resourceType: string;
  resourceId: string | null;
  details: unknown;
}

export interface AdminUserFilters {
  q?: string;
  role?: string;
  locked?: boolean;
  page: number;
}

export interface AuditFilters {
  action?: string;
  resourceType?: string;
  actorId?: string;
  from?: string;
  to?: string;
}
