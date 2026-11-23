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

export async function exportAccountData(): Promise<{ blob: Blob; filename: string }> {
  const response = await api.get<Blob>('/api/account/export', { responseType: 'blob' });
  const disposition = response.headers['content-disposition'] as string | undefined;
  const match = disposition?.match(/filename="?([^";]+)"?/i);
  return {
    blob: response.data,
    filename: match?.[1] ?? `fairfold-data-${new Date().toISOString().slice(0, 10)}.json`,
  };
}

export async function deleteAccount(password: string, confirmation: string): Promise<void> {
  await api.delete('/api/account', { data: { password, confirmation } });
}

export interface MfaSetup {
  secret: string;
  otpauthUri: string;
  qrDataUri: string;
}

export async function setupMfa(): Promise<MfaSetup> {
  const { data } = await api.post<MfaSetup>('/api/account/mfa/setup');
  return data;
}

export async function enableMfa(code: string): Promise<void> {
  await api.post('/api/account/mfa/enable', { code });
}

export async function disableMfa(password: string, code: string): Promise<void> {
  await api.post('/api/account/mfa/disable', { password, code });
}
