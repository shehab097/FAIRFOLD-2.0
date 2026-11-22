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
  ResumeSuggestions,
  WorkHistory,
  WorkHistoryInput,
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

export async function getResumeFileBlob(): Promise<Blob> {
  const { data } = await api.get<Blob>('/api/candidate/resume/file', {
    responseType: 'blob',
  });
  return data;
}

export async function uploadResume(file: File): Promise<Resume> {
  const form = new FormData();
  form.append('file', file);
  const { data } = await api.post<Resume>('/api/candidate/resume', form);
  return data;
}

export async function getResumeSuggestions(): Promise<ResumeSuggestions> {
  const { data } = await api.get<ResumeSuggestions>('/api/candidate/resume/suggestions');
  return data;
}

export async function applyResumeSuggestions(input: {
  skills: { name: string; level: string; years: number | null }[];
  totalExperienceYears: number | null;
  educationLevel: string | null;
  jobTitles: string[];
  workHistory: WorkHistoryInput[];
}): Promise<void> {
  await api.post('/api/candidate/resume/suggestions/apply', input);
}

export async function reportWrongStrip(input: { note: string; excerpt?: string }): Promise<void> {
  await api.post('/api/candidate/resume/strip-reports', input);
}

// --- work history ---

export async function listWorkHistory(): Promise<WorkHistory[]> {
  const { data } = await api.get<WorkHistory[]>('/api/candidate/work-history');
  return data;
}

export async function createWorkHistory(input: WorkHistoryInput): Promise<WorkHistory> {
  const { data } = await api.post<WorkHistory>('/api/candidate/work-history', input);
  return data;
}

export async function updateWorkHistory(id: string, input: WorkHistoryInput): Promise<WorkHistory> {
  const { data } = await api.put<WorkHistory>(`/api/candidate/work-history/${id}`, input);
  return data;
}

export async function deleteWorkHistory(id: string): Promise<void> {
  await api.delete(`/api/candidate/work-history/${id}`);
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
