import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { listShortlisted } from '../../api/shortlist';

export function useShortlisted(page: number) {
  return useQuery({
    queryKey: ['employer', 'shortlisted', page],
    queryFn: () => listShortlisted(page),
    placeholderData: keepPreviousData,
  });
}
