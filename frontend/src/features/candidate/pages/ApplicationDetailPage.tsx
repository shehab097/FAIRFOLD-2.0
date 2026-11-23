import { Link, useParams } from 'react-router-dom';
import { ExternalLink, TriangleAlert } from 'lucide-react';
import { useApplication } from '../queries';
import { Banner } from '../../../components/ui/Banner';
import { Badge } from '../../../components/ui/Badge';
import { Button } from '../../../components/ui/Button';
import { Card } from '../../../components/ui/Card';
import { Icon } from '../../../components/ui/Icon';
import { ScoreMeter } from '../../../components/ui/ScoreMeter';
import { Skeleton } from '../../../components/ui/Skeleton';
import { formatDate } from '../../../lib/format';

export function ApplicationDetailPage() {
  const { applicationId } = useParams<{ applicationId: string }>();
  const { data: application, isLoading, isError } = useApplication(applicationId);

  if (isLoading) return <Skeleton className="h-64 w-full" />;
  if (isError || !application) return <Banner tone="error">Application not found.</Banner>;

  return (
    <div className="space-y-6">
      <div>
        <div className="flex items-center gap-3">
          <h1 className="text-[28px] font-bold leading-9">{application.jobTitle}</h1>
          <Badge status={application.status} />
        </div>
        <p className="text-text-secondary">
          {application.companyId ? (
            <Link to={`/companies/${application.companyId}`} className="text-primary-text hover:underline">
              {application.companyName ?? 'Company'}
            </Link>
          ) : (
            application.companyName ?? 'Confidential company'
          )}{' '}
          · {application.city ? `${application.city}, ` : ''}
          {application.country}
        </p>
      </div>

      {application.status === 'not_matched' && (
        <Banner tone="warning">
          Not matched: {application.notMatchedRule ?? 'A hard filter excluded this application.'}
        </Banner>
      )}

      {(application.score !== null || application.claims.length > 0) && (
        <Card>
          <div className="flex flex-wrap items-start justify-between gap-4">
            <div>
              <h2 className="text-xl font-semibold">How you were scored</h2>
              <p className="mt-1 text-sm text-text-secondary">
                Scores are advisory. Each claim is backed by a quote from your stripped resume.
              </p>
            </div>
            {application.scoringMode === 'rule_based' && (
              <Badge tone="warning" icon={TriangleAlert}>
                Rule-based result
              </Badge>
            )}
          </div>
          {application.score !== null && (
            <div className="mt-5 max-w-sm">
              <ScoreMeter score={application.score} />
            </div>
          )}
          <div className="mt-5 space-y-3">
            {application.claims.map((claim, index) => (
              <div key={`${claim.requirement}-${index}`} className="rounded-xl border border-border bg-bg p-3">
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <span className="text-sm font-semibold text-text">{claim.requirement}</span>
                  <Badge status={claim.verdict} />
                </div>
                {claim.quote && (
                  <p className="mt-2 rounded-md bg-highlight px-2 py-1 text-xs text-text">
                    “{claim.quote}”
                  </p>
                )}
              </div>
            ))}
          </div>
          {application.missingSkills.length > 0 && (
            <div className="mt-5">
              <h3 className="mb-2 text-sm font-semibold text-text">Missing skills</h3>
              <div className="flex flex-wrap gap-1.5">
                {application.missingSkills.map((skill) => (
                  <span
                    key={skill}
                    className="rounded-full border border-border bg-raised px-2.5 py-0.5 text-xs text-text-secondary"
                  >
                    {skill}
                  </span>
                ))}
              </div>
            </div>
          )}
        </Card>
      )}

      {application.requiredAssessments.length > 0 && (
        <Card>
          <div className="flex flex-wrap items-start justify-between gap-3">
            <div>
              <h2 className="text-xl font-semibold">Required assessments</h2>
              <p className="mt-1 text-sm text-text-secondary">
                This employer requires a pass before your application can be shortlisted.
              </p>
            </div>
            <Badge status={application.gateStatus} />
          </div>
          <ul className="mt-4 divide-y divide-border">
            {application.requiredAssessments.map((assessment) => (
              <li key={assessment.assessmentId} className="flex flex-wrap items-center justify-between gap-3 py-3">
                <div>
                  <div className="text-sm font-semibold text-text">
                    {assessment.title ?? 'Assessment'}
                  </div>
                  <div className="text-xs text-text-secondary">
                    Best {assessment.bestScore === null ? '—' : `${assessment.bestScore}%`} ·
                    minimum {assessment.minScore}%
                  </div>
                </div>
                <div className="flex items-center gap-2">
                  <Badge status={assessment.status} />
                  {assessment.status !== 'passed' && (
                    <Link to={`/candidate/assessments/${assessment.assessmentId}/attempt`}>
                      <Button className="h-8 px-3 text-xs">
                        {assessment.status === 'failed' ? 'Retake' : 'Take'}
                      </Button>
                    </Link>
                  )}
                </div>
              </li>
            ))}
          </ul>
        </Card>
      )}

      <Card>
        <h2 className="mb-4 text-xl font-semibold">Status timeline</h2>
        <ol className="space-y-0">
          {application.history.map((h, i) => (
            <li key={i} className="flex items-start gap-4 border-b border-border py-3 last:border-b-0">
              <span className="mt-1 h-2 w-2 shrink-0 bg-primary" />
              <div>
                <div className="text-sm font-medium text-text">{h.status.replace('_', ' ')}</div>
                <div className="text-xs text-text-subtle">{formatDate(h.at)}</div>
              </div>
            </li>
          ))}
          {application.history.length === 0 && (
            <li className="text-sm text-text-subtle">No status updates yet.</li>
          )}
        </ol>
      </Card>

      <div className="flex flex-wrap items-center gap-4 text-sm">
        <Link to={`/jobs/${application.jobId}`}>
          <Button variant="secondary">
            <Icon icon={ExternalLink} size={16} />
            View job details
          </Button>
        </Link>
        <Link to="/candidate/applications" className="text-primary-text hover:underline">
          Back to applications
        </Link>
      </div>
    </div>
  );
}
