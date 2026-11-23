import { api } from './client';
import type { PageResponse } from './types';
import type { Billing, PaymentItem } from '../features/employer/billingTypes';

export async function getBilling(): Promise<Billing> {
  const { data } = await api.get<Billing>('/api/employer/billing');
  return data;
}

export async function listPayments(page: number): Promise<PageResponse<PaymentItem>> {
  const { data } = await api.get<PageResponse<PaymentItem>>('/api/employer/billing/payments', {
    params: { page, size: 20 },
  });
  return data;
}

export async function checkout(planId: string): Promise<{ redirectUrl: string }> {
  const { data } = await api.post<{ redirectUrl: string }>('/api/employer/billing/checkout', {
    planId,
  });
  return data;
}
