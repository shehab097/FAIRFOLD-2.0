import { describe, expect, it } from 'vitest';
import { renderToStaticMarkup } from 'react-dom/server';
import { Markdown } from './Markdown';

const html = (src: string) => renderToStaticMarkup(<Markdown source={src} />);

describe('Markdown', () => {
  it('renders common syntax', () => {
    const out = html('## Role\n\n**Bold** and _it_\n\n- a\n- b\n\n1. one\n\n---\n\n[site](https://x.io)');
    expect(out).toContain('<h3');
    expect(out).toContain('<strong>Bold</strong>');
    expect(out).toContain('<em>it</em>');
    expect(out).toMatch(/<ul[^>]*><li>a<\/li><li>b<\/li><\/ul>/);
    expect(out).toContain('<ol');
    expect(out).toContain('<hr');
    expect(out).toContain('href="https://x.io"');
  });

  it('never renders raw HTML or unsafe links', () => {
    const out = html('<script>alert(1)</script> [x](javascript:alert(1))');
    expect(out).not.toContain('<script>');
    expect(out).not.toContain('javascript:');
  });
});

