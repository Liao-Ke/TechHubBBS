import { describe, it, expect, vi, beforeEach } from 'vitest'
import { usePermission } from '../usePermission'

// Mock useUserStore before importing the composable
const mockUseUserStore = vi.fn()

vi.mock('@/stores/user', () => ({
  useUserStore: () => mockUseUserStore(),
}))

describe('usePermission', () => {
  function createMockStore(role: string) {
    return {
      isLoggedIn: role !== 'GUEST',
      userInfo: role === 'GUEST' ? null : { id: '1', username: 'test', role },
      role,
      isAdmin: role === 'ADMIN',
      isModerator: role === 'MODERATOR' || role === 'ADMIN',
    }
  }

  beforeEach(() => {
    mockUseUserStore.mockReturnValue(createMockStore('USER'))
  })

  // ── isAdmin ──
  describe('isAdmin', () => {
    it('is true for ADMIN role', () => {
      mockUseUserStore.mockReturnValue(createMockStore('ADMIN'))
      const { isAdmin } = usePermission()
      expect(isAdmin.value).toBe(true)
    })

    it('is false for MODERATOR role', () => {
      mockUseUserStore.mockReturnValue(createMockStore('MODERATOR'))
      const { isAdmin } = usePermission()
      expect(isAdmin.value).toBe(false)
    })

    it('is false for USER role', () => {
      mockUseUserStore.mockReturnValue(createMockStore('USER'))
      const { isAdmin } = usePermission()
      expect(isAdmin.value).toBe(false)
    })

    it('is false for GUEST (no role)', () => {
      mockUseUserStore.mockReturnValue(createMockStore('GUEST'))
      const { isAdmin } = usePermission()
      expect(isAdmin.value).toBe(false)
    })
  })

  // ── isModerator ──
  describe('isModerator', () => {
    it('is true for ADMIN role (inherits moderator)', () => {
      mockUseUserStore.mockReturnValue(createMockStore('ADMIN'))
      const { isModerator } = usePermission()
      expect(isModerator.value).toBe(true)
    })

    it('is true for MODERATOR role', () => {
      mockUseUserStore.mockReturnValue(createMockStore('MODERATOR'))
      const { isModerator } = usePermission()
      expect(isModerator.value).toBe(true)
    })

    it('is false for USER role', () => {
      mockUseUserStore.mockReturnValue(createMockStore('USER'))
      const { isModerator } = usePermission()
      expect(isModerator.value).toBe(false)
    })

    it('is false for GUEST', () => {
      mockUseUserStore.mockReturnValue(createMockStore('GUEST'))
      const { isModerator } = usePermission()
      expect(isModerator.value).toBe(false)
    })
  })

  // ── canAccess ──
  describe('canAccess', () => {
    it('returns true when user role matches required single role', () => {
      const { canAccess } = usePermission()
      expect(canAccess('USER')).toBe(true)
    })

    it('returns false when user role does not match required single role', () => {
      const { canAccess } = usePermission()
      expect(canAccess('ADMIN')).toBe(false)
    })

    it('returns true when user role is in required role array', () => {
      const { canAccess } = usePermission()
      expect(canAccess(['USER', 'MODERATOR'])).toBe(true)
    })

    it('returns false when user role is not in required role array', () => {
      const { canAccess } = usePermission()
      expect(canAccess(['ADMIN', 'MODERATOR'])).toBe(false)
    })

    it('admin passes MODERATOR check (admin inherits moderator)', () => {
      mockUseUserStore.mockReturnValue(createMockStore('ADMIN'))
      const { canAccess } = usePermission()
      expect(canAccess('MODERATOR')).toBe(true)
    })

    it('MODERATOR does not pass ADMIN check', () => {
      mockUseUserStore.mockReturnValue(createMockStore('MODERATOR'))
      const { canAccess } = usePermission()
      expect(canAccess('ADMIN')).toBe(false)
    })

    it('admin passes ADMIN check', () => {
      mockUseUserStore.mockReturnValue(createMockStore('ADMIN'))
      const { canAccess } = usePermission()
      expect(canAccess('ADMIN')).toBe(true)
    })

    it('admin passes both ADMIN and MODERATOR in array', () => {
      mockUseUserStore.mockReturnValue(createMockStore('ADMIN'))
      const { canAccess } = usePermission()
      expect(canAccess(['ADMIN', 'MODERATOR'])).toBe(true)
    })

    it('GUEST role passes no permission checks except USER', () => {
      mockUseUserStore.mockReturnValue(createMockStore('GUEST'))
      const { canAccess } = usePermission()
      expect(canAccess('USER')).toBe(true)
      expect(canAccess('MODERATOR')).toBe(false)
      expect(canAccess('ADMIN')).toBe(false)
    })
  })
})
