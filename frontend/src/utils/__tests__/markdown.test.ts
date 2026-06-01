import { describe, it, expect } from 'vitest'
import { renderMarkdown } from '../markdown'

describe('renderMarkdown', () => {
  it('renders basic markdown headings', () => {
    const result = renderMarkdown('# Hello')
    expect(result).toContain('<h1>')
    expect(result).toContain('Hello')
    expect(result).toContain('</h1>')
  })

  it('renders bold and italic text', () => {
    const result = renderMarkdown('**bold** and *italic*')
    expect(result).toContain('<strong>bold</strong>')
    expect(result).toContain('<em>italic</em>')
  })

  it('renders code block with hljs classes', () => {
    const result = renderMarkdown('```typescript\nconst x = 1\n```')
    expect(result).toContain('<pre class="hljs"')
    expect(result).toContain('<code')
  })

  it('renders inline code', () => {
    const result = renderMarkdown('Use `code` inline')
    expect(result).toContain('<code>code</code>')
  })

  it('prevents XSS: raw script tags are escaped by markdown-it html:false', () => {
    const result = renderMarkdown('<script>alert(1)</script>')
    expect(result).not.toContain('<script>')
    expect(result).toContain('&lt;script&gt;')
  })

  it('prevents XSS: script tags in markdown text are escaped', () => {
    const result = renderMarkdown('Hello <script>alert("xss")</script> World')
    expect(result).not.toContain('<script>')
    expect(result).toContain('Hello')
    expect(result).toContain('World')
  })

  it('prevents XSS: javascript: protocol links are rejected by markdown-it validateLink', () => {
    const result = renderMarkdown('[click](javascript:alert(1))')
    expect(result).not.toContain('<a')
    expect(result).toContain('[click](javascript:alert(1))')
  })

  it('auto-links URLs when linkify is true', () => {
    const result = renderMarkdown('Visit https://example.com')
    expect(result).toContain('<a href="https://example.com"')
    expect(result).toContain('https://example.com')
  })

  it('converts line breaks to <br> when breaks is true', () => {
    const result = renderMarkdown('line1\nline2')
    expect(result).toContain('<br>')
  })

  it('renders unordered lists', () => {
    const result = renderMarkdown('- item1\n- item2')
    expect(result).toContain('<ul>')
    expect(result).toContain('<li>item1</li>')
    expect(result).toContain('<li>item2</li>')
  })

  it('renders links', () => {
    const result = renderMarkdown('[TechHub](https://techhub.example.com)')
    expect(result).toContain('<a href="https://techhub.example.com"')
    expect(result).toContain('TechHub')
  })

  it('handles empty content', () => {
    expect(renderMarkdown('')).toBe('')
  })

  it('prevents XSS: raw HTML tags are escaped by markdown-it html:false', () => {
    const result = renderMarkdown('<img src=x onerror="alert(1)">')
    expect(result).not.toContain('<img')
    expect(result).toContain('&lt;img')
  })
})
