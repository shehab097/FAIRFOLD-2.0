import { api } from './client';
import type { PageResponse } from './types';
import type {
  CandidateApplicationDetail,
  CandidateApplicationListItem,
  CandidateProfile,
  CandidateProfileInput,
  CandidateSkill,
  Dashboard,
  Resume,
} from '../features/candidate/types';

// --- profile ---

export async function getProfile(): Promise<CandidateProfile> {
  const { data } = await api.get<CandidateProfile>('/api/candidate/profile');
  return data;
}

export async function updateProfile(input: CandidateProfileInput): Promise<CandidateProfile> {
  const { data } = await api.put<CandidateProfile>('/api/candidate/profile', input);
  return data;
}

// --- skills ---

export async function listSkills(): Promise<CandidateSkill[]> {
  const { data } = await api.get<CandidateSkill[]>('/api/candidate/skills');
  return data;
}

export async function addSkill(input: {
  skillId: string;
  level: string;
  years: number | null;
}): Promise<CandidateSkill> {
  const { data } = await api.post<CandidateSkill>('/api/candidate/skills', input);
  return data;
}

export async function updateSkill(
  id: string,
  input: { level: string; years: number | null },
): Promise<CandidateSkill> {
  const { data } = await api.put<CandidateSkill>(`/api/candidate/skills/${id}`, input);
  return data;
}

export async function deleteSkill(id: string): Promise<void> {
  await api.delete(`/api/candidate/skills/${id}`);
}

// --- resume ---

export async function getResume(): Promise<Resume> {
  const { data } = await api.get<Resume>('/api/candidate/resume');
  return data;
}

export async function uploadResume(file: File): Promise<Resume> {
  const form = new FormData();
  form.append('file', file);
  const { data } = await api.post<Resume>('/api/candidate/resume', form);
  return data;
}

// --- applications ---

export async function applyToJob(jobId: string): Promise<CandidateApplicationDetail> {
  const { data } = await api.post<CandidateApplicationDetail>(`/api/jobs/${jobId}/applications`);
  return data;
}

export async function listApplications(params: {
  status?: string;
  page?: number;
  size?: number;
}): Promise<PageResponse<CandidateApplicationListItem>> {
  const { data } = await api.get<PageResponse<CandidateApplicationListItem>>(
    '/api/candidate/applications',
    { params },
  );
  return data;
}

export async function getApplication(id: string): Promise<CandidateApplicationDetail> {
  const { data } = await api.get<CandidateApplicationDetail>(`/api/candidate/applications/${id}`);
  return data;
}

// --- dashboard ---

export async function getDashboard(): Promise<Dashboard> {
  const { data } = await api.get<Dashboard>('/api/candidate/dashboard');
  return data;
}
