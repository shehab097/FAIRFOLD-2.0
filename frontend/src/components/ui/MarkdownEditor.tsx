import { useRef } from 'react';
import { Bold, Code, Heading2, Heading3, Italic, Link2, List, ListOrdered, Minus, Quote, type LucideIcon } from 'lucide-react';
import { Icon } from './Icon';
import { Markdown } from './Markdown';

type Tool = { label: string; icon: LucideIcon } & (
  | { wrap: [string, string]; placeholder: string }
  | { prefix: string }
  | { insert: string }
);

const TOOLS: Tool[] = [
  { label: 'Heading', icon: Heading2, prefix: '## ' },
  { label: 'Subheading', icon: Heading3, prefix: '### ' },
  { label: 'Bold', icon: Bold, wrap: ['**', '**'], placeholder: 'bold' },
  { label: 'Italic', icon: Italic, wrap: ['_', '_'], placeholder: 'italic' },
  { label: 'Code', icon: Code, wrap: ['`', '`'], placeholder: 'code' },
  { label: 'Link', icon: Link2, wrap: ['[', '](https://)'], placeholder: 'link text' },
  { label: 'Bulleted list', icon: List, prefix: '- ' },
  { label: 'Numbered list', icon: ListOrdered, prefix: '1. ' },
  { label: 'Quote', icon: Quote, prefix: '> ' },
  { label: 'Horizontal rule', icon: Minus, insert: '\n\n---\n\n' },
];

interface Props {
  id?: string;
  value: string;
  onChange: (value: string) => void;
  rows?: number;
  required?: boolean;
  error?: boolean;
  placeholder?: string;
}

/** Markdown textarea with a quick-format toolbar and a live preview. */
export function MarkdownEditor({ id, value, onChange, rows = 10, required, error, placeholder }: Props) {
  const ref = useRef<HTMLTextAreaElement>(null);

  function apply(tool: Tool) {
    const el = ref.current;
    if (!el) return;
    const { selectionStart: s, selectionEnd: e } = el;
    const sel = value.slice(s, e);
    let text: string;
    let start: number;
    let end: number;
    if ('wrap' in tool) {
      const body = sel || tool.placeholder;
      text = tool.wrap[0] + body + tool.wrap[1];
      start = s + tool.wrap[0].length;
      end = start + body.length;
    } else if ('prefix' in tool) {
      const lineStart = value.lastIndexOf('\n', s - 1) + 1;
      const block = value.slice(lineStart, e);
      text = block.split('\n').map((l, i) => (tool.prefix === '1. ' ? `${i + 1}. ` : tool.prefix) + l).join('\n');
      onChange(value.slice(0, lineStart) + text + value.slice(e));
      return requestAnimationFrame(() => (el.focus(), el.setSelectionRange(lineStart + text.length, lineStart + text.length)));
    } else {
      text = tool.insert;
      start = end = s + text.length;
    }
    onChange(value.slice(0, s) + text + value.slice(e));
    requestAnimationFrame(() => (el.focus(), el.setSelectionRange(start, end)));
  }

  return (
    <div className={error ? 'rounded-xl border border-accent overflow-hidden shadow-sm' : 'rounded-xl border border-border overflow-hidden shadow-sm'}>
      <div className="flex flex-wrap gap-1 border-b border-border bg-bg px-2 py-1.5" role="toolbar" aria-label="Formatting">
        {TOOLS.map((t) => (
          <button
            key={t.label}
            type="button"
            title={t.label}
            aria-label={t.label}
            onClick={() => apply(t)}
            className="rounded-md p-1.5 text-text-secondary hover:bg-surface hover:text-text transition-colors"
          >
            <Icon icon={t.icon} size={16} />
          </button>
        ))}
      </div>
      <div className="grid lg:grid-cols-2">
        <textarea
          id={id}
          ref={ref}
          rows={rows}
          value={value}
          onChange={(e) => onChange(e.target.value)}
          required={required}
          placeholder={placeholder ?? 'Write in Markdown…'}
          className="w-full resize-y bg-bg px-3 py-2 font-mono text-sm text-text placeholder:text-text-subtle"
        />
        <div className="min-h-full border-t border-border bg-surface p-3 text-sm lg:border-l lg:border-t-0" aria-label="Preview">
          {value.trim() ? <Markdown source={value} /> : <p className="text-text-subtle">Preview appears here.</p>}
        </div>
      </div>
    </div>
  );
}
