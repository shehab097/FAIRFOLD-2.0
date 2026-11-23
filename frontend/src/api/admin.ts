import { api } from './client';
import type { PageResponse } from './types';
import type {
  AdminStats,
  AdminUser,
  AdminUserFilters,
  AuditEntry,
  AuditFilters,
} from '../features/admin/adminTypes';

export async function getAdminStats(): Promise<AdminStats> {
  const { data } = await api.get<AdminStats>('/api/admin/stats');
  return data;
}

export async function listAdminUsers(
  filters: AdminUserFilters,
): Promise<PageResponse<AdminUser>> {
  const { data } = await api.get<PageResponse<AdminUser>>('/api/admin/users', {
    params: {
      q: filters.q || undefined,
      role: filters.role || undefined,
      locked: filters.locked,
      page: filters.page,
      size: 20,
    },
  });
  return data;
}

export async function changeUserRole(id: string, role: string): Promise<AdminUser> {
  const { data } = await api.patch<AdminUser>(`/api/admin/users/${id}/role`, { role });
  return data;
}

export async function unlockUser(id: string): Promise<AdminUser> {
  const { data } = await api.post<AdminUser>(`/api/admin/users/${id}/unlock`);
  return data;
}

export async function sendUserPasswordReset(id: string): Promise<void> {
  await api.post(`/api/admin/users/${id}/password-reset`);
}

export async function listAudit(
  filters: AuditFilters,
  page: number,
): Promise<PageResponse<AuditEntry>> {
  const { data } = await api.get<PageResponse<AuditEntry>>('/api/admin/audit', {
    params: {
      ...filters,
      page,
      size: 25,
    },
  });
  return data;
}

export async function downloadAuditCsv(filters: AuditFilters): Promise<void> {
  const response = await api.get<Blob>('/api/admin/audit', {
    params: { ...filters, format: 'csv' },
    responseType: 'blob',
  });
  const disposition = response.headers['content-disposition'] as string | undefined;
  const match = disposition?.match(/filename="?([^";]+)"?/i);
  const filename = match?.[1] ?? `fairfold-audit-${new Date().toISOString().slice(0, 10)}.csv`;
  const url = URL.createObjectURL(response.data);
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = filename;
  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();
  URL.revokeObjectURL(url);
}
