import { api } from './client';
import type { CompanyProfile } from '../features/jobs/types';

export async function getCompany(): Promise<CompanyProfile> {
  const { data } = await api.get<CompanyProfile>('/api/employer/company');
  return data;
}

export interface CompanyInput {
  name: string;
  industry: string;
  size: string;
  showNamePublicly: boolean;
}

export async function updateCompany(input: CompanyInput): Promise<CompanyProfile> {
  const { data } = await api.put<CompanyProfile>('/api/employer/company', input);
  return data;
}
