import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import VisibilitySelector from '../VisibilitySelector.vue'

describe('VisibilitySelector.vue', () => {
  // ── Rendering ──
  describe('rendering', () => {
    it('renders el-select component', () => {
      const wrapper = mount(VisibilitySelector, {
        props: { modelValue: 0 },
      })
      expect(wrapper.findComponent({ name: 'ElSelect' }).exists()).toBe(true)
    })

    it('shows hint text for selected option', () => {
      const wrapper = mount(VisibilitySelector, {
        props: { modelValue: 0 },
      })
      expect(wrapper.find('.visibility-selector__hint').text()).toBe('所有人可见')
    })

    it('shows correct hint for login-only', () => {
      const wrapper = mount(VisibilitySelector, {
        props: { modelValue: 1 },
      })
      expect(wrapper.find('.visibility-selector__hint').text()).toBe('仅登录用户可见')
    })

    it('shows correct hint for followers-only', () => {
      const wrapper = mount(VisibilitySelector, {
        props: { modelValue: 2 },
      })
      expect(wrapper.find('.visibility-selector__hint').text()).toBe('仅粉丝可见')
    })

    it('shows correct hint for private', () => {
      const wrapper = mount(VisibilitySelector, {
        props: { modelValue: 3 },
      })
      expect(wrapper.find('.visibility-selector__hint').text()).toBe('仅自己可见')
    })

    it('shows empty hint for unknown value', () => {
      const wrapper = mount(VisibilitySelector, {
        props: { modelValue: 99 },
      })
      expect(wrapper.find('.visibility-selector__hint').text()).toBe('')
    })
  })

  // ── Events ──
  describe('events', () => {
    it('emits update:modelValue when option changes', async () => {
      const wrapper = mount(VisibilitySelector, {
        props: { modelValue: 0 },
      })

      const select = wrapper.findComponent({ name: 'ElSelect' })
      await select.vm.$emit('update:modelValue', 2)

      expect(wrapper.emitted('update:modelValue')).toBeTruthy()
      expect(wrapper.emitted('update:modelValue')![0]).toEqual([2])
    })
  })

  // ── Props reactivity ──
  describe('props', () => {
    it('reflects modelValue prop changes', async () => {
      const wrapper = mount(VisibilitySelector, {
        props: { modelValue: 0 },
      })

      expect(wrapper.find('.visibility-selector__hint').text()).toBe('所有人可见')

      await wrapper.setProps({ modelValue: 3 })
      expect(wrapper.find('.visibility-selector__hint').text()).toBe('仅自己可见')
    })
  })
})
