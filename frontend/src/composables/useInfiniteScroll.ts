import { ref, onMounted, onUnmounted } from 'vue'

export interface InfiniteScrollOptions {
  /** IntersectionObserver threshold (0-1) */
  threshold?: number
  /** CSS margin string for rootMargin */
  rootMargin?: string
}

/**
 * Composable for infinite scrolling using IntersectionObserver.
 * Observes a sentinel element at the bottom of a list.
 * When visible and hasMore + not loading → calls loadFn.
 */
export function useInfiniteScroll(
  loadFn: () => Promise<void>,
  options: InfiniteScrollOptions = {},
) {
  const sentinelRef = ref<HTMLElement | null>(null)
  const loading = ref(false)
  const hasMore = ref(true)
  const error = ref<Error | null>(null)

  let observer: IntersectionObserver | null = null

  /** Trigger loadFn if conditions allow */
  async function loadMore(): Promise<void> {
    if (loading.value || !hasMore.value) return
    loading.value = true
    error.value = null
    try {
      await loadFn()
    } catch (e) {
      error.value = e instanceof Error ? e : new Error(String(e))
      // Don't set hasMore to false on error — allow retry
    } finally {
      loading.value = false
    }
  }

  /** Reset state (e.g. when changing filters) */
  function reset(): void {
    hasMore.value = true
    error.value = null
    loading.value = false
  }

  onMounted(() => {
    if (!sentinelRef.value) return

    observer = new IntersectionObserver(
      (entries) => {
        if (entries[0]?.isIntersecting && hasMore.value && !loading.value) {
          loadMore()
        }
      },
      {
        threshold: options.threshold ?? 0.1,
        rootMargin: options.rootMargin ?? '100px',
      },
    )

    observer.observe(sentinelRef.value)
  })

  onUnmounted(() => {
    observer?.disconnect()
    observer = null
  })

  return { sentinelRef, loading, hasMore, error, loadMore, reset }
}
