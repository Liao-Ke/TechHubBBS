import { describe, it, expect } from 'vitest'
import { sanitizeHtml } from '../sanitize'

describe('sanitizeHtml', () => {
  it('removes script tags', () => {
    const result = sanitizeHtml('<script>alert("xss")</script>')
    expect(result).not.toContain('<script>')
    expect(result).not.toContain('alert')
  })

  it('removes event handlers like onclick', () => {
    const result = sanitizeHtml('<div onclick="alert(1)">click me</div>')
    expect(result).not.toContain('onclick')
    expect(result).toContain('click me')
  })

  it('preserves safe HTML tags', () => {
    const result = sanitizeHtml('<strong>bold</strong> <em>italic</em>')
    expect(result).toContain('<strong>bold</strong>')
    expect(result).toContain('<em>italic</em>')
  })

  it('removes data-* attributes when ALLOW_DATA_ATTR is false', () => {
    const result = sanitizeHtml('<div data-user-id="123">user</div>')
    expect(result).not.toContain('data-user-id')
    expect(result).toContain('user')
  })

  it('removes javascript: protocol in href', () => {
    const result = sanitizeHtml('<a href="javascript:alert(1)">link</a>')
    expect(result).not.toContain('javascript:')
  })

  it('handles empty string', () => {
    expect(sanitizeHtml('')).toBe('')
  })

  it('preserves allowed attributes like href and class', () => {
    const result = sanitizeHtml('<a href="https://example.com" class="link" target="_blank">safe link</a>')
    expect(result).toContain('href="https://example.com"')
    expect(result).toContain('class="link"')
    expect(result).toContain('target="_blank"')
  })

  it('removes disallowed tags like style', () => {
    const result = sanitizeHtml('<style>body { color: red; }</style>')
    expect(result).not.toContain('<style>')
  })
})
