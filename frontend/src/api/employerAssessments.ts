import { api } from './client';
import type { EmployerAssessmentOption } from '../features/jobs/types';

export async function listEmployerAssessments(): Promise<EmployerAssessmentOption[]> {
  const { data } = await api.get<EmployerAssessmentOption[]>('/api/employer/assessments');
  return data;
}
