import MarkdownIt from 'markdown-it'
import DOMPurify from 'dompurify'
import hljs from 'highlight.js'

const md: MarkdownIt = new MarkdownIt({
  html: false,        // Disable raw HTML in input
  linkify: true,      // Auto-linkify URLs
  typographer: true,  // Smart quotes, dashes
  breaks: true,       // Convert \n to <br>
  highlight: (str: string, lang: string): string => {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return `<pre class="hljs"><code class="language-${lang}">${hljs.highlight(str, { language: lang, ignoreIllegals: true }).value}</code></pre>`
      } catch {
        // fall through to auto-detect
      }
    }
    return `<pre class="hljs"><code>${md.utils.escapeHtml(str)}</code></pre>`
  },
})

/**
 * Render Markdown to safe HTML.
 * Pipeline: Markdown → HTML → DOMPurify sanitization → safe HTML
 */
export function renderMarkdown(content: string): string {
  const rawHtml = md.render(content)
  return DOMPurify.sanitize(rawHtml, {
    ALLOWED_TAGS: [
      'a', 'abbr', 'b', 'blockquote', 'br', 'code', 'dd', 'del', 'div', 'dl',
      'dt', 'em', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6', 'hr', 'i', 'img',
      'input', 'li', 'ol', 'p', 'pre', 's', 'span', 'strong', 'sub', 'sup',
      'table', 'tbody', 'td', 'th', 'thead', 'tr', 'ul',
    ],
    ALLOWED_ATTR: [
      'href', 'src', 'alt', 'title', 'class', 'id', 'target', 'rel',
      'checked', 'disabled', 'type',
    ],
    ALLOW_DATA_ATTR: false,
  })
}
