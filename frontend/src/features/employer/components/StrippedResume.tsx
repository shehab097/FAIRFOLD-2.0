import { Fragment, type ReactNode } from 'react';
import type { EmployerReviewClaim } from '../../jobs/types';

const PLACEHOLDER = /(\[(?:EMPLOYER|INSTITUTION|NAME|EMAIL|PHONE|URL|YEAR|DATE)(?:_\d+)?\])/g;

const PLACEHOLDER_LABEL: Record<string, string> = {
  NAME: 'Name',
  EMAIL: 'Email',
  PHONE: 'Phone',
  URL: 'Link',
  YEAR: 'Year',
  DATE: 'Date',
};

const SECTION_NAMES =
  /^(professional summary|summary|profile|about|technical expertise|technical skills|skills|core skills|work experience|professional experience|experience|employment|education|academic|projects|certifications|certificates|awards|languages|interests|references)\b/i;

interface Token {
  text: string;
  placeholder: boolean;
  start: number;
}

function tokenize(line: string): Token[] {
  const tokens: Token[] = [];
  let cursor = 0;
  for (const match of line.matchAll(PLACEHOLDER)) {
    const start = match.index ?? 0;
    if (start > cursor) {
      tokens.push({ text: line.slice(cursor, start), placeholder: false, start: cursor });
    }
    tokens.push({ text: match[0], placeholder: true, start });
    cursor = start + match[0].length;
  }
  if (cursor < line.length) {
    tokens.push({ text: line.slice(cursor), placeholder: false, start: cursor });
  }
  return tokens;
}

function placeholderLabel(token: string): string {
  const key = token.slice(1, -1).replace(/_\d+$/, '');
  return PLACEHOLDER_LABEL[key] ?? (key === 'EMPLOYER' ? 'Employer' : 'Institution');
}

function isHeading(line: string): boolean {
  const clean = line.trim();
  if (!clean || clean.length > 90 || clean.includes('[') || clean.includes('|')) return false;
  if (SECTION_NAMES.test(clean)) return true;
  return !/[a-z]/.test(clean) && /[A-Z]{2,}/.test(clean);
}

function isBullet(line: string): boolean {
  return /^\s*[-•*\u2013\u2014]\s+/.test(line);
}

function isEntryLine(line: string): boolean {
  return /\[(?:EMPLOYER|INSTITUTION)_\d+]/.test(line) || /\(\d+ yrs? \d+ mos?\)/.test(line);
}

function bulletText(line: string): string {
  return line.replace(/^\s*[-•*\u2013\u2014]\s+/, '');
}

/** Build exact quote ranges for one line, skipping overlaps. */
function quoteRanges(line: string, claims: EmployerReviewClaim[]) {
  const ranges: { start: number; end: number; index: number }[] = [];
  claims.forEach((claim, index) => {
    const quote = claim.quote?.trim();
    if (!quote || quote.length < 10) return;
    let from = 0;
    while (from < line.length) {
      const start = line.indexOf(quote, from);
      if (start < 0) break;
      const end = start + quote.length;
      if (!ranges.some((r) => start < r.end && end > r.start)) {
        ranges.push({ start, end, index });
        break;
      }
      from = start + 1;
    }
  });
  return ranges.sort((a, b) => a.start - b.start);
}

function RichText({
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
  if (!text) return null;
  const ranges = quoteRanges(text, claims);
  const tokens = tokenize(text);
  const out: ReactNode[] = [];

  for (const token of tokens) {
    if (token.placeholder) {
      out.push(
        <span
          key={`p-${token.start}`}
          title="Personal detail removed"
          className="inline rounded-sm bg-raised px-1 py-0.5 align-baseline text-[11px] font-medium text-text-secondary"
        >
          [{placeholderLabel(token.text)}]
        </span>,
      );
      continue;
    }
    const localRanges = ranges
      .filter((r) => r.start < token.start + token.text.length && r.end > token.start)
      .map((r) => ({
        start: Math.max(0, r.start - token.start),
        end: Math.min(token.text.length, r.end - token.start),
        index: r.index,
      }));
    let cursor = 0;
    localRanges.forEach((range, i) => {
      if (range.start > cursor) {
        out.push(<Fragment key={`t-${token.start}-${i}`}>{token.text.slice(cursor, range.start)}</Fragment>);
      }
      out.push(
        <mark
          key={`q-${token.start}-${i}-${range.index}`}
          onClick={() => onSelectClaim(range.index)}
          className={
            'cursor-pointer rounded-sm bg-highlight px-0.5 py-0.5 text-text underline decoration-accent decoration-2 underline-offset-2 ' +
            (activeClaim === range.index ? 'ring-2 ring-primary-text' : '')
          }
        >
          {token.text.slice(range.start, range.end)}
        </mark>,
      );
      cursor = range.end;
    });
    if (cursor < token.text.length) {
      out.push(<Fragment key={`t-${token.start}-end`}>{token.text.slice(cursor)}</Fragment>);
    }
  }
  return <>{out}</>;
}

function Header({
  lines,
  claims,
  activeClaim,
  onSelectClaim,
}: {
  lines: string[];
  claims: EmployerReviewClaim[];
  activeClaim: number | null;
  onSelectClaim: (index: number) => void;
}) {
  if (lines.length === 0) return null;
  const [first, ...rest] = lines;
  const restParts = rest.flatMap((line) => line.split('|').map((part) => part.trim()).filter(Boolean));
  return (
    <header className="border-b border-border pb-4">
      <div className="text-xl font-bold leading-7 text-text">
        <RichText text={first} claims={claims} activeClaim={activeClaim} onSelectClaim={onSelectClaim} />
      </div>
      {restParts.length > 0 && (
        <div className="mt-2 flex flex-wrap items-center gap-x-3 gap-y-1.5 text-sm text-text-secondary">
          {restParts.map((part, index) => (
            <Fragment key={index}>
              {index > 0 && <span className="text-text-subtle">·</span>}
              <span>
                <RichText text={part} claims={claims} activeClaim={activeClaim} onSelectClaim={onSelectClaim} />
              </span>
            </Fragment>
          ))}
        </div>
      )}
    </header>
  );
}

function SkillChips({
  lines,
  claims,
  activeClaim,
  onSelectClaim,
}: {
  lines: string[];
  claims: EmployerReviewClaim[];
  activeClaim: number | null;
  onSelectClaim: (index: number) => void;
}) {
  const skills = lines
    .flatMap((line) => line.split(','))
    .map((skill) => skill.trim().replace(/\.$/, ''))
    .filter(Boolean);
  return (
    <div className="flex flex-wrap gap-1.5">
      {skills.map((skill, index) => (
        <span
          key={`${skill}-${index}`}
          className="rounded-md border border-border bg-bg px-2 py-0.5 text-xs text-text"
        >
          <RichText text={skill} claims={claims} activeClaim={activeClaim} onSelectClaim={onSelectClaim} />
        </span>
      ))}
    </div>
  );
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
  const lines = text.split('\n').map((line) => line.replace(/\s+$/, ''));
  let index = 0;
  while (index < lines.length && !lines[index].trim()) index += 1;
  const headerLines: string[] = [];
  while (index < lines.length && !isHeading(lines[index])) {
    if (lines[index].trim()) headerLines.push(lines[index].trim());
    index += 1;
  }

  const sections: { title: string; lines: string[] }[] = [];
  while (index < lines.length) {
    const title = lines[index].trim();
    index += 1;
    const body: string[] = [];
    while (index < lines.length && !isHeading(lines[index])) {
      if (lines[index].trim()) body.push(lines[index].trim());
      index += 1;
    }
    sections.push({ title, lines: body });
  }

  return (
    <article className="space-y-6">
      <Header
        lines={headerLines}
        claims={claims}
        activeClaim={activeClaim}
        onSelectClaim={onSelectClaim}
      />

      {sections.map((section) => {
        const isSkills = /skill|technical expertise|technical skills/i.test(section.title);
        const blocks: { type: 'bullets' | 'paragraph' | 'entry'; lines: string[] }[] = [];
        for (const line of section.lines) {
          if (isBullet(line)) {
            const last = blocks[blocks.length - 1];
            if (last?.type === 'bullets') last.lines.push(bulletText(line));
            else blocks.push({ type: 'bullets', lines: [bulletText(line)] });
          } else if (isEntryLine(line)) {
            blocks.push({ type: 'entry', lines: [line] });
          } else {
            const last = blocks[blocks.length - 1];
            if (last?.type === 'paragraph' && !isEntryLine(last.lines[last.lines.length - 1])) {
              last.lines.push(line);
            } else {
              blocks.push({ type: 'paragraph', lines: [line] });
            }
          }
        }

        return (
          <section key={section.title} className="space-y-3">
            <h3 className="border-b border-border pb-1.5 text-xs font-bold uppercase tracking-[0.08em] text-text-secondary">
              {section.title}
            </h3>
            {isSkills ? (
              <SkillChips
                lines={section.lines}
                claims={claims}
                activeClaim={activeClaim}
                onSelectClaim={onSelectClaim}
              />
            ) : (
              blocks.map((block, blockIndex) =>
                block.type === 'bullets' ? (
                  <ul key={blockIndex} className="space-y-1.5">
                    {block.lines.map((line, lineIndex) => (
                      <li key={lineIndex} className="flex gap-2.5 text-sm leading-6 text-text">
                        <span className="mt-2.5 h-1.5 w-1.5 shrink-0 rounded-full bg-text-subtle" />
                        <span>
                          <RichText
                            text={line}
                            claims={claims}
                            activeClaim={activeClaim}
                            onSelectClaim={onSelectClaim}
                          />
                        </span>
                      </li>
                    ))}
                  </ul>
                ) : block.type === 'entry' ? (
                  <p key={blockIndex} className="pt-1 text-sm font-semibold leading-6 text-text">
                    <RichText
                      text={block.lines[0]}
                      claims={claims}
                      activeClaim={activeClaim}
                      onSelectClaim={onSelectClaim}
                    />
                  </p>
                ) : (
                  <p key={blockIndex} className="text-sm leading-6 text-text">
                    <RichText
                      text={block.lines.join(' ')}
                      claims={claims}
                      activeClaim={activeClaim}
                      onSelectClaim={onSelectClaim}
                    />
                  </p>
                ),
              )
            )}
          </section>
        );
      })}
    </article>
  );
}
