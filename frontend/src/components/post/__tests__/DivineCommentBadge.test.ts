import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import DivineCommentBadge from '../DivineCommentBadge.vue'

describe('DivineCommentBadge.vue', () => {
  // ── True state ──
  describe('when isDivine is true', () => {
    it('renders the tag', () => {
      const wrapper = mount(DivineCommentBadge, {
        props: { isDivine: true },
      })
      expect(wrapper.findComponent({ name: 'ElTag' }).exists()).toBe(true)
    })

    it('displays 神评 text', () => {
      const wrapper = mount(DivineCommentBadge, {
        props: { isDivine: true },
      })
      expect(wrapper.text()).toContain('神评')
    })

    it('has warning type and dark effect', () => {
      const wrapper = mount(DivineCommentBadge, {
        props: { isDivine: true },
      })
      const tag = wrapper.findComponent({ name: 'ElTag' })
      expect(tag.props('type')).toBe('warning')
      expect(tag.props('effect')).toBe('dark')
      expect(tag.props('size')).toBe('small')
    })
  })

  // ── False state ──
  describe('when isDivine is false', () => {
    it('renders nothing', () => {
      const wrapper = mount(DivineCommentBadge, {
        props: { isDivine: false },
      })
      expect(wrapper.html()).toBe('<!--v-if-->')
    })

    it('does not contain 神评 text', () => {
      const wrapper = mount(DivineCommentBadge, {
        props: { isDivine: false },
      })
      expect(wrapper.text()).toBe('')
    })
  })

  // ── Default ──
  describe('default state', () => {
    it('defaults to false (no prop)', () => {
      const wrapper = mount(DivineCommentBadge)
      expect(wrapper.text()).toBe('')
    })
  })
})
