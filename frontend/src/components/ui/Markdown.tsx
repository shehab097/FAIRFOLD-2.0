import type { ReactNode } from 'react';
import { cn } from '../../lib/cn';

// Minimal, safe Markdown renderer: builds React elements (no innerHTML).
// Supports #/##/### headings, **bold**, *italic*/_italic_, `code`, [links](https://…),
// - / 1. lists, > quotes and --- rules.

const INLINE = /(\*\*[^*]+\*\*|\*[^*\s][^*]*\*|_[^_\s][^_]*_|`[^`]+`|\[[^\]]+\]\([^)\s]+\))/g;
const SAFE_URL = /^(https?:\/\/|mailto:)/i;

function inline(text: string): ReactNode[] {
  return text.split(INLINE).map((part, i) => {
    if (part.startsWith('**') && part.endsWith('**') && part.length > 4) return <strong key={i}>{inline(part.slice(2, -2))}</strong>;
    if (part.startsWith('`') && part.endsWith('`') && part.length > 2)
      return <code key={i} className="bg-raised px-1 font-mono text-[0.9em]">{part.slice(1, -1)}</code>;
    const link = /^\[([^\]]+)\]\(([^)\s]+)\)$/.exec(part);
    if (link) {
      return SAFE_URL.test(link[2]) ? (
        <a key={i} href={link[2]} target="_blank" rel="noopener noreferrer" className="text-primary-text underline">
          {link[1]}
        </a>
      ) : (
        link[1]
      );
    }
    if (/^(\*[^*].*\*|_[^_].*_)$/.test(part)) return <em key={i}>{inline(part.slice(1, -1))}</em>;
    return part;
  });
}

const LIST = { ul: /^\s*[-*+]\s+/, ol: /^\s*\d+[.)]\s+/ };

export function Markdown({ source, className }: { source: string; className?: string }) {
  const lines = source.replace(/\r\n?/g, '\n').split('\n');
  const out: ReactNode[] = [];
  let para: string[] = [];
  const flush = () => {
    if (para.length) out.push(<p key={out.length}>{inline(para.join(' '))}</p>);
    para = [];
  };

  for (let i = 0; i < lines.length; i++) {
    const line = lines[i];
    const h = /^(#{1,3})\s+(.*)$/.exec(line);
    if (!line.trim()) flush();
    else if (h) {
      flush();
      const Tag = (['h2', 'h3', 'h4'] as const)[h[1].length - 1];
      out.push(<Tag key={out.length} className={h[1].length === 1 ? 'text-xl font-semibold' : 'text-base font-semibold'}>{inline(h[2])}</Tag>);
    } else if (/^\s*([-*_])(\s*\1){2,}\s*$/.test(line)) {
      flush();
      out.push(<hr key={out.length} className="border-border" />);
    } else if (line.startsWith('>')) {
      flush();
      const quote: string[] = [];
      for (; i < lines.length && lines[i].startsWith('>'); i++) quote.push(lines[i].replace(/^>\s?/, ''));
      i--;
      out.push(<blockquote key={out.length} className="border-l-2 border-border-strong pl-3 text-text-secondary">{inline(quote.join(' '))}</blockquote>);
    } else {
      const kind = LIST.ul.test(line) ? 'ul' : LIST.ol.test(line) ? 'ol' : null;
      if (!kind) {
        para.push(line.trim());
        continue;
      }
      flush();
      const items: string[] = [];
      for (; i < lines.length && LIST[kind].test(lines[i]); i++) items.push(lines[i].replace(LIST[kind], ''));
      i--;
      const List = kind;
      out.push(
        <List key={out.length} className={cn('space-y-1 pl-5', kind === 'ul' ? 'list-disc' : 'list-decimal')}>
          {items.map((it, j) => <li key={j}>{inline(it)}</li>)}
        </List>,
      );
    }
  }
  flush();
  return <div className={cn('space-y-3 break-words text-text', className)}>{out}</div>;
}

