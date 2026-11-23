import { useState } from 'react';
import { Copy, Wand2 } from 'lucide-react';
import { useStory } from '../queries';
import { Badge } from '../../../components/ui/Badge';
import { Banner } from '../../../components/ui/Banner';
import { Button } from '../../../components/ui/Button';
import { Card } from '../../../components/ui/Card';
import { Icon } from '../../../components/ui/Icon';
import { useToast } from '../../../components/ui/Toast';
import { getApiErrorMessage } from '../../../lib/errors';

export function StoryPanel({ applicationId }: { applicationId: string }) {
  const story = useStory();
  const { toast } = useToast();
  const [error, setError] = useState<string | null>(null);

  async function generate() {
    setError(null);
    try {
      await story.mutateAsync(applicationId);
    } catch (err) {
      setError(getApiErrorMessage(err));
    }
  }

  async function copy() {
    if (!story.data?.story) return;
    await navigator.clipboard.writeText(story.data.story);
    toast('Story copied.', 'success');
  }

  return (
    <Card>
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h2 className="text-xl font-semibold">Job story</h2>
          <p className="mt-1 text-sm text-text-secondary">
            A short first-person draft that connects your stripped resume to this job. Generate it,
            then edit anything before using it.
          </p>
        </div>
        <Button loading={story.isPending} onClick={generate}>
          <Icon icon={Wand2} size={16} />
          {story.data ? 'Regenerate' : 'Write my story for this job'}
        </Button>
      </div>

      {error && (
        <div className="mt-4">
          <Banner tone="warning">{error}</Banner>
        </div>
      )}

      {story.data && (
        <div className="mt-4 space-y-3">
          <div className="flex flex-wrap items-center gap-2">
            <Badge tone="warning">AI draft, check before using</Badge>
            <Button variant="secondary" className="h-8 px-2 text-xs" onClick={copy}>
              <Icon icon={Copy} size={14} /> Copy
            </Button>
          </div>
          <p className="whitespace-pre-wrap rounded-xl border border-border bg-bg p-4 text-sm text-text">
            {story.data.story}
          </p>
        </div>
      )}
    </Card>
  );
}
