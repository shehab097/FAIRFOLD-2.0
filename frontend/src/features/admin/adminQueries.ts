import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  changeUserRole,
  getAdminStats,
  listAdminUsers,
  listAudit,
  resetUserMfa,
  sendUserPasswordReset,
  unlockUser,
} from '../../api/admin';
import type { AdminUserFilters, AuditFilters } from './adminTypes';

export function useAdminStats() {
  return useQuery({ queryKey: ['admin', 'stats'], queryFn: getAdminStats });
}

export function useAdminUsers(filters: AdminUserFilters) {
  return useQuery({
    queryKey: ['admin', 'users', filters],
    queryFn: () => listAdminUsers(filters),
    placeholderData: keepPreviousData,
  });
}

export function useChangeUserRole() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, role }: { id: string; role: string }) => changeUserRole(id, role),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['admin', 'users'] }),
  });
}

export function useUnlockUser() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: unlockUser,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['admin', 'users'] }),
  });
}

export function useSendPasswordReset() {
  return useMutation({ mutationFn: sendUserPasswordReset });
}

export function useResetUserMfa() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: resetUserMfa,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['admin', 'users'] }),
  });
}

export function useAudit(filters: AuditFilters, page: number) {
  return useQuery({
    queryKey: ['admin', 'audit', filters, page],
    queryFn: () => listAudit(filters, page),
    placeholderData: keepPreviousData,
  });
}
