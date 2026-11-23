import { api } from './client';
import type { Journey } from '../features/candidate/journeyTypes';

export async function getJourney(): Promise<Journey> {
  const { data } = await api.get<Journey>('/api/candidate/journey');
  return data;
}
