import { api } from './client';
import type { Certification, CertificationInput } from '../features/candidate/types';

export async function listCertifications(): Promise<Certification[]> {
  const { data } = await api.get<Certification[]>('/api/candidate/certifications');
  return data;
}

export async function createCertification(input: CertificationInput): Promise<Certification> {
  const { data } = await api.post<Certification>('/api/candidate/certifications', input);
  return data;
}

export async function updateCertification(
  id: string,
  input: CertificationInput,
): Promise<Certification> {
  const { data } = await api.put<Certification>(`/api/candidate/certifications/${id}`, input);
  return data;
}

export async function deleteCertification(id: string): Promise<void> {
  await api.delete(`/api/candidate/certifications/${id}`);
}
