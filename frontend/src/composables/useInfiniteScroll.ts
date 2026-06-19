import { ref, watch, onUnmounted } from 'vue'

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
 *
 * Uses `watch` on sentinelRef (flush: 'sync') instead of `onMounted`,
 * so the observer is created reactively when the sentinel element
 * finally appears in the DOM (e.g. after v-if/v-else conditions resolve).
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

  // Reactively create/destroy IntersectionObserver when sentinelRef
  // appears/disappears from the DOM (fixes issue where sentinel is
  // inside v-if/v-else and doesn't exist at onMounted time).
  const stopWatch = watch(
    () => sentinelRef.value,
    (el) => {
      // Disconnect previous observer
      observer?.disconnect()
      observer = null

      if (!el) return

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

      observer.observe(el)
    },
    { flush: 'sync' },
  )

  onUnmounted(() => {
    stopWatch()
    observer?.disconnect()
    observer = null
  })

  return { sentinelRef, loading, hasMore, error, loadMore, reset }
}
