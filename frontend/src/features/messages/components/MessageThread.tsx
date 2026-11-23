import { useEffect, useRef, useState } from 'react';
import { getApiErrorMessage } from '../../../lib/errors';
import { formatDate } from '../../../lib/format';
import { cn } from '../../../lib/cn';
import { useMessages, useSendMessage } from '../queries';
import { Banner } from '../../../components/ui/Banner';
import { Button } from '../../../components/ui/Button';
import { Skeleton } from '../../../components/ui/Skeleton';

const MAX = 2000;

export function MessageThread({ applicationId }: { applicationId: string }) {
  const { data, isLoading, isError, refetch } = useMessages(applicationId, true);
  const send = useSendMessage(applicationId);
  const [body, setBody] = useState('');
  const [error, setError] = useState<string | null>(null);
  const bottomRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth', block: 'end' });
  }, [data?.length]);

  async function submit() {
    setError(null);
    const clean = body.trim();
    if (clean.length < 1 || clean.length > MAX) {
      setError('Write a message of 1 to 2000 characters.');
      return;
    }
    try {
      await send.mutateAsync(clean);
      setBody('');
    } catch (err) {
      setError(getApiErrorMessage(err));
    }
  }

  return (
    <div className="rounded-xl border border-border bg-surface shadow-sm">
      <div className="border-b border-border px-5 py-4">
        <h2 className="text-lg font-semibold">Messages</h2>
        <p className="text-xs text-text-secondary">
          Messages stay inside this application. They are never sent to AI.
        </p>
      </div>

      <div className="max-h-[420px] space-y-3 overflow-y-auto p-5">
        {isLoading && <Skeleton className="h-24 w-full rounded-lg" />}
        {isError && (
          <Banner tone="error">
            Could not load messages.{' '}
            <button type="button" className="underline" onClick={() => refetch()}>
              Retry
            </button>
          </Banner>
        )}
        {!isLoading && !isError && (data?.length ?? 0) === 0 && (
          <p className="text-sm text-text-subtle">No messages yet. Start the conversation.</p>
        )}
        {data?.map((message) => (
          <div
            key={message.id}
            className={cn('flex', message.mine ? 'justify-end' : 'justify-start')}
          >
            <div
              className={cn(
                'max-w-[80%] rounded-xl px-3 py-2',
                message.mine ? 'bg-primary-subtle text-text' : 'bg-raised text-text',
              )}
            >
              <div className="text-xs font-semibold text-text-secondary">
                {message.mine ? 'You' : message.senderName}
              </div>
              <div className="whitespace-pre-wrap text-sm">{message.body}</div>
              <div className="mt-1 text-right text-[11px] text-text-subtle">
                {formatDate(message.createdAt)}
              </div>
            </div>
          </div>
        ))}
        <div ref={bottomRef} />
      </div>

      <div className="border-t border-border p-4">
        {error && <p className="mb-2 text-sm text-accent">{error}</p>}
        <textarea
          value={body}
          onChange={(e) => {
            setBody(e.target.value.slice(0, MAX));
            setError(null);
          }}
          rows={3}
          maxLength={MAX}
          className="w-full rounded-lg border border-border bg-bg px-3 py-2 text-sm text-text placeholder:text-text-subtle focus:border-primary focus:outline-none"
          placeholder="Write a message…"
        />
        <div className="mt-2 flex items-center justify-between gap-3">
          <span className="text-xs text-text-secondary">
            {body.length}/{MAX}
          </span>
          <Button loading={send.isPending} onClick={submit} disabled={body.trim().length === 0}>
            Send
          </Button>
        </div>
      </div>
    </div>
  );
}
