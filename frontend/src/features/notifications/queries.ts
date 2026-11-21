import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  getUnreadCount,
  listNotifications,
  markAllNotificationsRead,
  markNotificationRead,
} from '../../api/notifications';

export function useNotifications(page: number) {
  return useQuery({
    queryKey: ['notifications', page],
    queryFn: () => listNotifications({ page }),
    placeholderData: keepPreviousData,
  });
}

export function useUnreadCount(options?: { refetchInterval?: number; enabled?: boolean }) {
  return useQuery({
    queryKey: ['notifications', 'unread'],
    queryFn: getUnreadCount,
    refetchInterval: options?.refetchInterval ?? false,
    enabled: options?.enabled ?? true,
  });
}

export function useMarkRead() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: markNotificationRead,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['notifications'] });
    },
  });
}

export function useMarkAllRead() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: markAllNotificationsRead,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['notifications'] }),
  });
}
