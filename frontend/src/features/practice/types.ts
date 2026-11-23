export interface PracticeQuestionOption {
  id: string;
  text: string;
  topic: string;
}

export interface StarPoint {
  comment: string | null;
  quote: string;
}

export interface StarComponent {
  rating: number;
  points: StarPoint[];
}

export interface StarFeedback {
  situation: StarComponent;
  task: StarComponent;
  action: StarComponent;
  result: StarComponent;
  summary: string;
}

export interface PracticeSession {
  id: string;
  questionId: string;
  questionText: string | null;
  topic: string | null;
  answer: string;
  feedbackAvailable: boolean;
  feedback: StarFeedback | null;
  rubricVersion: string;
  provider: string | null;
  rating: number | null;
  createdAt: string;
}

export interface PracticeSessionListItem {
  id: string;
  questionId: string;
  questionText: string | null;
  topic: string | null;
  rating: number | null;
  feedbackAvailable: boolean;
  createdAt: string;
}

export interface AdminPracticeQuestion {
  id: string;
  text: string;
  topic: string;
  active: boolean;
}
