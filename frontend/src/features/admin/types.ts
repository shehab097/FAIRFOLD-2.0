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

export interface AssessmentListItem {
  id: string;
  title: string;
  skillName: string | null;
  questionCount: number;
  durationMinutes: number;
  passMark: number;
  active: boolean;
  attemptCount: number;
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
  questions: AssessmentQuestionInput[];
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
