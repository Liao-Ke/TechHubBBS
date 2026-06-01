import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import { setupRouterGuards } from './router/guards'
import { vPermission } from './directives/permission'

// === Styles (order matters) ===
import 'element-plus/dist/index.css'
import './assets/styles/element-theme.scss'
import './assets/styles/variables.scss'
import './assets/styles/global.scss'
import './assets/styles/markdown.scss'

// === Font ===
import '@fontsource/inter/400.css'
import '@fontsource/inter/500.css'
import '@fontsource/inter/600.css'
import '@fontsource/inter/700.css'

// === Code Highlighting Theme ===
import 'highlight.js/styles/github-dark.css'

// === Dark mode init ===
const storedTheme = localStorage.getItem('techhub-dark-mode')
if (storedTheme === 'light') {
  document.documentElement.classList.add('light')
  document.documentElement.classList.remove('dark')
} else if (storedTheme === 'dark') {
  document.documentElement.classList.add('dark')
  document.documentElement.classList.remove('light')
} else {
  // No localStorage preference – check system preference
  if (window.matchMedia('(prefers-color-scheme: dark)').matches) {
    document.documentElement.classList.add('dark')
    document.documentElement.classList.remove('light')
  } else {
    document.documentElement.classList.add('light')
    document.documentElement.classList.remove('dark')
  }
}

// Listen for system preference changes (only when no explicit preference set)
const systemDarkQuery = window.matchMedia('(prefers-color-scheme: dark)')
systemDarkQuery.addEventListener('change', (e) => {
  if (localStorage.getItem('techhub-dark-mode') === null) {
    if (e.matches) {
      document.documentElement.classList.add('dark')
      document.documentElement.classList.remove('light')
    } else {
      document.documentElement.classList.add('light')
      document.documentElement.classList.remove('dark')
    }
  }
})

const app = createApp(App)

// Plugins
app.use(createPinia())
app.use(router)

// Directives
app.directive('permission', vPermission)

// Router guards
setupRouterGuards(router)

app.mount('#app')
