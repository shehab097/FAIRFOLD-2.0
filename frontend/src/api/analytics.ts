import { api } from './client';
import type { Analytics, AnalyticsFilters } from '../features/employer/analyticsTypes';

export async function getAnalytics(filters: AnalyticsFilters): Promise<Analytics> {
  const { data } = await api.get<Analytics>('/api/employer/analytics', { params: filters });
  return data;
}
