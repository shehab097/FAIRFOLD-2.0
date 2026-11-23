import { useQuery } from '@tanstack/react-query';
import { getAnalytics } from '../../api/analytics';
import type { AnalyticsFilters } from './analyticsTypes';

export function useAnalytics(filters: AnalyticsFilters) {
  return useQuery({
    queryKey: ['employer', 'analytics', filters],
    queryFn: () => getAnalytics(filters),
  });
}
