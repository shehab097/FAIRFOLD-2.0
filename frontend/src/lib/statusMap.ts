import { Check, Clock, Lock, Minus, TriangleAlert, X } from 'lucide-react';
import type { LucideIcon } from 'lucide-react';

export type StatusTone =
  | 'neutral'
  | 'progress'
  | 'success'
  | 'warning'
  | 'failure'
  | 'closed';

export interface StatusStyle {
  tone: StatusTone;
  icon: LucideIcon;
  label: string;
}

const TONE_ICONS: Record<StatusTone, LucideIcon> = {
  neutral: Minus,
  progress: Clock,
  success: Check,
  warning: TriangleAlert,
  failure: X,
  closed: Lock,
};

const TONE_CLASSES: Record<StatusTone, string> = {
  neutral: 'bg-raised border border-border text-text-secondary',
  progress: 'bg-primary-subtle text-primary-text',
  success: 'bg-primary-subtle text-primary-text',
  warning: 'bg-accent-subtle text-accent',
  failure: 'bg-danger text-white',
  closed: 'bg-raised border border-border text-text-subtle',
};

const STATUS_ENTRIES: Record<string, { tone: StatusTone; label: string }> = {
  // Job
  draft: { tone: 'neutral', label: 'Draft' },
  active: { tone: 'success', label: 'Active' },
  paused: { tone: 'warning', label: 'Paused' },
  closed: { tone: 'closed', label: 'Closed' },
  // Application
  applied: { tone: 'neutral', label: 'Applied' },
  screening: { tone: 'progress', label: 'Screening' },
  screened: { tone: 'success', label: 'Screened' },
  not_matched: { tone: 'neutral', label: 'Not matched' },
  shortlisted: { tone: 'success', label: 'Shortlisted' },
  interview: { tone: 'progress', label: 'Interview' },
  hired: { tone: 'success', label: 'Hired' },
  rejected: { tone: 'failure', label: 'Rejected' },
  // Screening item
  queued: { tone: 'neutral', label: 'Queued' },
  running: { tone: 'progress', label: 'Running' },
  done: { tone: 'success', label: 'Done' },
  failed: { tone: 'failure', label: 'Failed' },
  // Assessment gate
  not_required: { tone: 'neutral', label: 'Not required' },
  pending: { tone: 'progress', label: 'Pending' },
  passed: { tone: 'success', label: 'Passed' },
  // Bias
  clear: { tone: 'success', label: 'Clear' },
  flagged: { tone: 'warning', label: 'Flagged' },
  // Claim verdicts
  met: { tone: 'success', label: 'Met' },
  partial: { tone: 'warning', label: 'Partial' },
  not_met: { tone: 'failure', label: 'Not met' },
  // Resume
  uploaded: { tone: 'neutral', label: 'Uploaded' },
  processing: { tone: 'progress', label: 'Processing' },
  ready: { tone: 'success', label: 'Ready' },
  // Content
  broken: { tone: 'failure', label: 'Broken' },
};

export function statusStyle(status: string): StatusStyle {
  const entry = STATUS_ENTRIES[status] ?? { tone: 'neutral' as StatusTone, label: status };
  return { tone: entry.tone, icon: TONE_ICONS[entry.tone], label: entry.label };
}

export function statusToneClasses(tone: StatusTone): string {
  return TONE_CLASSES[tone];
}
