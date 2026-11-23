export interface BillingCurrentPlan {
  name: string;
  monthlyScreenings: number;
  endsAt: string | null;
}

export interface BillingPlan {
  id: string;
  name: string;
  monthlyScreenings: number;
  priceBdt: number;
  durationDays: number | null;
}

export interface Billing {
  plan: BillingCurrentPlan;
  used: number;
  quota: number;
  plans: BillingPlan[];
}

export interface PaymentItem {
  id: string;
  planName: string;
  amountBdt: number;
  status: 'initiated' | 'completed' | 'failed' | 'cancelled';
  createdAt: string;
}
