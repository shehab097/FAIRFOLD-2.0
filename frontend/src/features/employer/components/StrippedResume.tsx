import type { EmployerReviewClaim } from '../../jobs/types';

interface Segment {
  text: string;
  claim?: number;
}

function buildSegments(text: string, claims: EmployerReviewClaim[]): Segment[] {
  const ranges: { start: number; end: number; index: number }[] = [];
  claims.forEach((claim, index) => {
    const quote = claim.quote?.trim();
    if (!quote || quote.length < 10) return;
    let from = 0;
    while (from <= text.length) {
      const start = text.indexOf(quote, from);
      if (start < 0) break;
      const end = start + quote.length;
      const overlaps = ranges.some((r) => start < r.end && end > r.start);
      if (!overlaps) {
        ranges.push({ start, end, index });
        break;
      }
      from = start + 1;
    }
  });
  ranges.sort((a, b) => a.start - b.start);
  const segments: Segment[] = [];
  let cursor = 0;
  for (const range of ranges) {
    if (range.start > cursor) {
      segments.push({ text: text.slice(cursor, range.start) });
    }
    segments.push({ text: text.slice(range.start, range.end), claim: range.index });
    cursor = range.end;
  }
  if (cursor < text.length) {
    segments.push({ text: text.slice(cursor) });
  }
  return segments;
}

export function StrippedResume({
  text,
  claims,
  activeClaim,
  onSelectClaim,
}: {
  text: string;
  claims: EmployerReviewClaim[];
  activeClaim: number | null;
  onSelectClaim: (index: number) => void;
}) {
  const segments = buildSegments(text, claims);
  return (
    <div className="whitespace-pre-wrap break-words text-sm leading-7 text-text">
      {segments.map((segment, index) =>
        segment.claim === undefined ? (
          <span key={index}>{segment.text}</span>
        ) : (
          <mark
            key={index}
            id={`quote-${segment.claim}`}
            onClick={() => onSelectClaim(segment.claim as number)}
            className={
              'cursor-pointer rounded-sm bg-highlight px-0.5 py-0.5 text-text underline decoration-accent decoration-2 underline-offset-2 ' +
              (activeClaim === segment.claim ? 'ring-2 ring-primary-text' : '')
            }
          >
            {segment.text}
          </mark>
        ),
      )}
    </div>
  );
}
