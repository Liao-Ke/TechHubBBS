import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createRouter, createWebHistory } from 'vue-router'
import { createPinia, setActivePinia } from 'pinia'
import { nextTick } from 'vue'
import type { UserProfileVO } from '@/api/types'

// ── Mock API modules ──
const mockUpdateMe = vi.fn()
const mockApi = vi.fn() // direct api() call for password change

vi.mock('@/api/modules/user', () => ({
  userApi: {
    updateMe: (...args: unknown[]) => mockUpdateMe(...args),
  },
}))

vi.mock('@/api', () => ({
  api: (...args: unknown[]) => mockApi(...args),
}))

// ── Mock user store ──
const mockStore = {
  isLoggedIn: true,
  userInfo: {
    id: 'user-1',
    username: 'TestUser',
    avatarUrl: 'https://example.com/avatar.jpg',
    bio: 'My current bio',
    role: 'user',
    status: 1,
    createTime: '2026-01-01T00:00:00Z',
  } as UserProfileVO,
  fetchUserInfo: vi.fn(),
}

vi.mock('@/stores/user', () => ({
  useUserStore: () => mockStore,
}))

// ── Mock router push ──
const mockPush = vi.fn()
vi.mock('vue-router', async () => {
  const actual = await vi.importActual('vue-router') as { createRouter: typeof createRouter; createWebHistory: typeof createWebHistory; [key: string]: unknown }
  return {
    ...actual,
    useRouter: () => ({
      ...actual.createRouter({ history: actual.createWebHistory(), routes: [] }),
      push: mockPush,
      replace: mockPush,
    }),
  }
})

// ── Mock ElMessage ──
vi.mock('element-plus', async () => {
  const actual = await vi.importActual('element-plus')
  return {
    ...actual,
    ElMessage: {
      success: vi.fn(),
      error: vi.fn(),
    },
  }
})

import SettingsPage from '@/pages/user/SettingsPage.vue'
import { ElMessage } from 'element-plus'

// ── Helpers ──
function createTestRouter() {
  return createRouter({
    history: createWebHistory(),
    routes: [
      { path: '/settings', name: 'settings', component: SettingsPage },
      { path: '/login', name: 'login', component: { template: '<div>login</div>' } },
      { path: '/', name: 'home', component: { template: '<div>home</div>' } },
    ],
  })
}

async function mountPage() {
  const router = createTestRouter()
  const pinia = createPinia()
  setActivePinia(pinia)

  await router.push('/settings')
  await router.isReady()

  const wrapper = mount(SettingsPage, {
    global: { plugins: [router, pinia] },
  })

  await flushPromises()
  await nextTick()

  return { wrapper, router, pinia }
}

describe('SettingsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockStore.isLoggedIn = true
    mockStore.userInfo = {
      id: 'user-1',
      username: 'TestUser',
      avatarUrl: 'https://example.com/avatar.jpg',
      bio: 'My current bio',
      role: 'user',
      status: 1,
      createTime: '2026-01-01T00:00:00Z',
    } as UserProfileVO
    mockStore.fetchUserInfo = vi.fn()
    mockUpdateMe.mockReset()
    mockApi.mockReset()
    mockPush.mockReset()
  })

  // ── Rendering ──
  describe('rendering', () => {
    it('renders page title "编辑资料"', async () => {
      const { wrapper } = await mountPage()

      expect(wrapper.text()).toContain('编辑资料')
    })

    it('renders UserAvatar component', async () => {
      const { wrapper } = await mountPage()

      expect(wrapper.findComponent({ name: 'UserAvatar' }).exists()).toBe(true)
    })

    it('renders ImageUpload component', async () => {
      const { wrapper } = await mountPage()

      expect(wrapper.findComponent({ name: 'ImageUpload' }).exists()).toBe(true)
    })

    it('renders bio textarea with current bio value', async () => {
      const { wrapper } = await mountPage()

      const textarea = wrapper.find('textarea')
      expect(textarea.exists()).toBe(true)
      expect(textarea.element.value).toContain('My current bio')
    })

    it('renders password change toggle', async () => {
      const { wrapper } = await mountPage()

      expect(wrapper.text()).toContain('修改密码')
    })

    it('renders save button', async () => {
      const { wrapper } = await mountPage()

      const saveBtn = wrapper.find('.el-button--primary')
      expect(saveBtn.exists()).toBe(true)
      expect(saveBtn.text()).toContain('保存修改')
    })

    it('is not a stub (has real content)', async () => {
      const { wrapper } = await mountPage()

      const text = wrapper.text()
      expect(text.length).toBeGreaterThan(20)
    })
  })

  // ── Auth guard ──
  describe('auth guard', () => {
    it('redirects to /login when not logged in', async () => {
      mockStore.isLoggedIn = false

      await mountPage()

      expect(mockPush).toHaveBeenCalledWith('/login?redirect=/settings')
    })
  })

  // ── Password section toggle ──
  describe('password section', () => {
    it('hides password fields by default', async () => {
      const { wrapper } = await mountPage()

      const passwordInputs = wrapper.findAll('input[type="password"]')
      expect(passwordInputs.length).toBe(0)
    })

    it('shows password fields when toggle is clicked', async () => {
      const { wrapper } = await mountPage()

      const toggle = wrapper.find('.settings-page__section-toggle')
      expect(toggle.exists()).toBe(true)

      await toggle.trigger('click')
      await nextTick()

      const passwordInputs = wrapper.findAll('input[type="password"]')
      expect(passwordInputs.length).toBeGreaterThanOrEqual(3)
    })

    it('shows three password fields: current, new, confirm', async () => {
      const { wrapper } = await mountPage()

      const toggle = wrapper.find('.settings-page__section-toggle')
      await toggle.trigger('click')
      await nextTick()

      const labels = wrapper.findAll('.el-form-item__label')
      const labelTexts = labels.map((l) => l.text())
      expect(labelTexts.some((t) => t.includes('当前密码'))).toBe(true)
      expect(labelTexts.some((t) => t.includes('新密码'))).toBe(true)
      expect(labelTexts.some((t) => t.includes('确认新密码'))).toBe(true)
    })
  })

  // ── Form validation ──
  describe('form validation', () => {
    it('validates bio max length (500 chars)', async () => {
      const { wrapper } = await mountPage()

      const textarea = wrapper.find('textarea')
      expect(textarea.attributes('maxlength')).toBe('500')
    })

    it('validates password confirmation mismatch', async () => {
      const { wrapper } = await mountPage()

      // Open password section
      const toggle = wrapper.find('.settings-page__section-toggle')
      await toggle.trigger('click')
      await nextTick()

      // Fill fields with mismatched passwords
      const inputs = wrapper.findAll('input[type="password"]')
      await inputs[0]!.setValue('oldpass')
      await inputs[1]!.setValue('newpass123')
      await inputs[2]!.setValue('different')
      await nextTick()

      // Trigger validation by submitting
      const saveBtn = wrapper.find('.el-button--primary')
      await saveBtn.trigger('click')
      await flushPromises()
      await nextTick()

      // Should not call API because validation fails
      expect(mockUpdateMe).not.toHaveBeenCalled()
    })
  })

  // ── Save ──
  describe('save', () => {
    it('calls updateMe with avatar and bio', async () => {
      mockUpdateMe.mockResolvedValue({ data: {} })

      const { wrapper } = await mountPage()

      const textarea = wrapper.find('textarea')
      await textarea.setValue('Updated bio text')
      await nextTick()

      const saveBtn = wrapper.find('.el-button--primary')
      await saveBtn.trigger('click')
      await flushPromises()
      await nextTick()

      expect(mockUpdateMe).toHaveBeenCalledWith({
        avatarUrl: 'https://example.com/avatar.jpg',
        bio: 'Updated bio text',
      })
    })

    it('shows loading state while saving', async () => {
      // Never resolve to keep saving=true
      mockUpdateMe.mockReturnValue(new Promise(() => {}))

      const { wrapper } = await mountPage()

      const saveBtn = wrapper.find('.el-button--primary')
      await saveBtn.trigger('click')
      // Wait for form validation and DOM update
      await flushPromises()
      await nextTick()

      // The button should be in loading state (saving=true → :loading="true")
      expect(saveBtn.attributes('disabled')).toBeDefined()
    })

    it('calls password change API when passwords are filled', async () => {
      mockUpdateMe.mockResolvedValue({ data: {} })
      mockApi.mockResolvedValue({ data: {} })

      const { wrapper } = await mountPage()

      // Open password section
      const toggle = wrapper.find('.settings-page__section-toggle')
      await toggle.trigger('click')
      await nextTick()

      // Fill password fields
      const inputs = wrapper.findAll('input[type="password"]')
      await inputs[0]!.setValue('oldpass')
      await inputs[1]!.setValue('newpass123')
      await inputs[2]!.setValue('newpass123')
      await nextTick()

      const saveBtn = wrapper.find('.el-button--primary')
      await saveBtn.trigger('click')
      await flushPromises()
      await nextTick()

      expect(mockUpdateMe).toHaveBeenCalled()
      expect(mockApi).toHaveBeenCalledWith('/users/me/password', {
        method: 'PATCH',
        body: { oldPassword: 'oldpass', newPassword: 'newpass123' },
      })
    })

    it('shows success message after save', async () => {
      mockUpdateMe.mockResolvedValue({ data: {} })

      const { wrapper } = await mountPage()

      const saveBtn = wrapper.find('.el-button--primary')
      await saveBtn.trigger('click')
      await flushPromises()
      await nextTick()

      expect(ElMessage.success).toHaveBeenCalledWith('更新成功')
    })

    it('shows error message on save failure', async () => {
      mockUpdateMe.mockRejectedValue(new Error('服务器错误'))

      const { wrapper } = await mountPage()

      const saveBtn = wrapper.find('.el-button--primary')
      await saveBtn.trigger('click')
      await flushPromises()
      await nextTick()

      expect(ElMessage.error).toHaveBeenCalled()
    })

    it('calls fetchUserInfo after successful save', async () => {
      mockUpdateMe.mockResolvedValue({ data: {} })

      const { wrapper } = await mountPage()

      const saveBtn = wrapper.find('.el-button--primary')
      await saveBtn.trigger('click')
      await flushPromises()
      await nextTick()

      expect(mockStore.fetchUserInfo).toHaveBeenCalled()
    })
  })

  // ── Avatar upload ──
  describe('avatar upload', () => {
    it('updates avatar preview on upload success', async () => {
      const { wrapper } = await mountPage()

      const imageUpload = wrapper.findComponent({ name: 'ImageUpload' })
      expect(imageUpload.exists()).toBe(true)

      await imageUpload.vm.$emit('upload-success', 'https://example.com/new-avatar.jpg')
      await nextTick()

      // The avatarUrl ref should be updated, and UserAvatar should receive new src
      const userAvatar = wrapper.findComponent({ name: 'UserAvatar' })
      expect(userAvatar.props('src')).toBe('https://example.com/new-avatar.jpg')
    })
  })
})
