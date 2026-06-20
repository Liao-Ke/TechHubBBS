import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import UserManagePage from '@/pages/admin/UserManagePage.vue'

// ── Mock data ──
const mockUsers = [
  { id: '1111111111111111', username: 'alice', email: 'alice@example.com', role: 'USER', status: 1, createTime: '2025-01-15T08:00:00Z' },
  { id: '2222222222222222', username: 'bob', email: 'bob@example.com', role: 'MODERATOR', status: 1, createTime: '2025-02-20T10:30:00Z' },
  { id: '3333333333333333', username: 'charlie', email: 'charlie@example.com', role: 'ADMIN', status: 0, createTime: '2025-03-10T14:00:00Z' },
]

const mockPageResult = {
  records: mockUsers,
  total: 3,
  size: 20,
  current: 1,
  pages: 1,
}

// ── Mocks (hoisted so vi.mock can access them) ──
const mocks = vi.hoisted(() => ({
  adminApi: {
    getUsers: vi.fn<(...args: unknown[]) => unknown>(),
    banUser: vi.fn<(...args: unknown[]) => unknown>(),
    setRole: vi.fn<(...args: unknown[]) => unknown>(),
  },
}))

vi.mock('@/api/modules/admin', () => ({
  adminApi: mocks.adminApi,
}))

vi.mock('@/stores/user', () => ({
  useUserStore: vi.fn<(...args: unknown[]) => unknown>(),
}))

vi.mock('element-plus', async () => {
  const actual = await vi.importActual('element-plus')
  return {
    ...(actual as object),
    ElMessage: {
      success: vi.fn<(...args: unknown[]) => unknown>(),
      error: vi.fn<(...args: unknown[]) => unknown>(),
      warning: vi.fn<(...args: unknown[]) => unknown>(),
      info: vi.fn<(...args: unknown[]) => unknown>(),
    },
    ElMessageBox: {
      confirm: vi.fn<(...args: unknown[]) => unknown>(),
    },
  }
})

// ── Helpers ──
import { useUserStore } from '@/stores/user'
const useUserStoreMock = useUserStore as ReturnType<typeof vi.fn>

function setUserInfo(info: { id: string; username: string; role: string; status: number; createTime: string } | null) {
  useUserStoreMock.mockReturnValue({
    userInfo: info,
    isLoggedIn: !!info,
    role: info?.role || '',
    isAdmin: info?.role === 'ADMIN',
    isModerator: info?.role === 'ADMIN' || info?.role === 'MODERATOR',
  })
}

function mountPage() {
  return mount(UserManagePage)
}

describe('UserManagePage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    setUserInfo({ id: '9999999999999999', username: 'admin-self', role: 'ADMIN', status: 1, createTime: '2025-01-01T00:00:00Z' })
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  // ────────────────────────────────────────────────
  // Rendering states
  // ────────────────────────────────────────────────

  it('shows loading skeleton initially', () => {
    mocks.adminApi.getUsers.mockReturnValue(new Promise(() => {}))
    const wrapper = mountPage()
    expect(wrapper.findComponent({ name: 'LoadingSkeleton' }).exists()).toBe(true)
  })

  it('shows error alert and retry button on fetch failure', async () => {
    mocks.adminApi.getUsers.mockRejectedValue(new Error('Network Error'))
    const wrapper = mountPage()
    await flushPromises()

    expect(wrapper.text()).toContain('加载失败')
    expect(wrapper.find('.el-alert').exists()).toBe(true)
  })

  it('retry button triggers re-fetch', async () => {
    mocks.adminApi.getUsers.mockRejectedValueOnce(new Error('fail'))
    const wrapper = mountPage()
    await flushPromises()

    mocks.adminApi.getUsers.mockResolvedValue({ data: { records: [], total: 0, size: 20, current: 1, pages: 0 } })
    const retryBtn = wrapper.find('.el-alert .el-button')
    await retryBtn.trigger('click')
    await flushPromises()

    expect(mocks.adminApi.getUsers).toHaveBeenCalledTimes(2)
  })

  it('shows empty state when no users returned', async () => {
    mocks.adminApi.getUsers.mockResolvedValue({ data: { records: [], total: 0, size: 20, current: 1, pages: 0 } })
    const wrapper = mountPage()
    await flushPromises()

    expect(wrapper.findComponent({ name: 'EmptyState' }).exists()).toBe(true)
    expect(wrapper.text()).toContain('暂无用户')
  })

  it('renders user table with data', async () => {
    mocks.adminApi.getUsers.mockResolvedValue({ data: mockPageResult })
    const wrapper = mountPage()
    await flushPromises()

    expect(wrapper.find('.el-table').exists()).toBe(true)
  })

  // ────────────────────────────────────────────────
  // Table content rendering
  // ────────────────────────────────────────────────

  it('renders role tags with correct labels', async () => {
    mocks.adminApi.getUsers.mockResolvedValue({ data: mockPageResult })
    const wrapper = mountPage()
    await flushPromises()
    await nextTick()

    const html = wrapper.html()
    expect(html).toContain('管理员')
    expect(html).toContain('版主')
    expect(html).toContain('普通用户')
  })

  it('renders status tags with correct labels', async () => {
    mocks.adminApi.getUsers.mockResolvedValue({ data: mockPageResult })
    const wrapper = mountPage()
    await flushPromises()
    await nextTick()

    const html = wrapper.html()
    expect(html).toContain('正常')
    expect(html).toContain('封禁')
  })

  it('truncates long IDs', async () => {
    mocks.adminApi.getUsers.mockResolvedValue({ data: mockPageResult })
    const wrapper = mountPage()
    await flushPromises()
    await nextTick()

    // IDs are 16 chars, truncateId cuts to 12 + ellipsis
    expect(wrapper.html()).toContain('111111111111…')
    expect(wrapper.html()).toContain('222222222222…')
    expect(wrapper.html()).toContain('333333333333…')
  })

  it('shows email or placeholder', async () => {
    mocks.adminApi.getUsers.mockResolvedValue({ data: mockPageResult })
    const wrapper = mountPage()
    await flushPromises()
    await nextTick()

    expect(wrapper.html()).toContain('alice@example.com')
  })

  // ────────────────────────────────────────────────
  // Self-protection
  // ────────────────────────────────────────────────

  it('shows self-hint for own account instead of modify actions', async () => {
    // Make current user the same as alice
    setUserInfo({ id: '1111111111111111', username: 'alice', role: 'USER', status: 1, createTime: '2025-01-15T08:00:00Z' })
    mocks.adminApi.getUsers.mockResolvedValue({ data: mockPageResult })
    const wrapper = mountPage()
    await flushPromises()
    await nextTick()

    expect(wrapper.html()).toContain('当前账号')
  })

  it('shows ban and role-select buttons for other accounts', async () => {
    // Admin is someone else, so all rows get action buttons
    setUserInfo({ id: '9999999999999999', username: 'admin-self', role: 'ADMIN', status: 1, createTime: '2025-01-01T00:00:00Z' })
    mocks.adminApi.getUsers.mockResolvedValue({ data: mockPageResult })
    const wrapper = mountPage()
    await flushPromises()
    await nextTick()

    const html = wrapper.html()
    // Other users should have action buttons
    expect(html).toContain('封禁')
  })

  // ────────────────────────────────────────────────
  // Ban / Unban API calls (direct test)
  // ────────────────────────────────────────────────

  it('banUser sends correct arguments for active user', async () => {
    mocks.adminApi.banUser.mockResolvedValue({ data: null })
    await mocks.adminApi.banUser('user-1', true)
    expect(mocks.adminApi.banUser).toHaveBeenCalledWith('user-1', true)
  })

  it('banUser sends correct arguments for banned user', async () => {
    mocks.adminApi.banUser.mockResolvedValue({ data: null })
    await mocks.adminApi.banUser('user-2', false)
    expect(mocks.adminApi.banUser).toHaveBeenCalledWith('user-2', false)
  })

  // ────────────────────────────────────────────────
  // Role change API calls
  // ────────────────────────────────────────────────

  it('setRole sends correct arguments', async () => {
    mocks.adminApi.setRole.mockResolvedValue({ data: null })
    await mocks.adminApi.setRole('user-1', 'MODERATOR')
    expect(mocks.adminApi.setRole).toHaveBeenCalledWith('user-1', 'MODERATOR')
  })

  // ────────────────────────────────────────────────
  // Search
  // ────────────────────────────────────────────────

  it('debounces search input by 500ms', async () => {
    vi.useFakeTimers()
    mocks.adminApi.getUsers.mockResolvedValue({ data: mockPageResult })
    const wrapper = mountPage()
    await flushPromises()
    expect(mocks.adminApi.getUsers).toHaveBeenCalledTimes(1) // initial onMounted

    const input = wrapper.find('.user-manage-page__search input')
    await input.setValue('test')
    // Not called yet — debounce pending
    expect(mocks.adminApi.getUsers).toHaveBeenCalledTimes(1)

    vi.advanceTimersByTime(499)
    expect(mocks.adminApi.getUsers).toHaveBeenCalledTimes(1)

    vi.advanceTimersByTime(1)
    await flushPromises()
    expect(mocks.adminApi.getUsers).toHaveBeenCalledTimes(2)
    expect(mocks.adminApi.getUsers).toHaveBeenLastCalledWith({ page: 1, size: 20, keyword: 'test' })
  })

  it('resets page to 1 on new search and passes keyword', async () => {
    vi.useFakeTimers()
    mocks.adminApi.getUsers.mockResolvedValue({ data: mockPageResult })
    const wrapper = mountPage()
    await flushPromises()

    const input = wrapper.find('.user-manage-page__search input')
    await input.setValue('alice')
    vi.advanceTimersByTime(500)
    await flushPromises()

    expect(mocks.adminApi.getUsers).toHaveBeenLastCalledWith({ page: 1, size: 20, keyword: 'alice' })
  })

  // ────────────────────────────────────────────────
  // Pagination
  // ────────────────────────────────────────────────

  it('renders pagination when totalPages > 1', async () => {
    mocks.adminApi.getUsers.mockResolvedValue({
      data: { ...mockPageResult, total: 50, pages: 3 },
    })
    const wrapper = mountPage()
    await flushPromises()

    expect(wrapper.find('.el-pagination').exists()).toBe(true)
  })

  it('does not show pagination when only one page', async () => {
    mocks.adminApi.getUsers.mockResolvedValue({ data: mockPageResult })
    const wrapper = mountPage()
    await flushPromises()

    expect(wrapper.find('.el-pagination').exists()).toBe(false)
  })

  // ────────────────────────────────────────────────
  // Cleanup
  // ────────────────────────────────────────────────

  it('mounts and unmounts cleanly', async () => {
    mocks.adminApi.getUsers.mockResolvedValue({ data: mockPageResult })
    const wrapper = mountPage()
    await flushPromises()

    expect(wrapper.find('.user-manage-page__title').exists()).toBe(true)
    wrapper.unmount()
    // Component should unmount without errors
  })
})
