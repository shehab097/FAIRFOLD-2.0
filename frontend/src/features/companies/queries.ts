import { useQuery } from '@tanstack/react-query';
import { getPublicCompany } from '../../api/publicCompanies';

export function usePublicCompany(id: string | undefined) {
  return useQuery({
    queryKey: ['public-company', id],
    queryFn: () => getPublicCompany(id as string),
    enabled: Boolean(id),
  });
}
