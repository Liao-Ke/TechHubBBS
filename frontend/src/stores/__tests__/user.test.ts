import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'

// Mock auth API module
vi.mock('@/api/modules/auth', () => ({
  authApi: {
    login: vi.fn<(...args: unknown[]) => unknown>(),
    register: vi.fn<(...args: unknown[]) => unknown>(),
  },
}))

// Mock user API module
vi.mock('@/api/modules/user', () => ({
  userApi: {
    getMe: vi.fn<(...args: unknown[]) => unknown>(),
  },
}))

import { useUserStore } from '@/stores/user'
import { authApi } from '@/api/modules/auth'
import { userApi } from '@/api/modules/user'
import type { LoginResponse, UserProfileVO, UserDetailVO } from '@/api/types'

const TOKEN_KEY = 'techhub_token'

const mockLoginResponse: LoginResponse = {
  token: 'jwt-token-abc123',
  tokenType: 'Bearer',
  userId: 'user-1',
  username: 'testuser',
  role: 'USER',
}

const mockUserDetail: UserDetailVO = {
  id: 'user-1',
  username: 'testuser',
  avatarUrl: 'https://example.com/avatar.png',
  bio: 'Hello world',
  role: 'USER',
  status: 1,
  createTime: '2025-01-01T00:00:00Z',
  email: 'testuser@example.com',
  updateTime: '2025-01-01T00:00:00Z',
}

const mockUserProfile: UserProfileVO = {
  id: 'user-1',
  username: 'testuser',
  avatarUrl: 'https://example.com/avatar.png',
  bio: 'Hello world',
  role: 'USER',
  status: 1,
  createTime: '2025-01-01T00:00:00Z',
}

const mockAdminDetail: UserDetailVO = {
  ...mockUserDetail,
  role: 'ADMIN',
}

const mockAdminProfile: UserProfileVO = {
  ...mockUserProfile,
  role: 'ADMIN',
}

const mockModeratorProfile: UserProfileVO = {
  ...mockUserProfile,
  role: 'MODERATOR',
}

describe('useUserStore', () => {
  beforeEach(() => {
    // Reset Pinia state between tests
    setActivePinia(createPinia())
    // Mock localStorage
    const store: Record<string, string> = {}
    vi.stubGlobal('localStorage', {
      getItem: vi.fn((key: string) => store[key] ?? null),
      setItem: vi.fn((key: string, value: string) => { store[key] = value }),
      removeItem: vi.fn((key: string) => { delete store[key] }),
      clear: vi.fn(() => { Object.keys(store).forEach(k => delete store[k]) }),
      key: vi.fn<(...args: unknown[]) => unknown>(),
      length: 0,
    })
    // Clear mocks between tests
    vi.clearAllMocks()
  })

  describe('initial state', () => {
    it('is not logged in by default', () => {
      const store = useUserStore()
      expect(store.isLoggedIn).toBe(false)
      expect(store.token).toBeNull()
      expect(store.userInfo).toBeNull()
    })

    it('has empty role when not logged in', () => {
      const store = useUserStore()
      expect(store.role).toBe('')
    })
  })

  describe('login', () => {
    it('stores token and fetches user info on successful login', async () => {
      vi.mocked(authApi.login).mockResolvedValue({
        code: 200,
        message: 'success',
        data: mockLoginResponse,
      })
      vi.mocked(userApi.getMe).mockResolvedValue({
        code: 200,
        message: 'success',
        data: mockUserDetail,
      })

      const store = useUserStore()
      await store.login('testuser', 'password123')

      expect(authApi.login).toHaveBeenCalledWith({
        username: 'testuser',
        password: 'password123',
      })
      expect(store.token).toBe('jwt-token-abc123')
      expect(store.isLoggedIn).toBe(true)
      expect(userApi.getMe).toHaveBeenCalled()
      expect(store.userInfo).toEqual(mockUserDetail)
    })

    it('persists token to localStorage', async () => {
      vi.mocked(authApi.login).mockResolvedValue({
        code: 200,
        message: 'success',
        data: mockLoginResponse,
      })
      vi.mocked(userApi.getMe).mockResolvedValue({
        code: 200,
        message: 'success',
        data: mockUserDetail,
      })

      const store = useUserStore()
      await store.login('testuser', 'password123')

      expect(localStorage.setItem).toHaveBeenCalledWith(TOKEN_KEY, 'jwt-token-abc123')
    })
  })

  describe('register', () => {
    it('calls authApi.register with username and password', async () => {
      vi.mocked(authApi.register).mockResolvedValue({
        code: 200,
        message: 'success',
        data: null,
      })

      const store = useUserStore()
      await store.register('newuser', 'secret')

      expect(authApi.register).toHaveBeenCalledWith({
        username: 'newuser',
        password: 'secret',
      })
    })

    it('passes email when provided', async () => {
      vi.mocked(authApi.register).mockResolvedValue({
        code: 200,
        message: 'success',
        data: null,
      })

      const store = useUserStore()
      await store.register('newuser', 'secret', 'newuser@example.com')

      expect(authApi.register).toHaveBeenCalledWith({
        username: 'newuser',
        password: 'secret',
        email: 'newuser@example.com',
      })
    })
  })

  describe('fetchUserInfo', () => {
    it('updates userInfo on success', async () => {
      vi.mocked(userApi.getMe).mockResolvedValue({
        code: 200,
        message: 'success',
        data: mockAdminDetail,
      })

      // Set token first so we're "logged in"
      const store = useUserStore()
      store.token = 'existing-token'
      await store.fetchUserInfo()

      expect(store.userInfo).toEqual(mockAdminDetail)
    })

    it('does nothing when not logged in', async () => {
      const store = useUserStore()
      await store.fetchUserInfo()

      expect(userApi.getMe).not.toHaveBeenCalled()
    })

    it('logs out when getMe fails', async () => {
      vi.mocked(userApi.getMe).mockRejectedValue(new Error('401 Unauthorized'))

      const store = useUserStore()
      store.token = 'expired-token'
      await store.fetchUserInfo()

      expect(store.token).toBeNull()
      expect(store.userInfo).toBeNull()
      expect(store.isLoggedIn).toBe(false)
    })
  })

  describe('logout', () => {
    it('clears token, userInfo, and localStorage', async () => {
      vi.mocked(authApi.login).mockResolvedValue({
        code: 200,
        message: 'success',
        data: mockLoginResponse,
      })
      vi.mocked(userApi.getMe).mockResolvedValue({
        code: 200,
        message: 'success',
        data: mockUserDetail,
      })

      const store = useUserStore()
      await store.login('testuser', 'password123')
      expect(store.isLoggedIn).toBe(true)

      store.logout()

      expect(store.token).toBeNull()
      expect(store.userInfo).toBeNull()
      expect(store.isLoggedIn).toBe(false)
      expect(localStorage.removeItem).toHaveBeenCalledWith(TOKEN_KEY)
    })
  })

  describe('computed getters', () => {
    it('isLoggedIn is computed from token', () => {
      const store = useUserStore()
      expect(store.isLoggedIn).toBe(false)

      store.token = 'some-token'
      expect(store.isLoggedIn).toBe(true)

      store.token = null
      expect(store.isLoggedIn).toBe(false)
    })

    it('role is computed from userInfo', () => {
      const store = useUserStore()
      expect(store.role).toBe('')

      store.userInfo = mockUserProfile
      expect(store.role).toBe('USER')
    })

    it('isAdmin returns true only for ADMIN role', () => {
      const store = useUserStore()
      expect(store.isAdmin).toBe(false)

      store.userInfo = mockUserProfile
      expect(store.isAdmin).toBe(false)

      store.userInfo = mockAdminProfile
      expect(store.isAdmin).toBe(true)
    })

    it('isModerator returns true for ADMIN and MODERATOR roles', () => {
      const store = useUserStore()
      expect(store.isModerator).toBe(false)

      store.userInfo = mockUserProfile
      expect(store.isModerator).toBe(false)

      store.userInfo = mockModeratorProfile
      expect(store.isModerator).toBe(true)

      store.userInfo = mockAdminProfile
      expect(store.isModerator).toBe(true)
    })
  })
})
