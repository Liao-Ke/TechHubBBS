import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import MdViewer from '../MdViewer.vue'

describe('MdViewer', () => {
  it('renders markdown content as HTML', () => {
    const wrapper = mount(MdViewer, {
      props: { content: '# Hello World' },
    })
    const html = wrapper.find('.markdown-body').element.innerHTML
    expect(html).toContain('<h1>')
    expect(html).toContain('Hello World')
  })

  it('renders bold text', () => {
    const wrapper = mount(MdViewer, {
      props: { content: '**bold text**' },
    })
    const html = wrapper.find('.markdown-body').element.innerHTML
    expect(html).toContain('<strong>bold text</strong>')
  })

  it('renders links', () => {
    const wrapper = mount(MdViewer, {
      props: { content: '[TechHub](https://example.com)' },
    })
    const html = wrapper.find('.markdown-body').element.innerHTML
    expect(html).toContain('<a href="https://example.com"')
    expect(html).toContain('TechHub')
  })

  it('renders code blocks with highlight.js classes', () => {
    const wrapper = mount(MdViewer, {
      props: { content: '```js\nconst x = 1\n```' },
    })
    const html = wrapper.find('.markdown-body').element.innerHTML
    expect(html).toContain('<pre class="hljs"')
    expect(html).toContain('<code')
  })

  it('escapes raw HTML to prevent XSS', () => {
    const wrapper = mount(MdViewer, {
      props: { content: '<script>alert(1)</script>' },
    })
    const html = wrapper.find('.markdown-body').element.innerHTML
    expect(html).not.toContain('<script>')
    expect(html).toContain('&lt;script&gt;')
  })

  it('handles empty content gracefully', () => {
    const wrapper = mount(MdViewer, {
      props: { content: '' },
    })
    expect(wrapper.find('.markdown-body').exists()).toBe(true)
    expect(wrapper.find('.markdown-body').element.innerHTML).toBe('')
  })

  it('reacts to content prop changes', async () => {
    const wrapper = mount(MdViewer, {
      props: { content: 'first' },
    })
    expect(wrapper.find('.markdown-body').element.innerHTML).toContain('first')

    await wrapper.setProps({ content: 'second' })
    expect(wrapper.find('.markdown-body').element.innerHTML).toContain('second')
  })
})
