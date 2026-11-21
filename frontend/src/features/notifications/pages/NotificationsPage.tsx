import { useNavigate } from 'react-router-dom';
import { useMarkAllRead, useMarkRead, useNotifications } from '../queries';
import { Banner } from '../../../components/ui/Banner';
import { Button } from '../../../components/ui/Button';
import { Card } from '../../../components/ui/Card';
import { Skeleton } from '../../../components/ui/Skeleton';
import { cn } from '../../../lib/cn';
import { formatDate } from '../../../lib/format';

export function NotificationsPage() {
  const { data, isLoading, isError } = useNotifications(0);
  const markRead = useMarkRead();
  const markAllRead = useMarkAllRead();
  const navigate = useNavigate();

  async function open(id: string, link: string | null) {
    try {
      await markRead.mutateAsync(id);
    } catch {
      // Ignore; still navigate.
    }
    if (link) navigate(link);
  }

  return (
    <div>
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="text-[28px] font-bold leading-9">Notifications</h1>
          <p className="text-text-secondary">Updates about your applications.</p>
        </div>
        <Button
          variant="secondary"
          loading={markAllRead.isPending}
          onClick={() => markAllRead.mutate()}
        >
          Mark all read
        </Button>
      </div>

      {isLoading && <Skeleton className="h-64 w-full" />}
      {isError && <Banner tone="error">Could not load notifications.</Banner>}

      {!isLoading && !isError && (
        <Card className="divide-y divide-border p-0">
          {data?.items.length === 0 && (
            <div className="p-8 text-center text-text-secondary">No notifications.</div>
          )}
          {data?.items.map((n) => (
            <button
              key={n.id}
              type="button"
              onClick={() => open(n.id, n.link)}
              className={cn(
                'block w-full px-4 py-3 text-left hover:bg-raised',
                !n.readAt && 'bg-primary-subtle',
              )}
            >
              <div className="flex items-center gap-2">
                {!n.readAt && <span className="h-2 w-2 shrink-0 bg-primary" />}
                <span className="text-sm font-semibold text-text">{n.title}</span>
              </div>
              <p className="mt-1 text-sm text-text-secondary">{n.body}</p>
              <p className="mt-1 text-xs text-text-subtle">{formatDate(n.createdAt)}</p>
            </button>
          ))}
        </Card>
      )}
    </div>
  );
}
