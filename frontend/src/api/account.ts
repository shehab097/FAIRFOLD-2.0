import { api } from './client';

export async function changePassword(currentPassword: string, newPassword: string): Promise<void> {
  await api.post('/api/account/change-password', { currentPassword, newPassword });
}
