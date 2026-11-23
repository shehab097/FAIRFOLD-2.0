export function SaveIndicator({
  saving,
  saved,
  error,
}: {
  saving: boolean;
  saved: boolean;
  error: string | null;
}) {
  return (
    <div
      aria-live="polite"
      className={error ? 'text-sm text-accent' : 'text-sm text-text-secondary'}
    >
      {error ? error : saving ? 'Saving…' : saved ? 'Saved' : 'Answers save automatically'}
    </div>
  );
}
