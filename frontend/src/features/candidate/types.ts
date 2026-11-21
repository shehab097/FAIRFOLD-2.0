export type CandidateSkillLevel = 'beginner' | 'intermediate' | 'expert';
export type RemotePreference = 'onsite' | 'hybrid' | 'remote' | 'any';
export type ApplicationStatus =
  | 'applied'
  | 'screening'
  | 'screened'
  | 'not_matched'
  | 'shortlisted'
  | 'interview'
  | 'hired'
  | 'rejected';

export interface CandidateProfile {
  phone: string | null;
  title: string | null;
  bio: string | null;
  location: string | null;
  remotePreference: RemotePreference;
  totalExperienceYears: number | null;
  educationLevel: string | null;
}

export interface CandidateProfileInput {
  phone: string;
  title: string;
  bio: string;
  location: string;
  remotePreference: RemotePreference;
  totalExperienceYears: number | null;
  educationLevel: string | null;
}

export interface CandidateSkill {
  id: string;
  skillId: string;
  name: string;
  level: CandidateSkillLevel;
  years: number | null;
  verified: boolean;
}

export interface Resume {
  id: string;
  filename: string | null;
  sizeBytes: number;
  status: string;
  failureReason: string | null;
  uploadedAt: string;
}

export interface CandidateApplicationListItem {
  id: string;
  jobId: string;
  jobTitle: string | null;
  companyName: string | null;
  companyId: string | null;
  status: ApplicationStatus;
  appliedAt: string;
}

export interface ApplicationHistoryEntry {
  status: ApplicationStatus;
  at: string;
}

export interface CandidateApplicationDetail {
  id: string;
  jobId: string;
  jobTitle: string;
  companyName: string | null;
  companyId: string | null;
  city: string | null;
  country: string;
  status: ApplicationStatus;
  appliedAt: string;
  history: ApplicationHistoryEntry[];
}

export interface DashboardChecklist {
  profileSaved: boolean;
  resumeUploaded: boolean;
  skillsAdded: boolean;
}

export interface LatestApplication {
  id: string;
  jobId: string;
  jobTitle: string | null;
  status: ApplicationStatus;
  appliedAt: string | null;
}

export interface Dashboard {
  checklist: DashboardChecklist;
  latestApplications: LatestApplication[];
}

export const SKILL_LEVELS: CandidateSkillLevel[] = ['beginner', 'intermediate', 'expert'];
export const REMOTE_PREFERENCES: RemotePreference[] = ['onsite', 'hybrid', 'remote', 'any'];
export const EDUCATION_LEVELS = [
  'high_school',
  'diploma',
  'bachelor',
  'master',
  'phd',
  'other',
] as const;
