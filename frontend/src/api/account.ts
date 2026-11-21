import { api } from './client';
import type { UserDto } from '../features/auth/types';

export async function changePassword(currentPassword: string, newPassword: string): Promise<void> {
  await api.post('/api/account/change-password', { currentPassword, newPassword });
}

export async function uploadProfilePhoto(file: File): Promise<UserDto> {
  const form = new FormData();
  form.append('file', file);
  const { data } = await api.post<UserDto>('/api/account/photo', form);
  return data;
}

export async function deleteProfilePhoto(): Promise<UserDto> {
  const { data } = await api.delete<UserDto>('/api/account/photo');
  return data;
}
