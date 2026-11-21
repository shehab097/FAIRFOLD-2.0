import { api } from './client';
import type { PublicCompanyProfile } from '../features/jobs/types';

export async function getPublicCompany(id: string): Promise<PublicCompanyProfile> {
  const { data } = await api.get<PublicCompanyProfile>(`/api/companies/${id}`);
  return data;
}
