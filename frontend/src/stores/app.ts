import { defineStore } from 'pinia'
import { ref } from 'vue'

const DARK_MODE_KEY = 'techhub-dark-mode'

function resolveInitialDarkMode(): boolean {
  const stored = localStorage.getItem(DARK_MODE_KEY)
  if (stored === 'dark') return true
  if (stored === 'light') return false
  if (typeof window !== 'undefined' && window.matchMedia) {
    return window.matchMedia('(prefers-color-scheme: dark)').matches
  }
  return true
}

export const useAppStore = defineStore('app', () => {
  const sidebarCollapsed = ref(false)
  const isDarkMode = ref(resolveInitialDarkMode())

  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  function toggleDarkMode() {
    isDarkMode.value = !isDarkMode.value
    if (isDarkMode.value) {
      document.documentElement.classList.add('dark')
      document.documentElement.classList.remove('light')
      localStorage.setItem(DARK_MODE_KEY, 'dark')
    } else {
      document.documentElement.classList.add('light')
      document.documentElement.classList.remove('dark')
      localStorage.setItem(DARK_MODE_KEY, 'light')
    }
  }

  return { sidebarCollapsed, isDarkMode, toggleSidebar, toggleDarkMode }
})
