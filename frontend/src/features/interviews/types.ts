export interface PackQuestion {
  id: string;
  text: string;
}

export interface PackRubricItem {
  id: string;
  criterion: string;
  description: string;
}

export interface InterviewPack {
  questions: PackQuestion[];
  rubric: PackRubricItem[];
  screeningQuestions: string[];
  provider: string | null;
}

export interface InterviewerRef {
  id: string;
  name: string;
}

export type Recommendation = 'strong_yes' | 'yes' | 'no' | 'strong_no';

export interface FeedbackView {
  interviewerId: string | null;
  interviewerName: string | null;
  scores: Record<string, number>;
  comments: string | null;
  recommendation: Recommendation;
}

export interface InterviewListItem {
  id: string;
  scheduledAt: string;
  candidateName: string | null;
  jobTitle: string;
  interviewers: InterviewerRef[];
  myFeedbackSubmitted: boolean;
}

export interface InterviewDetail {
  id: string;
  scheduledAt: string;
  durationMinutes: number;
  meetingLink: string;
  candidate: { firstName: string; lastName: string } | null;
  applicationId: string;
  jobId: string;
  jobTitle: string;
  interviewers: { id: string; name: string; role: string }[];
  pack: InterviewPack;
  myFeedback: FeedbackView | null;
  allFeedback: FeedbackView[] | null;
}

export interface InterviewFeedbackInput {
  scores: Record<string, number>;
  comments: string | null;
  recommendation: Recommendation;
}

export interface ScheduleInterviewInput {
  scheduledAt: string;
  durationMinutes: number;
  meetingLink: string;
  interviewerIds: string[];
}
