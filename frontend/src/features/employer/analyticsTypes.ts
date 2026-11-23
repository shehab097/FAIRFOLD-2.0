export interface AnalyticsFunnelStage {
  stage: string;
  count: number;
}

export interface AnalyticsTimeToHire {
  month: string;
  hires: number;
  avgDays: number;
}

export interface AnalyticsOverride {
  date: string;
  userName: string;
  jobTitle: string;
  alias: string;
  direction: string;
  reason: string | null;
}

export interface Analytics {
  funnel: AnalyticsFunnelStage[];
  timeToHire: AnalyticsTimeToHire[];
  overrides: AnalyticsOverride[];
  pullBacks: number;
}

export interface AnalyticsFilters {
  jobId?: string;
  from?: string;
  to?: string;
}
