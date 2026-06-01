import { describe, it, expect } from 'vitest'
import { useVisibility, VisibilityLevel } from '../useVisibility'

describe('useVisibility', () => {
  const { visibilityLabel, visibilityIcon, canView } = useVisibility()

  // ── VisibilityLevel constants ──
  describe('VisibilityLevel', () => {
    it('PUBLIC equals 0', () => {
      expect(VisibilityLevel.PUBLIC).toBe(0)
    })
    it('LOGIN equals 1', () => {
      expect(VisibilityLevel.LOGIN).toBe(1)
    })
    it('FOLLOWERS equals 2', () => {
      expect(VisibilityLevel.FOLLOWERS).toBe(2)
    })
    it('PRIVATE equals 3', () => {
      expect(VisibilityLevel.PRIVATE).toBe(3)
    })
  })

  // ── visibilityLabel ──
  describe('visibilityLabel', () => {
    it('returns 公开 for PUBLIC (0)', () => {
      expect(visibilityLabel(0)).toBe('公开')
    })
    it('returns 登录可见 for LOGIN (1)', () => {
      expect(visibilityLabel(1)).toBe('登录可见')
    })
    it('returns 粉丝可见 for FOLLOWERS (2)', () => {
      expect(visibilityLabel(2)).toBe('粉丝可见')
    })
    it('returns 私密 for PRIVATE (3)', () => {
      expect(visibilityLabel(3)).toBe('私密')
    })
    it('returns 未知 for unknown codes', () => {
      expect(visibilityLabel(-1)).toBe('未知')
      expect(visibilityLabel(99)).toBe('未知')
    })
  })

  // ── visibilityIcon ──
  describe('visibilityIcon', () => {
    it('returns View for PUBLIC (0)', () => {
      expect(visibilityIcon(0)).toBe('View')
    })
    it('returns Lock for LOGIN (1)', () => {
      expect(visibilityIcon(1)).toBe('Lock')
    })
    it('returns User for FOLLOWERS (2)', () => {
      expect(visibilityIcon(2)).toBe('User')
    })
    it('returns Hide for PRIVATE (3)', () => {
      expect(visibilityIcon(3)).toBe('Hide')
    })
    it('returns QuestionFilled for unknown codes', () => {
      expect(visibilityIcon(99)).toBe('QuestionFilled')
    })
  })

  // ── canView ──
  describe('canView', () => {
    // --- PUBLIC (0) ---
    describe('PUBLIC visibility', () => {
      it('guest can view', () => {
        expect(canView(0, false, false, false)).toBe(true)
      })
      it('logged-in user can view', () => {
        expect(canView(0, true, false, false)).toBe(true)
      })
      it('follower can view', () => {
        expect(canView(0, true, true, false)).toBe(true)
      })
      it('author can view', () => {
        expect(canView(0, true, false, true)).toBe(true)
      })
      it('admin can view', () => {
        expect(canView(0, false, false, false, true)).toBe(true)
      })
    })

    // --- LOGIN (1) ---
    describe('LOGIN visibility', () => {
      it('guest cannot view', () => {
        expect(canView(1, false, false, false)).toBe(false)
      })
      it('logged-in user can view', () => {
        expect(canView(1, true, false, false)).toBe(true)
      })
      it('logged-in follower can view', () => {
        expect(canView(1, true, true, false)).toBe(true)
      })
      it('author can always view', () => {
        expect(canView(1, false, false, true)).toBe(true)
      })
    })

    // --- FOLLOWERS (2) ---
    describe('FOLLOWERS visibility', () => {
      it('guest cannot view', () => {
        expect(canView(2, false, false, false)).toBe(false)
      })
      it('logged-in non-follower cannot view', () => {
        expect(canView(2, true, false, false)).toBe(false)
      })
      it('logged-in follower can view', () => {
        expect(canView(2, true, true, false)).toBe(true)
      })
      it('author can always view (even if not following themselves)', () => {
        expect(canView(2, false, false, true)).toBe(true)
      })
    })

    // --- PRIVATE (3) ---
    describe('PRIVATE visibility', () => {
      it('guest cannot view', () => {
        expect(canView(3, false, false, false)).toBe(false)
      })
      it('logged-in user (not author) cannot view', () => {
        expect(canView(3, true, false, false)).toBe(false)
      })
      it('follower (not author) cannot view', () => {
        expect(canView(3, true, true, false)).toBe(false)
      })
      it('author can view own private post', () => {
        expect(canView(3, false, false, true)).toBe(true)
      })
      it('admin can view private post', () => {
        expect(canView(3, false, false, false, true)).toBe(true)
      })
      it('exact match: canView(3, false, false, true) == true', () => {
        // Requirement: author can always see own private posts
        expect(canView(3, false, false, true)).toBe(true)
      })
    })

    // --- Edge cases ---
    describe('edge cases', () => {
      it('admin can see everything regardless of visibility', () => {
        expect(canView(0, false, false, false, true)).toBe(true)
        expect(canView(1, false, false, false, true)).toBe(true)
        expect(canView(2, false, false, false, true)).toBe(true)
        expect(canView(3, false, false, false, true)).toBe(true)
      })
      it('author can see own posts of any visibility', () => {
        expect(canView(0, false, false, true)).toBe(true)
        expect(canView(1, false, false, true)).toBe(true)
        expect(canView(2, false, false, true)).toBe(true)
        expect(canView(3, false, false, true)).toBe(true)
      })
      it('unknown visibility code returns false for non-author/non-admin', () => {
        expect(canView(99, true, true, false)).toBe(false)
      })
      it('unknown visibility code returns true for author', () => {
        expect(canView(99, false, false, true)).toBe(true)
      })
      it('admin has priority over all checks', () => {
        expect(canView(3, false, false, false, true)).toBe(true)
      })
      it('author takes priority over visibility level', () => {
        // Private post, not logged in, not following → still true because isAuthor
        expect(canView(3, false, false, true)).toBe(true)
      })
    })
  })
})
