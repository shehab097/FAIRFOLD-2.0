export type ExperienceLevel = 'entry' | 'mid' | 'senior' | 'lead';
export type JobStatus = 'draft' | 'active' | 'paused' | 'closed';
export type SalaryType = 'fixed' | 'range' | 'negotiable';
export type SalaryPeriod = 'year' | 'month' | 'hour';

export interface SalaryFields {
  salaryType: SalaryType;
  salaryMin: number | null;
  salaryMax: number | null;
  salaryCurrency: string;
  salaryPeriod: SalaryPeriod;
  salaryNegotiable: boolean;
}

export interface PublicJobListItem extends SalaryFields {
  id: string;
  title: string;
  city: string | null;
  country: string;
  remote: boolean;
  experienceLevel: ExperienceLevel;
  topSkills: string[];
  publishedAt: string;
  companyName: string | null;
  companyId: string | null;
  companyLogoUrl: string | null;
}

export interface PublicJobDetail extends SalaryFields {
  id: string;
  title: string;
  description: string;
  requirements: string;
  city: string | null;
  address: string | null;
  country: string;
  remote: boolean;
  experienceLevel: ExperienceLevel;
  requiredSkills: string[];
  niceToHaveSkills: string[];
  publishedAt: string;
  companyName: string | null;
  companyId: string | null;
  companyLogoUrl: string | null;
}

export interface PublicCompanyProfile {
  id: string;
  name: string | null;
  industry: string | null;
  size: string | null;
  tagline: string | null;
  /** Markdown */
  about: string | null;
  website: string | null;
  logoUrl: string | null;
  linkedinUrl: string | null;
  foundedYear: number | null;
  headquarters: string | null;
  /** Markdown */
  benefits: string | null;
}

export interface EmployerJobListItem {
  id: string;
  title: string;
  status: JobStatus;
  applicationsCount: number;
  awaitingScreeningCount: number;
  createdAt: string;
}

export interface SkillRef {
  id: string;
  name: string;
  mustHave: boolean;
}

export interface EmployerJobDetail extends SalaryFields {
  id: string;
  title: string;
  description: string;
  requirements: string;
  experienceLevel: ExperienceLevel;
  city: string | null;
  address: string | null;
  country: string;
  remote: boolean;
  requiredSkills: SkillRef[];
  niceToHaveSkills: SkillRef[];
  hfMinYears: number | null;
  hfCountry: string | null;
  cutoffScore: number | null;
  status: JobStatus;
  publishedAt: string | null;
  screeningQuestions: string[];
  requiredAssessments: RequiredAssessmentRef[];
}

export interface RequiredAssessmentRef {
  assessmentId: string;
  title: string | null;
  minScore: number;
}

export interface RequiredAssessmentInput {
  assessmentId: string;
  minScore: number;
}

export interface EmployerAssessmentOption {
  id: string;
  title: string;
  skillName: string | null;
  passMark: number;
  durationMinutes: number;
}

export interface JobInput extends SalaryFields {
  title: string;
  description: string;
  requirements: string;
  experienceLevel: ExperienceLevel;
  city: string;
  address: string;
  country: string;
  remote: boolean;
  requiredSkillIds: string[];
  niceToHaveSkillIds: string[];
  mustHaveSkillIds: string[];
  hfMinYears: number | null;
  hfCountry: string;
  cutoffScore: number | null;
  requiredAssessments: RequiredAssessmentInput[];
}

export interface JobFilters {
  q?: string;
  location?: string;
  country?: string;
  companyId?: string;
  salaryMin?: number;
  salaryMax?: number;
  remote?: boolean;
  level?: ExperienceLevel;
  page?: number;
  size?: number;
}

export interface CompanyProfile {
  name: string | null;
  industry: string | null;
  size: string | null;
  showNamePublicly: boolean;
  tagline: string | null;
  /** Markdown */
  about: string | null;
  website: string | null;
  logoUrl: string | null;
  linkedinUrl: string | null;
  foundedYear: number | null;
  headquarters: string | null;
  contactEmail: string | null;
  phone: string | null;
  /** Markdown */
  benefits: string | null;
}

export interface SkillDto {
  id: string;
  name: string;
}

export const COMPANY_SIZES = ['1-10', '11-50', '51-200', '201-1000', '1000+'] as const;
export const EXPERIENCE_LEVELS: ExperienceLevel[] = ['entry', 'mid', 'senior', 'lead'];

export const EXPERIENCE_LABELS: Record<ExperienceLevel, string> = {
  entry: 'Junior',
  mid: 'Mid',
  senior: 'Senior',
  lead: 'Lead',
};
export const SALARY_TYPES: SalaryType[] = ['fixed', 'range', 'negotiable'];
export const SALARY_PERIODS: SalaryPeriod[] = ['year', 'month', 'hour'];

export interface EmployerApplicantListItem {
  id: string;
  alias: string;
  label: string;
  score: number | null;
  status: string;
  biasStatus: string | null;
  gateStatus: string;
  scoringMode: string | null;
  notMatchedRule: string | null;
  appliedAt: string;
}

export interface ApplicantFilters {
  status?: string;
  gate?: string;
  bias?: string;
  includeNotMatched?: boolean;
  page?: number;
  size?: number;
}

export interface EmployerReviewClaim {
  requirement: string;
  kind: string;
  verdict: 'met' | 'partial' | 'not_met';
  quote: string | null;
  source: string;
}

export interface EmployerReviewHistory {
  status: string;
  at: string;
}

export interface EmployerIdentity {
  firstName: string;
  lastName: string;
  email: string;
  phone: string | null;
}

export interface EmployerReview {
  id: string;
  jobId: string;
  jobTitle: string;
  alias: string;
  status: string;
  score: number | null;
  scoringMode: 'ai' | 'rule_based' | null;
  biasStatus: 'clear' | 'flagged' | null;
  biasNote: string | null;
  gateStatus: string;
  cutoffScore: number | null;
  missingSkills: string[];
  claims: EmployerReviewClaim[];
  strippedText: string | null;
  notMatchedRule: string | null;
  history: EmployerReviewHistory[];
  identity: EmployerIdentity | null;
  assessments: EmployerAssessmentSummary[];
  verifiedSkills: string[];
  interviews: EmployerInterviewSummary[];
}

export interface EmployerInterviewSummary {
  id: string;
  scheduledAt: string;
  durationMinutes: number;
  meetingLink: string;
  interviewers: { id: string; name: string }[];
  feedbackCount: number;
}

export interface EmployerAssessmentFocus {
  recorded: boolean;
  leftCount: number;
  totalSeconds: number;
  explanation: string | null;
}

export interface EmployerAssessmentSummary {
  assessmentTitle: string | null;
  minScore: number;
  bestScore: number | null;
  passed: boolean;
  lastAttemptAt: string | null;
  focus: EmployerAssessmentFocus;
}

export interface ScreeningEstimate {
  count: number;
  mode: 'ai' | 'rule_based';
  estimatedCostUsd: number;
}

export interface ScreeningProgress {
  runId: string | null;
  status: 'running' | 'finished' | null;
  mode: 'ai' | 'rule_based' | null;
  total: number;
  done: number;
  failed: number;
  startedAt: string | null;
}

export interface ScreeningQuestions {
  questions: string[];
}

export interface EmployerDashboardPipeline {
  applied: number;
  screening: number;
  screened: number;
  shortlisted: number;
  interview: number;
  hired: number;
  rejected: number;
  notMatched: number;
}

export interface EmployerDashboardApplication {
  id: string;
  jobId: string;
  jobTitle: string | null;
  alias: string;
  score: number | null;
  status: string;
  biasStatus: string | null;
  appliedAt: string;
}

export interface EmployerDashboardJob {
  id: string;
  title: string;
  status: string;
  applicants: number;
  awaitingScreening: number;
  screened: number;
}

export interface EmployerDashboard {
  openJobs: number;
  newApplications: number;
  awaitingScreening: number;
  biasFlags: number;
  interviewsThisWeek: number | null;
  quota: unknown | null;
  pipeline: EmployerDashboardPipeline;
  recentApplications: EmployerDashboardApplication[];
  jobs: EmployerDashboardJob[];
}
