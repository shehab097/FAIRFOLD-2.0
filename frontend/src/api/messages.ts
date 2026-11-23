import { api } from './client';
import type { Message } from '../features/messages/types';

export async function getMessages(applicationId: string, afterId?: string): Promise<Message[]> {
  const { data } = await api.get<Message[]>(`/api/applications/${applicationId}/messages`, {
    params: afterId ? { afterId } : undefined,
  });
  return data;
}

export async function sendMessage(applicationId: string, body: string): Promise<Message> {
  const { data } = await api.post<Message>(`/api/applications/${applicationId}/messages`, { body });
  return data;
}
