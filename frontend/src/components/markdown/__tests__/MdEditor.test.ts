import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import MdEditor from '../MdEditor.vue'

describe('MdEditor', () => {
  it('renders with initial modelValue', () => {
    const wrapper = mount(MdEditor, {
      props: { modelValue: '# Test' },
    })
    expect(wrapper.find('.md-editor').exists()).toBe(true)
    expect(wrapper.find('.md-editor__textarea').exists()).toBe(true)
    expect(wrapper.find('.md-editor__preview').exists()).toBe(true)
    expect(wrapper.find('.md-editor__footer').exists()).toBe(true)
  })

  it('renders the toolbar', () => {
    const wrapper = mount(MdEditor, {
      props: { modelValue: '' },
    })
    expect(wrapper.findComponent({ name: 'Toolbar' }).exists()).toBe(true)
  })

  it('displays character count in footer', () => {
    const wrapper = mount(MdEditor, {
      props: { modelValue: 'Hello World' },
    })
    const footer = wrapper.find('.md-editor__count')
    expect(footer.text()).toContain('11')
    expect(footer.text()).toContain('字')
  })

  it('updates character count when modelValue changes', async () => {
    const wrapper = mount(MdEditor, {
      props: { modelValue: 'ab' },
    })
    expect(wrapper.find('.md-editor__count').text()).toContain('2')

    await wrapper.setProps({ modelValue: 'abcdef' })
    expect(wrapper.find('.md-editor__count').text()).toContain('6')
  })

  it('emits update:modelValue when textarea input changes', async () => {
    const wrapper = mount(MdEditor, {
      props: { modelValue: '' },
    })
    const elInput = wrapper.findComponent({ name: 'ElInput' })
    await elInput.vm.$emit('update:model-value', 'new content')
    expect(wrapper.emitted('update:modelValue')).toBeTruthy()
    expect(wrapper.emitted('update:modelValue')![0]).toEqual(['new content'])
  })

  it('renders preview of modelValue via MdViewer', () => {
    const wrapper = mount(MdEditor, {
      props: { modelValue: '# Title' },
    })
    const preview = wrapper.find('.md-editor__preview')
    const html = preview.find('.markdown-body').element.innerHTML
    expect(html).toContain('<h1>')
    expect(html).toContain('Title')
  })

  it('has responsive layout classes', () => {
    const wrapper = mount(MdEditor, {
      props: { modelValue: '' },
    })
    expect(wrapper.find('.md-editor__panes').exists()).toBe(true)
    expect(wrapper.find('.md-editor__edit').exists()).toBe(true)
    expect(wrapper.find('.md-editor__preview').exists()).toBe(true)
  })
})
