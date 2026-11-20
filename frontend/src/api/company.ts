import { api } from './client';
import type { CompanyProfile } from '../features/jobs/types';

export async function getCompany(): Promise<CompanyProfile> {
  const { data } = await api.get<CompanyProfile>('/employer/company');
  return data;
}

export interface CompanyInput {
  name: string;
  industry: string;
  size: string;
  showNamePublicly: boolean;
}

export async function updateCompany(input: CompanyInput): Promise<CompanyProfile> {
  const { data } = await api.put<CompanyProfile>('/employer/company', input);
  return data;
}
