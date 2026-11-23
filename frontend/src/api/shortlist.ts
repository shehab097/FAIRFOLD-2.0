import { api } from './client';
import type { PageResponse } from './types';
import type { ShortlistedCandidate } from '../features/shortlist/types';

export async function listShortlisted(page: number): Promise<PageResponse<ShortlistedCandidate>> {
  const { data } = await api.get<PageResponse<ShortlistedCandidate>>('/api/employer/shortlisted', {
    params: { page },
  });
  return data;
}
