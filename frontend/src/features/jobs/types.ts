export type ExperienceLevel = 'entry' | 'mid' | 'senior' | 'lead';
export type JobStatus = 'draft' | 'active' | 'paused' | 'closed';

export interface PublicJobListItem {
  id: string;
  title: string;
  city: string | null;
  country: string;
  remote: boolean;
  experienceLevel: ExperienceLevel;
  topSkills: string[];
  publishedAt: string;
  companyName: string | null;
}

export interface PublicJobDetail {
  id: string;
  title: string;
  description: string;
  requirements: string;
  city: string | null;
  country: string;
  remote: boolean;
  experienceLevel: ExperienceLevel;
  requiredSkills: string[];
  niceToHaveSkills: string[];
  publishedAt: string;
  companyName: string | null;
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

export interface EmployerJobDetail {
  id: string;
  title: string;
  description: string;
  requirements: string;
  experienceLevel: ExperienceLevel;
  city: string | null;
  country: string;
  remote: boolean;
  requiredSkills: SkillRef[];
  niceToHaveSkills: SkillRef[];
  hfMinYears: number | null;
  hfCountry: string | null;
  cutoffScore: number | null;
  status: JobStatus;
  publishedAt: string | null;
}

export interface JobInput {
  title: string;
  description: string;
  requirements: string;
  experienceLevel: ExperienceLevel;
  city: string;
  country: string;
  remote: boolean;
  requiredSkillIds: string[];
  niceToHaveSkillIds: string[];
  mustHaveSkillIds: string[];
  hfMinYears: number | null;
  hfCountry: string;
  cutoffScore: number | null;
}

export interface JobFilters {
  q?: string;
  location?: string;
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
}export interface SkillDto {
  id: string;
  name: string;
}

export const COMPANY_SIZES = ['1-10', '11-50', '51-200', '201-1000', '1000+'] as const;
export const EXPERIENCE_LEVELS: ExperienceLevel[] = ['entry', 'mid', 'senior', 'lead'];

export interface EmployerApplicantListItem {
  id: string;
  label: string;
  status: string;
  appliedAt: string;
}
