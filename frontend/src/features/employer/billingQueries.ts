import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { checkout, getBilling, listPayments } from '../../api/billing';

export function useBilling() {
  return useQuery({ queryKey: ['employer', 'billing'], queryFn: getBilling });
}

export function usePayments(page: number) {
  return useQuery({
    queryKey: ['employer', 'billing', 'payments', page],
    queryFn: () => listPayments(page),
    placeholderData: keepPreviousData,
  });
}

export function useCheckout() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: checkout,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['employer', 'billing'] });
      qc.invalidateQueries({ queryKey: ['employer', 'dashboard'] });
    },
  });
}
