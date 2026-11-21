import { Link, useParams } from 'react-router-dom';
import { ExternalLink } from 'lucide-react';
import { useApplication } from '../queries';
import { Banner } from '../../../components/ui/Banner';
import { Badge } from '../../../components/ui/Badge';
import { Button } from '../../../components/ui/Button';
import { Card } from '../../../components/ui/Card';
import { Icon } from '../../../components/ui/Icon';
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
