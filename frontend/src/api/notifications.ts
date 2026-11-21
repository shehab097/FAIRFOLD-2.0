import { api } from './client';
import type { PageResponse } from './types';
import type { NotificationItem, UnreadCount } from '../features/notifications/types';

export async function listNotifications(params: {
  page?: number;
  size?: number;
}): Promise<PageResponse<NotificationItem>> {
  const { data } = await api.get<PageResponse<NotificationItem>>('/api/notifications', { params });
  return data;
}

export async function getUnreadCount(): Promise<UnreadCount> {
  const { data } = await api.get<UnreadCount>('/api/notifications/unread-count');
  return data;
}

export async function markNotificationRead(id: string): Promise<NotificationItem> {
  const { data } = await api.post<NotificationItem>(`/api/notifications/${id}/read`);
  return data;
}

export async function markAllNotificationsRead(): Promise<void> {
  await api.post('/api/notifications/read-all');
}
