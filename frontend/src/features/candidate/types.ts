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
  previousJobTitles: string[];
}

export interface CandidateProfileInput {
  phone: string;
  title: string;
  bio: string;
  location: string;
  remotePreference: RemotePreference;
  totalExperienceYears: number | null;
  educationLevel: string | null;
  previousJobTitles?: string[];
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
  failureMessage: string | null;
  strippedText: string | null;
  droppedPlaceholders: DroppedPlaceholder[];
  uploadedAt: string;
}

export interface DroppedPlaceholder {
  placeholder: string;
  kind: string;
  originalValue: string;
}

export interface ResumeSuggestionSkill {
  name: string;
  skillId: string | null;
  level: CandidateSkillLevel;
  years: number | null;
}

export interface ResumeSuggestionWork {
  title: string;
  employer: string | null;
  startMonth: string | null;
  endMonth: string | null;
  isCurrent: boolean;
  skillsUsed: string[];
}

export interface ResumeSuggestions {
  provider: 'ai' | 'rule_based';
  skills: ResumeSuggestionSkill[];
  totalExperienceYears: number | null;
  educationLevel: string | null;
  jobTitles: string[];
  workHistory: ResumeSuggestionWork[];
}

export interface Certification {
  id: string;
  name: string;
  issuer: string;
  issuedOn: string | null;
  expiresOn: string | null;
  credentialId: string | null;
}

export interface CertificationInput {
  name: string;
  issuer: string;
  issuedOn: string | null;
  expiresOn: string | null;
  credentialId: string | null;
}

export interface WorkHistory {
  id: string;
  title: string;
  employer: string | null;
  startMonth: string | null;
  endMonth: string | null;
  isCurrent: boolean;
  skillsUsed: string[];
}

export interface WorkHistoryInput {
  title: string;
  employer: string | null;
  startMonth: string | null;
  endMonth: string | null;
  isCurrent: boolean;
  skillsUsed: string[];
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

export interface CandidateApplicationClaim {
  requirement: string;
  kind: string;
  verdict: 'met' | 'partial' | 'not_met';
  quote: string | null;
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
  score: number | null;
  scoringMode: 'ai' | 'rule_based' | null;
  claims: CandidateApplicationClaim[];
  missingSkills: string[];
  notMatchedRule: string | null;
  gateStatus: string;
  requiredAssessments: CandidateRequiredAssessment[];
  interviews: CandidateInterviewRef[];
}

export interface CandidateInterviewRef {
  scheduledAt: string;
  durationMinutes: number;
  meetingLink: string;
}

export interface CandidateAssessmentRequirement {
  applicationId: string;
  jobId: string;
  jobTitle: string;
  assessmentId: string;
  assessmentTitle: string | null;
  minScore: number;
  bestScore: number | null;
  status: 'pending' | 'failed' | 'passed';
}

export interface CandidateRequiredAssessment {
  assessmentId: string;
  title: string | null;
  minScore: number;
  bestScore: number | null;
  status: 'pending' | 'failed' | 'passed';
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

export interface MyApplication {
  applied: boolean;
  applicationId: string | null;
  status: ApplicationStatus | null;
  appliedAt: string | null;
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
