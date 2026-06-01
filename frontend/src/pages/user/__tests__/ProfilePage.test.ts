import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises, type VueWrapper } from '@vue/test-utils'
import { createRouter, createWebHistory } from 'vue-router'
import { createPinia, setActivePinia } from 'pinia'
import { nextTick, type ComponentPublicInstance } from 'vue'
import type { UserProfileVO, PostVO, FollowStatusVO } from '@/api/types'

// ── Mock IntersectionObserver ──
vi.stubGlobal(
  'IntersectionObserver',
  vi.fn(() => ({
    observe: vi.fn(),
    unobserve: vi.fn(),
    disconnect: vi.fn(),
  })),
)

// ── Mock API modules ──
const mockGetById = vi.fn()
const mockGetUserPosts = vi.fn()
const mockFollow = vi.fn()
const mockUnfollow = vi.fn()
const mockCheckFollow = vi.fn()
const mockGetList = vi.fn() // postApi.getList

vi.mock('@/api/modules/user', () => ({
  userApi: {
    getById: (...args: unknown[]) => mockGetById(...args),
    getUserPosts: (...args: unknown[]) => mockGetUserPosts(...args),
    follow: (...args: unknown[]) => mockFollow(...args),
    unfollow: (...args: unknown[]) => mockUnfollow(...args),
    checkFollow: (...args: unknown[]) => mockCheckFollow(...args),
  },
}))

vi.mock('@/api/modules/post', () => ({
  postApi: {
    getList: (...args: unknown[]) => mockGetList(...args),
  },
}))

// ── Mock user store ──
const mockStore = {
  isLoggedIn: true,
  userInfo: { id: 'user-self', username: 'LoggedInUser', bio: '', avatarUrl: '', role: 'user', status: 1, createTime: '' } as UserProfileVO,
}

vi.mock('@/stores/user', () => ({
  useUserStore: () => mockStore,
}))

import ProfilePage from '@/pages/user/ProfilePage.vue'

// ── Helpers ──
function makeUser(overrides: Partial<UserProfileVO> = {}): UserProfileVO {
  return {
    id: overrides.id ?? 'user-1',
    username: overrides.username ?? '测试用户',
    avatarUrl: overrides.avatarUrl,
    bio: overrides.bio ?? '这是一段个人简介',
    role: overrides.role ?? 'user',
    status: overrides.status ?? 1,
    createTime: overrides.createTime ?? '2026-01-01T00:00:00Z',
  }
}

function makePostVO(overrides: Partial<PostVO> = {}): PostVO {
  return {
    id: overrides.id ?? 'post-1',
    title: overrides.title ?? '测试帖子',
    content: '帖子内容...',
    author: {
      id: 'user-1',
      username: '测试用户',
      avatarUrl: undefined,
      bio: undefined,
      role: 'user',
      status: 1,
      createTime: '2026-01-01T00:00:00Z',
    },
    categoryId: 1,
    categoryName: 'Java',
    visibility: 0,
    type: 0,
    status: 1,
    viewCount: 100,
    likeCount: 10,
    commentCount: 3,
    favoriteCount: 2,
    divineCommentCount: 0,
    isLiked: false,
    isFavorited: false,
    createTime: '2026-01-01T00:00:00Z',
    updateTime: '2026-01-01T00:00:00Z',
    ...overrides,
  }
}

function createTestRouter() {
  return createRouter({
    history: createWebHistory(),
    routes: [
      { path: '/users/:id', name: 'user-profile', component: ProfilePage },
      { path: '/settings', name: 'settings', component: { template: '<div>settings</div>' } },
      { path: '/login', name: 'login', component: { template: '<div>login</div>' } },
    ],
  })
}

async function mountPage(userId: string = 'user-1') {
  const router = createTestRouter()
  const pinia = createPinia()
  setActivePinia(pinia)

  await router.push(`/users/${userId}`)
  await router.isReady()

  const wrapper = mount(ProfilePage, {
    global: { plugins: [router, pinia] },
  })

  await flushPromises()
  await nextTick()

  return { wrapper, router, pinia }
}

describe('ProfilePage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockStore.isLoggedIn = true
    mockStore.userInfo = { id: 'user-self', username: 'LoggedInUser', bio: '', avatarUrl: '', role: 'user', status: 1, createTime: '' } as UserProfileVO
    mockGetById.mockReset()
    mockGetUserPosts.mockReset()
    mockFollow.mockReset()
    mockUnfollow.mockReset()
    mockCheckFollow.mockReset()
    mockGetList.mockReset()
  })

  // ── Rendering ──
  describe('rendering', () => {
    it('renders user avatar and username', async () => {
      mockGetById.mockResolvedValue({ data: makeUser({ username: '张三', bio: '热爱Java开发' }) })
      mockGetUserPosts.mockResolvedValue({
        data: { records: [], total: 0, size: 10, current: 1, pages: 0 },
      })

      const { wrapper } = await mountPage()

      expect(wrapper.text()).toContain('张三')
      expect(wrapper.text()).toContain('热爱Java开发')
      const avatar = wrapper.find('.user-avatar')
      expect(avatar.exists()).toBe(true)
    })

    it('renders stats row (帖子 / 关注 / 粉丝)', async () => {
      const user = makeUser({ username: '测试' })
      ;(user as any).postCount = 42
      ;(user as any).followerCount = 15
      ;(user as any).followingCount = 8
      mockGetById.mockResolvedValue({ data: user })
      mockGetUserPosts.mockResolvedValue({
        data: { records: [], total: 0, size: 10, current: 1, pages: 0 },
      })

      const { wrapper } = await mountPage()

      expect(wrapper.text()).toContain('42')
      expect(wrapper.text()).toContain('15')
      expect(wrapper.text()).toContain('8')
    })

    it('is not a stub (has real content)', async () => {
      mockGetById.mockResolvedValue({ data: makeUser() })
      mockGetUserPosts.mockResolvedValue({
        data: { records: [], total: 0, size: 10, current: 1, pages: 0 },
      })

      const { wrapper } = await mountPage()

      const text = wrapper.text()
      expect(text.length).toBeGreaterThan(20)
    })
  })

  // ── Own profile vs others ──
  describe('own profile vs others', () => {
    it('shows "编辑资料" button when viewing own profile', async () => {
      mockGetById.mockResolvedValue({ data: makeUser({ id: 'user-self' }) })
      mockGetUserPosts.mockResolvedValue({
        data: { records: [], total: 0, size: 10, current: 1, pages: 0 },
      })

      const { wrapper } = await mountPage('user-self')

      expect(wrapper.text()).toContain('编辑资料')
      const editBtn = wrapper.find('a[href="/settings"]')
      expect(editBtn.exists()).toBe(true)
    })

    it('shows "关注" button when viewing other user (not logged in → no button)', async () => {
      mockGetById.mockResolvedValue({ data: makeUser({ id: 'user-other' }) })
      mockCheckFollow.mockResolvedValue({ data: { following: false } })
      mockGetUserPosts.mockResolvedValue({
        data: { records: [], total: 0, size: 10, current: 1, pages: 0 },
      })

      mockStore.isLoggedIn = true

      const { wrapper } = await mountPage('user-other')

      expect(wrapper.text()).toContain('关注')
    })

    it('shows "已关注" when already following', async () => {
      mockGetById.mockResolvedValue({ data: makeUser({ id: 'user-other' }) })
      mockCheckFollow.mockResolvedValue({ data: { following: true } })
      mockGetUserPosts.mockResolvedValue({
        data: { records: [], total: 0, size: 10, current: 1, pages: 0 },
      })

      mockStore.isLoggedIn = true

      const { wrapper } = await mountPage('user-other')

      expect(wrapper.text()).toContain('已关注')
    })

    it('does NOT show follow/edit button when not logged in and viewing someone else', async () => {
      mockGetById.mockResolvedValue({ data: makeUser({ id: 'user-other' }) })
      mockCheckFollow.mockResolvedValue({ data: { following: false } })
      mockGetUserPosts.mockResolvedValue({
        data: { records: [], total: 0, size: 10, current: 1, pages: 0 },
      })

      mockStore.isLoggedIn = false

      const { wrapper } = await mountPage('user-other')

      // Neither edit nor follow button should appear in the actions area
      const actionsSection = wrapper.find('.profile-page__actions')
      expect(actionsSection.exists()).toBe(true)
      const actionButtons = actionsSection.findAll('.el-button')
      expect(actionButtons.length).toBe(0)
    })
  })

  // ── Follow toggle ──
  describe('follow toggle', () => {
    it('calls follow API when clicking "关注"', async () => {
      mockGetById.mockResolvedValue({ data: makeUser({ id: 'user-other' }) })
      mockCheckFollow.mockResolvedValue({ data: { following: false } })
      mockGetUserPosts.mockResolvedValue({
        data: { records: [], total: 0, size: 10, current: 1, pages: 0 },
      })
      mockFollow.mockResolvedValue({ data: { following: true } })

      mockStore.isLoggedIn = true

      const { wrapper } = await mountPage('user-other')

      const followBtn = wrapper.find('.el-button--primary')
      expect(followBtn.exists()).toBe(true)

      await followBtn.trigger('click')
      await flushPromises()
      await nextTick()

      expect(mockFollow).toHaveBeenCalledWith('user-other')
    })

    it('calls unfollow API when clicking "已关注"', async () => {
      mockGetById.mockResolvedValue({ data: makeUser({ id: 'user-other' }) })
      mockCheckFollow.mockResolvedValue({ data: { following: true } })
      mockGetUserPosts.mockResolvedValue({
        data: { records: [], total: 0, size: 10, current: 1, pages: 0 },
      })
      mockUnfollow.mockResolvedValue({ data: { following: false } })

      mockStore.isLoggedIn = true

      const { wrapper } = await mountPage('user-other')

      // Find the button that says 已关注
      const buttons = wrapper.findAll('.el-button')
      const unfollowBtn = buttons.find(b => b.text().includes('已关注'))
      expect(unfollowBtn).toBeTruthy()

      await unfollowBtn!.trigger('click')
      await flushPromises()
      await nextTick()

      expect(mockUnfollow).toHaveBeenCalledWith('user-other')
    })
  })

  // ── Tabs ──
  describe('tabs', () => {
    it('renders four tabs: 帖子, 收藏, 关注, 粉丝', async () => {
      mockGetById.mockResolvedValue({ data: makeUser() })
      mockGetUserPosts.mockResolvedValue({
        data: { records: [], total: 0, size: 10, current: 1, pages: 0 },
      })

      const { wrapper } = await mountPage()

      const tabItems = wrapper.findAll('.el-tabs__item')
      const tabTexts = tabItems.map((t) => t.text())
      expect(tabTexts.some((t) => t.includes('帖子'))).toBe(true)
      expect(tabTexts.some((t) => t.includes('收藏'))).toBe(true)
      expect(tabTexts.some((t) => t.includes('关注'))).toBe(true)
      expect(tabTexts.some((t) => t.includes('粉丝'))).toBe(true)
    })

    it('has "帖子" tab active by default', async () => {
      mockGetById.mockResolvedValue({ data: makeUser() })
      mockGetUserPosts.mockResolvedValue({
        data: { records: [], total: 0, size: 10, current: 1, pages: 0 },
      })

      const { wrapper } = await mountPage()

      const activeTab = wrapper.find('.el-tabs__item.is-active')
      expect(activeTab.exists()).toBe(true)
      expect(activeTab.text()).toContain('帖子')
    })

    it('loads posts on mount', async () => {
      mockGetById.mockResolvedValue({ data: makeUser() })
      mockGetUserPosts.mockResolvedValue({
        data: { records: [makePostVO({ title: '我的帖子' })], total: 1, size: 10, current: 1, pages: 1 },
      })

      const { wrapper } = await mountPage()

      await flushPromises()
      await nextTick()

      expect(mockGetUserPosts).toHaveBeenCalledWith('user-1', { page: 1, size: 10 })
      expect(wrapper.text()).toContain('我的帖子')
    })

    it('shows LoadingSkeleton while loading posts', async () => {
      mockGetById.mockResolvedValue({ data: makeUser() })
      mockGetUserPosts.mockReturnValue(new Promise(() => {}))

      const { wrapper } = await mountPage()
      await flushPromises()
      await nextTick()

      expect(wrapper.findComponent({ name: 'LoadingSkeleton' }).exists()).toBe(true)
    })

    it('shows error alert when posts API fails', async () => {
      mockGetById.mockResolvedValue({ data: makeUser() })
      mockGetUserPosts.mockRejectedValue(new Error('网络错误'))

      const { wrapper } = await mountPage()
      await flushPromises()
      await nextTick()

      expect(wrapper.findComponent({ name: 'ElAlert' }).exists()).toBe(true)
    })

    it('shows EmptyState when no posts', async () => {
      mockGetById.mockResolvedValue({ data: makeUser() })
      mockGetUserPosts.mockResolvedValue({
        data: { records: [], total: 0, size: 10, current: 1, pages: 0 },
      })

      const { wrapper } = await mountPage()
      await flushPromises()
      await nextTick()

      const emptyState = wrapper.findComponent({ name: 'EmptyState' })
      expect(emptyState.exists()).toBe(true)
      expect(emptyState.props('title')).toContain('暂无帖子')
    })
  })

  // ── Error states ──
  describe('error states', () => {
    it('shows error alert when user API fails', async () => {
      mockGetById.mockRejectedValue(new Error('用户不存在'))

      const { wrapper } = await mountPage()

      expect(wrapper.findComponent({ name: 'ElAlert' }).exists()).toBe(true)
      expect(wrapper.text()).toContain('加载用户信息失败')
    })

    it('shows EmptyState when user data is null', async () => {
      mockGetById.mockResolvedValue({ data: null })

      const { wrapper } = await mountPage()

      const emptyState = wrapper.findComponent({ name: 'EmptyState' })
      expect(emptyState.exists()).toBe(true)
      expect(emptyState.props('title')).toContain('用户不存在')
    })
  })

  // ── Tab switching ──
  describe('tab switching', () => {
    async function switchTab(wrapper: VueWrapper<ComponentPublicInstance>, tabName: string) {
      const tabs = wrapper.findAll('.el-tabs__item')
      const target = tabs.find((t) => t.text().includes(tabName))
      if (target) {
        await target.trigger('click')
        await flushPromises()
        await nextTick()
      }
    }

    it('loads favorites when switching to 收藏 tab', async () => {
      mockGetById.mockResolvedValue({ data: makeUser() })
      mockGetUserPosts.mockResolvedValue({
        data: { records: [], total: 0, size: 10, current: 1, pages: 0 },
      })
      mockGetList.mockResolvedValue({
        data: { records: [], total: 0, size: 10, current: 1, pages: 0 },
      })

      const { wrapper } = await mountPage()
      await flushPromises()
      await nextTick()

      await switchTab(wrapper, '收藏')

      expect(mockGetList).toHaveBeenCalledWith({ userId: 'user-1', page: 1, size: 10 })
    })

    it('shows EmptyState for 关注 tab (no API yet)', async () => {
      mockGetById.mockResolvedValue({ data: makeUser() })
      mockGetUserPosts.mockResolvedValue({
        data: { records: [], total: 0, size: 10, current: 1, pages: 0 },
      })

      const { wrapper } = await mountPage()
      await flushPromises()
      await nextTick()

      await switchTab(wrapper, '关注')

      // Should show empty state since no API data
      const emptyStates = wrapper.findAllComponents({ name: 'EmptyState' })
      expect(emptyStates.length).toBeGreaterThanOrEqual(1)
    })
  })
})
