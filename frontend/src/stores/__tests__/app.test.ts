import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'

const DARK_MODE_KEY = 'techhub-dark-mode'

describe('useAppStore', () => {
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
      key: vi.fn(),
      length: 0,
    })
    // Mock document.documentElement
    const classList = new Set<string>()
    classList.add('light') // default light mode
    vi.stubGlobal('document', {
      documentElement: {
        classList: {
          add: vi.fn((cls: string) => classList.add(cls)),
          remove: vi.fn((cls: string) => classList.delete(cls)),
          contains: vi.fn((cls: string) => classList.has(cls)),
          toggle: vi.fn(),
          replace: vi.fn(),
        },
      },
    })
    vi.clearAllMocks()
  })

  // Dynamic import so the module sees our mocked globals
  async function importStore() {
    const mod = await import('@/stores/app')
    return mod.useAppStore
  }

  describe('initial state', () => {
    it('sidebar is not collapsed by default', async () => {
      const useAppStore = await importStore()
      const store = useAppStore()
      expect(store.sidebarCollapsed).toBe(false)
    })

    it('is in dark mode when localStorage does not have "light"', async () => {
      const useAppStore = await importStore()
      const store = useAppStore()
      // localStorage is empty, so getItem returns null, which !== 'light'
      expect(store.isDarkMode).toBe(true)
    })

    it('is in light mode when localStorage has "light"', async () => {
      localStorage.setItem(DARK_MODE_KEY, 'light')
      const useAppStore = await importStore()
      const store = useAppStore()
      expect(store.isDarkMode).toBe(false)
    })
  })

  describe('toggleSidebar', () => {
    it('toggles sidebarCollapsed state', async () => {
      const useAppStore = await importStore()
      const store = useAppStore()
      expect(store.sidebarCollapsed).toBe(false)

      store.toggleSidebar()
      expect(store.sidebarCollapsed).toBe(true)

      store.toggleSidebar()
      expect(store.sidebarCollapsed).toBe(false)
    })
  })

  describe('toggleDarkMode', () => {
    it('toggles isDarkMode', async () => {
      // Start in light mode
      localStorage.setItem(DARK_MODE_KEY, 'light')
      const useAppStore = await importStore()
      const store = useAppStore()
      expect(store.isDarkMode).toBe(false)

      store.toggleDarkMode()
      expect(store.isDarkMode).toBe(true)

      store.toggleDarkMode()
      expect(store.isDarkMode).toBe(false)
    })

    it('adds dark class and removes light class when switching to dark', async () => {
      localStorage.setItem(DARK_MODE_KEY, 'light')
      const useAppStore = await importStore()
      const store = useAppStore()

      store.toggleDarkMode()

      expect(document.documentElement.classList.add).toHaveBeenCalledWith('dark')
      expect(document.documentElement.classList.remove).toHaveBeenCalledWith('light')
    })

    it('adds light class and removes dark class when switching to light', async () => {
      // Start in dark mode (default when key is absent)
      const useAppStore = await importStore()
      const store = useAppStore()

      store.toggleDarkMode()

      expect(document.documentElement.classList.add).toHaveBeenCalledWith('light')
      expect(document.documentElement.classList.remove).toHaveBeenCalledWith('dark')
    })

    it('persists to localStorage when switching to dark', async () => {
      localStorage.setItem(DARK_MODE_KEY, 'light')
      const useAppStore = await importStore()
      const store = useAppStore()

      store.toggleDarkMode()

      expect(localStorage.setItem).toHaveBeenCalledWith(DARK_MODE_KEY, 'dark')
    })

    it('persists to localStorage when switching to light', async () => {
      const useAppStore = await importStore()
      const store = useAppStore()

      store.toggleDarkMode()

      expect(localStorage.setItem).toHaveBeenCalledWith(DARK_MODE_KEY, 'light')
    })
  })
})
