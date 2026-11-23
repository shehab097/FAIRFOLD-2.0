export interface CandidateAssessment {
  id: string;
  title: string;
  skillName: string | null;
  questionCount: number;
  durationMinutes: number;
  passMark: number;
  bestScore: number | null;
  passed: boolean;
  lastAttemptAt: string | null;
  retryAvailableAt: string | null;
  inProgressAttemptId: string | null;
}

export interface AttemptOption {
  id: string;
  text: string;
}

export interface AttemptQuestion {
  id: string;
  position: number;
  text: string;
  options: AttemptOption[];
}

export interface CandidateResource {
  id: string;
  title: string;
  url: string;
  cost: 'free' | 'paid';
  language: string;
  level: string;
}

export type AttemptStatus = 'in_progress' | 'submitted' | 'auto_submitted';

export interface AttemptView {
  attemptId: string;
  assessmentId: string;
  assessmentTitle: string;
  skillName: string | null;
  endsAt?: string | null;
  serverTime: string;
  questions?: AttemptQuestion[];
  answers?: Record<string, string> | null;
  score?: number | null;
  passed?: boolean | null;
  passMark?: number;
  correctCount?: number;
  totalQuestions?: number;
  status: AttemptStatus;
  skillVerified?: boolean;
  retryAvailableAt?: string | null;
  resources?: CandidateResource[];
}

export interface AttemptHistoryItem {
  id: string;
  assessmentTitle: string;
  score: number | null;
  passed: boolean | null;
  status: AttemptStatus;
  submittedAt: string | null;
}
