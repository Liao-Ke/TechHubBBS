import { describe, it, expect, vi, beforeEach } from 'vitest'
import { useInfiniteScroll } from '@/composables/useInfiniteScroll'

describe('useInfiniteScroll', () => {
  beforeEach(() => {
    vi.stubGlobal('IntersectionObserver', vi.fn(() => ({
      observe: vi.fn(),
      unobserve: vi.fn(),
      disconnect: vi.fn(),
    })))
  })

  describe('state management', () => {
    it('initializes with correct default state', () => {
      const loadFn = vi.fn()
      const { loading, hasMore, error, sentinelRef } = useInfiniteScroll(loadFn)

      expect(sentinelRef.value).toBeNull()
      expect(loading.value).toBe(false)
      expect(hasMore.value).toBe(true)
      expect(error.value).toBeNull()
    })
  })

  describe('loadMore', () => {
    it('calls loadFn when hasMore is true and not loading', async () => {
      const loadFn = vi.fn().mockResolvedValue(undefined)
      const { loadMore, loading } = useInfiniteScroll(loadFn)

      await loadMore()
      expect(loadFn).toHaveBeenCalledTimes(1)
      expect(loading.value).toBe(false)
    })

    it('does NOT call loadFn when loading is already true', async () => {
      const loadFn = vi.fn(() => new Promise<void>(() => {}))
      const { loadMore, loading } = useInfiniteScroll(loadFn)

      loadMore()
      await loadMore()

      expect(loadFn).toHaveBeenCalledTimes(1)
      expect(loading.value).toBe(true)
    })

    it('does NOT call loadFn when hasMore is false', async () => {
      const loadFn = vi.fn()
      const { loadMore, hasMore } = useInfiniteScroll(loadFn)

      hasMore.value = false
      await loadMore()
      expect(loadFn).not.toHaveBeenCalled()
    })

    it('sets error when loadFn throws', async () => {
      const testError = new Error('Load failed')
      const loadFn = vi.fn().mockRejectedValue(testError)
      const { loadMore, error, hasMore, loading } = useInfiniteScroll(loadFn)

      await loadMore()
      expect(error.value).toBe(testError)
      expect(hasMore.value).toBe(true)
      expect(loading.value).toBe(false)
    })

    it('handles non-Error thrown values', async () => {
      const loadFn = vi.fn().mockRejectedValue('Some string error')
      const { loadMore, error } = useInfiniteScroll(loadFn)

      await loadMore()
      expect(error.value).toBeInstanceOf(Error)
      expect(error.value!.message).toBe('Some string error')
    })

    it('clears previous error on subsequent successful load', async () => {
      const loadFn = vi.fn()
        .mockRejectedValueOnce(new Error('First fail'))
        .mockResolvedValueOnce(undefined)
      const { loadMore, error } = useInfiniteScroll(loadFn)

      await loadMore()
      expect(error.value).not.toBeNull()

      await loadMore()
      expect(error.value).toBeNull()
    })
  })

  describe('reset', () => {
    it('restores default state', () => {
      const loadFn = vi.fn()
      const { hasMore, error, loading, reset } = useInfiniteScroll(loadFn)

      hasMore.value = false
      error.value = new Error('Test')
      loading.value = true

      reset()

      expect(hasMore.value).toBe(true)
      expect(error.value).toBeNull()
      expect(loading.value).toBe(false)
    })
  })

  describe('options defaults', () => {
    it('uses default threshold and rootMargin when not specified', () => {
      const loadFn = vi.fn()
      const { loading, hasMore } = useInfiniteScroll(loadFn)
      expect(loading.value).toBe(false)
      expect(hasMore.value).toBe(true)
    })

    it('accepts custom options', () => {
      const loadFn = vi.fn()
      const { loading } = useInfiniteScroll(loadFn, {
        threshold: 0.5,
        rootMargin: '200px',
      })
      expect(loading.value).toBe(false)
    })
  })
})
