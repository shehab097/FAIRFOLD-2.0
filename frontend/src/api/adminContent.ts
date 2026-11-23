import { api } from './client';
import type { PageResponse } from './types';
import type {
  AssessmentDetail,
  AssessmentInput,
  AssessmentListItem,
  LearningResource,
  LinkCheckResult,
  ResourceInput,
  SkillMergeResult,
  SkillRescanResult,
} from '../features/admin/types';

export async function listAssessments(page: number): Promise<PageResponse<AssessmentListItem>> {
  const { data } = await api.get<PageResponse<AssessmentListItem>>('/api/admin/assessments', {
    params: { page },
  });
  return data;
}

export async function getAssessment(id: string): Promise<AssessmentDetail> {
  const { data } = await api.get<AssessmentDetail>(`/api/admin/assessments/${id}`);
  return data;
}

export async function createAssessment(input: AssessmentInput): Promise<AssessmentDetail> {
  const { data } = await api.post<AssessmentDetail>('/api/admin/assessments', input);
  return data;
}

export async function updateAssessment(
  id: string,
  input: AssessmentInput,
): Promise<AssessmentDetail> {
  const { data } = await api.put<AssessmentDetail>(`/api/admin/assessments/${id}`, input);
  return data;
}

export async function setAssessmentActive(
  id: string,
  active: boolean,
): Promise<AssessmentDetail> {
  const { data } = await api.patch<AssessmentDetail>(`/api/admin/assessments/${id}/active`, {
    active,
  });
  return data;
}

export async function generateAssessment(id: string, count = 30): Promise<AssessmentDetail> {
  const { data } = await api.post<AssessmentDetail>(`/api/admin/assessments/${id}/generate`, null, {
    params: { count },
  });
  return data;
}

export async function approveAssessment(id: string): Promise<AssessmentDetail> {
  const { data } = await api.post<AssessmentDetail>(`/api/admin/assessments/${id}/approve`);
  return data;
}

export async function mergeSkill(
  sourceSkillId: string,
  targetSkillId: string,
): Promise<SkillMergeResult> {
  const { data } = await api.post<SkillMergeResult>(
    `/api/admin/skills/${sourceSkillId}/merge`,
    { targetSkillId },
  );
  return data;
}

export async function rescanSkills(): Promise<SkillRescanResult> {
  const { data } = await api.post<SkillRescanResult>('/api/admin/skills/rescan');
  return data;
}

export async function listResources(
  page: number,
  skillId?: string,
): Promise<PageResponse<LearningResource>> {
  const { data } = await api.get<PageResponse<LearningResource>>('/api/admin/resources', {
    params: { page, skillId },
  });
  return data;
}

export async function createResource(input: ResourceInput): Promise<LearningResource> {
  const { data } = await api.post<LearningResource>('/api/admin/resources', input);
  return data;
}

export async function updateResource(
  id: string,
  input: ResourceInput,
): Promise<LearningResource> {
  const { data } = await api.put<LearningResource>(`/api/admin/resources/${id}`, input);
  return data;
}

export async function setResourceActive(
  id: string,
  active: boolean,
): Promise<LearningResource> {
  const { data } = await api.patch<LearningResource>(`/api/admin/resources/${id}/active`, {
    active,
  });
  return data;
}

export async function checkResourceLinks(): Promise<LinkCheckResult> {
  const { data } = await api.post<LinkCheckResult>('/api/admin/resources/check');
  return data;
}
