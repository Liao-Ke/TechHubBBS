import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import Toolbar from '../Toolbar.vue'

describe('Toolbar', () => {
  it('renders all toolbar buttons', () => {
    const wrapper = mount(Toolbar)
    const buttons = wrapper.findAll('.md-toolbar__btn')
    expect(buttons.length).toBe(12)
  })

  it('emits insert with bold syntax on bold button click', async () => {
    const wrapper = mount(Toolbar)
    const boldBtn = wrapper.findAll('.md-toolbar__btn')[0]!
    await boldBtn.trigger('click')
    expect(wrapper.emitted('insert')).toBeTruthy()
    expect(wrapper.emitted('insert')![0]).toEqual(['**text**'])
  })

  it('emits insert with italic syntax on italic button click', async () => {
    const wrapper = mount(Toolbar)
    const btn = wrapper.findAll('.md-toolbar__btn')[1]!
    await btn.trigger('click')
    expect(wrapper.emitted('insert')![0]).toEqual(['*text*'])
  })

  it('emits insert with strikethrough syntax', async () => {
    const wrapper = mount(Toolbar)
    const btn = wrapper.findAll('.md-toolbar__btn')[2]!
    await btn.trigger('click')
    expect(wrapper.emitted('insert')![0]).toEqual(['~~text~~'])
  })

  it('emits insert with H1 syntax', async () => {
    const wrapper = mount(Toolbar)
    const btn = wrapper.findAll('.md-toolbar__btn')[3]!
    await btn.trigger('click')
    expect(wrapper.emitted('insert')![0]).toEqual(['# '])
  })

  it('emits insert with H2 syntax', async () => {
    const wrapper = mount(Toolbar)
    const btn = wrapper.findAll('.md-toolbar__btn')[4]!
    await btn.trigger('click')
    expect(wrapper.emitted('insert')![0]).toEqual(['## '])
  })

  it('emits insert with H3 syntax', async () => {
    const wrapper = mount(Toolbar)
    const btn = wrapper.findAll('.md-toolbar__btn')[5]!
    await btn.trigger('click')
    expect(wrapper.emitted('insert')![0]).toEqual(['### '])
  })

  it('emits insert with link syntax', async () => {
    const wrapper = mount(Toolbar)
    const btn = wrapper.findAll('.md-toolbar__btn')[6]!
    await btn.trigger('click')
    expect(wrapper.emitted('insert')![0]).toEqual(['[text](url)'])
  })

  it('emits insert with image syntax', async () => {
    const wrapper = mount(Toolbar)
    const btn = wrapper.findAll('.md-toolbar__btn')[7]!
    await btn.trigger('click')
    expect(wrapper.emitted('insert')![0]).toEqual(['![alt](url)'])
  })

  it('emits insert with inline code syntax', async () => {
    const wrapper = mount(Toolbar)
    const btn = wrapper.findAll('.md-toolbar__btn')[8]!
    await btn.trigger('click')
    expect(wrapper.emitted('insert')![0]).toEqual(['`code`'])
  })

  it('emits insert with code block syntax', async () => {
    const wrapper = mount(Toolbar)
    const btn = wrapper.findAll('.md-toolbar__btn')[9]!
    await btn.trigger('click')
    expect(wrapper.emitted('insert')![0]).toEqual(['```\n\n```'])
  })

  it('emits insert with blockquote syntax', async () => {
    const wrapper = mount(Toolbar)
    const btn = wrapper.findAll('.md-toolbar__btn')[10]!
    await btn.trigger('click')
    expect(wrapper.emitted('insert')![0]).toEqual(['> '])
  })

  it('emits insert with unordered list syntax', async () => {
    const wrapper = mount(Toolbar)
    const btn = wrapper.findAll('.md-toolbar__btn')[11]!
    await btn.trigger('click')
    expect(wrapper.emitted('insert')![0]).toEqual(['- '])
  })

  it('has correct button titles for accessibility', () => {
    const wrapper = mount(Toolbar)
    const titles = wrapper.findAll('.md-toolbar__btn').map(btn => btn.attributes('title'))
    expect(titles).toEqual([
      '粗体', '斜体', '删除线',
      '一级标题', '二级标题', '三级标题',
      '链接', '图片', '行内代码', '代码块',
      '引用', '无序列表',
    ])
  })
})
