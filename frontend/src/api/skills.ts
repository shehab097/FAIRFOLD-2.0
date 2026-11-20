import { api } from './client';
import type { SkillDto } from '../features/jobs/types';

export async function suggestSkills(q: string): Promise<SkillDto[]> {
  const { data } = await api.get<SkillDto[]>('/api/skills/suggest', { params: { q } });
  return data;
}
