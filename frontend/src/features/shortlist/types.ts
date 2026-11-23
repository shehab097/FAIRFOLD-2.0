export interface ShortlistedCandidate {
  applicationId: string;
  jobId: string;
  jobTitle: string | null;
  candidateName: string | null;
  candidateEmail: string | null;
  score: number | null;
  status: string;
  shortlistedAt: string | null;
}
