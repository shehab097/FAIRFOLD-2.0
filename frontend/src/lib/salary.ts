import type { SalaryPeriod, SalaryType } from '../features/jobs/types';

export interface SalaryLike {
  salaryType: SalaryType;
  salaryMin: number | null;
  salaryMax: number | null;
  salaryCurrency: string;
  salaryPeriod: SalaryPeriod;
  salaryNegotiable: boolean;
}

const PERIOD_LABEL: Record<SalaryPeriod, string> = {
  year: 'per year',
  month: 'per month',
  hour: 'per hour',
};

export function formatMoney(amount: number, currency: string): string {
  try {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency,
      maximumFractionDigits: 0,
    }).format(amount);
  } catch {
    return `${currency} ${amount.toLocaleString()}`;
  }
}

/** "Salary negotiable", "$60,000 per year", or "$50,000 – $70,000 per year". */
export function formatSalary(job: SalaryLike): string {
  if (job.salaryType === 'negotiable' || job.salaryMin === null) {
    return 'Salary negotiable';
  }
  const min = formatMoney(job.salaryMin, job.salaryCurrency);
  const period = PERIOD_LABEL[job.salaryPeriod] ?? '';
  const base =
    job.salaryType === 'fixed' || job.salaryMax === null
      ? `${min} ${period}`.trim()
      : `${min} – ${formatMoney(job.salaryMax, job.salaryCurrency)} ${period}`.trim();
  return job.salaryNegotiable ? `${base} · Negotiable` : `${base} · Not negotiable`;
}
