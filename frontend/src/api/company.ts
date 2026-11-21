import { api } from './client';
import type { CompanyProfile } from '../features/jobs/types';

export async function getCompany(): Promise<CompanyProfile> {
  const { data } = await api.get<CompanyProfile>('/api/employer/company');
  return data;
}

export type CompanyInput = CompanyProfile;

export async function updateCompany(input: CompanyInput): Promise<CompanyProfile> {
  const { data } = await api.put<CompanyProfile>('/api/employer/company', input);
  return data;
}

export async function uploadCompanyLogo(file: File): Promise<CompanyProfile> {
  const form = new FormData();
  form.append('file', file);
  const { data } = await api.post<CompanyProfile>('/api/employer/company/logo', form);
  return data;
}

export async function deleteCompanyLogo(): Promise<CompanyProfile> {
  const { data } = await api.delete<CompanyProfile>('/api/employer/company/logo');
  return data;
}
