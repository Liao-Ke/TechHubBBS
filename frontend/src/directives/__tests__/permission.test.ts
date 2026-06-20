import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { vPermission } from '../permission'

// Mock return value — change per test case
let mockRole: string | undefined = 'USER'

vi.mock('@/stores/user', () => ({
  useUserStore: () => ({
    get role() {
      return mockRole
    },
    token: 'fake-token',
    userInfo: null,
    isLoggedIn: true,
  }),
}))

beforeEach(() => {
  // Default: user is USER
  mockRole = 'USER'
})

afterEach(() => {
  vi.restoreAllMocks()
})

/**
 * Helper: mount a minimal component with the v-permission directive applied.
 * Returns the wrapper and a reference to the target div.
 */
function mountWithPermission(value: string | string[]) {
  const wrapper = mount(
    {
      template: '<div v-permission="value" class="target">content</div>',
      data() {
        return { value }
      },
    },
    {
      global: {
        directives: { permission: vPermission },
      },
    },
  )
  return wrapper
}

describe('v-permission directive', () => {
  it('shows element when role matches exactly', async () => {
    mockRole = 'ADMIN'
    const wrapper = mountWithPermission('ADMIN')
    // Wait for the async directive to resolve
    await new Promise((resolve) => setTimeout(resolve, 0))
    const target = wrapper.find('.target')
    expect(target.exists()).toBe(true)
    expect(target.isVisible()).toBe(true)
  })

  it('removes element when role does not match', async () => {
    mockRole = 'USER'
    const wrapper = mountWithPermission('ADMIN')
    await new Promise((resolve) => setTimeout(resolve, 0))
    const target = wrapper.find('.target')
    expect(target.exists()).toBe(true)
    expect(target.isVisible()).toBe(false)
  })

  it('accepts a single string role', async () => {
    mockRole = 'MODERATOR'
    const wrapper = mountWithPermission('MODERATOR')
    await new Promise((resolve) => setTimeout(resolve, 0))
    const target = wrapper.find('.target')
    expect(target.exists()).toBe(true)
    expect(target.isVisible()).toBe(true)
  })

  it('accepts an array of roles', async () => {
    mockRole = 'MODERATOR'
    const wrapper = mountWithPermission(['ADMIN', 'MODERATOR'])
    await new Promise((resolve) => setTimeout(resolve, 0))
    const target = wrapper.find('.target')
    expect(target.exists()).toBe(true)
    expect(target.isVisible()).toBe(true)
  })

  it('allows higher-role user to see lower-role content (hierarchy)', async () => {
    mockRole = 'ADMIN'
    const wrapper = mountWithPermission('USER')
    await new Promise((resolve) => setTimeout(resolve, 0))
    const target = wrapper.find('.target')
    expect(target.exists()).toBe(true)
    expect(target.isVisible()).toBe(true)
  })

  it('removes element when user role is undefined', async () => {
    mockRole = undefined
    const wrapper = mountWithPermission('USER')
    await new Promise((resolve) => setTimeout(resolve, 0))
    const target = wrapper.find('.target')
    expect(target.exists()).toBe(true)
    expect(target.isVisible()).toBe(false)
  })

  it('removes element when user role is unknown', async () => {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    mockRole = 'SOME_UNKNOWN_ROLE' as any
    const wrapper = mountWithPermission('USER')
    await new Promise((resolve) => setTimeout(resolve, 0))
    const target = wrapper.find('.target')
    expect(target.exists()).toBe(true)
    expect(target.isVisible()).toBe(false)
  })
})
