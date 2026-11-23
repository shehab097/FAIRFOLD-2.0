import { api } from './client';
import type { PageResponse } from './types';
import type {
  FeedbackView,
  InterviewDetail,
  InterviewFeedbackInput,
  InterviewListItem,
  InterviewPack,
  ScheduleInterviewInput,
} from '../features/interviews/types';

export async function getInterviewPack(jobId: string): Promise<InterviewPack> {
  const { data } = await api.get<InterviewPack>(`/api/employer/jobs/${jobId}/interview-pack`);
  return data;
}

export async function generateInterviewPack(jobId: string): Promise<InterviewPack> {
  const { data } = await api.post<InterviewPack>(
    `/api/employer/jobs/${jobId}/interview-pack/generate`,
  );
  return data;
}

export async function saveInterviewPack(
  jobId: string,
  input: { questions: { id: string; text: string }[]; rubric: { id: string; criterion: string; description: string }[] },
): Promise<InterviewPack> {
  const { data } = await api.put<InterviewPack>(
    `/api/employer/jobs/${jobId}/interview-pack`,
    input,
  );
  return data;
}

export async function scheduleInterview(
  applicationId: string,
  input: ScheduleInterviewInput,
): Promise<InterviewDetail> {
  const { data } = await api.post<InterviewDetail>(
    `/api/employer/applications/${applicationId}/interviews`,
    input,
  );
  return data;
}

export async function listInterviews(
  tab: 'upcoming' | 'past',
  page: number,
): Promise<PageResponse<InterviewListItem>> {
  const { data } = await api.get<PageResponse<InterviewListItem>>('/api/interviews', {
    params: { tab, page },
  });
  return data;
}

export async function getInterview(id: string): Promise<InterviewDetail> {
  const { data } = await api.get<InterviewDetail>(`/api/interviews/${id}`);
  return data;
}

export async function submitInterviewFeedback(
  id: string,
  input: InterviewFeedbackInput,
): Promise<FeedbackView> {
  const { data } = await api.put<FeedbackView>(`/api/interviews/${id}/feedback`, input);
  return data;
}

export async function hireApplication(id: string): Promise<void> {
  await api.post(`/api/employer/applications/${id}/hire`);
}
