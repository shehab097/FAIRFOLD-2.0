export interface QuestionOption {
  id: string;
  text: string;
}

export interface AssessmentQuestion {
  id: string;
  position: number;
  text: string;
  options: QuestionOption[];
  correctOptionId: string;
}

export type AssessmentReviewStatus =
  | 'pending_generation'
  | 'pending_review'
  | 'approved'
  | 'ignored';

export interface AssessmentListItem {
  id: string;
  skillId: string;
  title: string;
  skillName: string | null;
  questionCount: number;
  durationMinutes: number;
  passMark: number;
  active: boolean;
  attemptCount: number;
  reviewStatus: AssessmentReviewStatus;
  poolSize: number;
  questionsPerAttempt: number;
  candidateDemandCount: number;
}

export interface AssessmentDetail {
  id: string;
  title: string;
  skillId: string;
  skillName: string | null;
  durationMinutes: number;
  passMark: number;
  active: boolean;
  attemptCount: number;
  reviewStatus: AssessmentReviewStatus;
  poolSize: number;
  questionsPerAttempt: number;
  candidateDemandCount: number;
  questions: AssessmentQuestion[];
}

export interface AssessmentQuestionInput {
  text: string;
  options: QuestionOption[];
  correctOptionId: string;
}

export interface AssessmentInput {
  title: string;
  skillId: string;
  durationMinutes: number;
  passMark: number;
  active: boolean;
  questionsPerAttempt: number;
  questions: AssessmentQuestionInput[];
}

export interface SkillMergeResult {
  sourceSkillId: string;
  targetSkillId: string;
  movedCandidateSkills: number;
  removedCandidateSkills: number;
  movedJobSkills: number;
  removedJobSkills: number;
  movedResources: number;
  removedAssessments: number;
  movedAssessment: boolean;
}

export interface SkillRescanResult {
  scanned: number;
  renamed: number;
  merged: number;
  skipped: number;
  skippedSkills: string[];
}

export type ResourceCost = 'free' | 'paid';
export type ResourceLevel = 'beginner' | 'intermediate' | 'advanced';

export interface LearningResource {
  id: string;
  skillId: string;
  skillName: string | null;
  title: string;
  url: string;
  cost: ResourceCost;
  language: string;
  level: ResourceLevel;
  active: boolean;
  broken: boolean;
  lastCheckedAt: string | null;
}

export interface ResourceInput {
  skillId: string;
  title: string;
  url: string;
  cost: ResourceCost;
  language: string;
  level: ResourceLevel;
  active: boolean;
}

export interface LinkCheckResult {
  checked: number;
  broken: number;
}
