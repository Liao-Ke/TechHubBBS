import type { Router } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { usePermission } from '@/composables/usePermission'

export function setupRouterGuards(router: Router) {
  router.beforeEach(async (to, _from, next) => {
    // Set document title
    document.title = to.meta.title ? `${to.meta.title} - TechHub` : 'TechHub'

    const userStore = useUserStore()

    // Redirect logged-in users away from auth pages
    if ((to.path === '/login' || to.path === '/register') && userStore.isLoggedIn) {
      return next({ path: '/' })
    }

    // Fetch user info if logged in but info not loaded (page refresh scenario)
    // Must run before public route early return to restore userInfo on refresh
    if (userStore.isLoggedIn && !userStore.userInfo) {
      await userStore.fetchUserInfo()
      // If fetchUserInfo fails, it calls logout() which clears token
      // Re-check authentication after fetch
      if (!userStore.isLoggedIn) {
        return next({ path: '/login', query: { redirect: to.fullPath } })
      }
    }

    // Public routes — always allow
    if (!to.meta.requiresAuth && !to.meta.roles) {
      return next()
    }

    // Check authentication
    if (to.meta.requiresAuth && !userStore.isLoggedIn) {
      return next({ path: '/login', query: { redirect: to.fullPath } })
    }

    // Check role requirements
    const requiredRoles = to.meta.roles
    if (requiredRoles && requiredRoles.length > 0) {
      const { isAdmin, isModerator } = usePermission()
      const hasRole = requiredRoles.some((r) => {
        if (r === 'ADMIN') return isAdmin.value
        if (r === 'MODERATOR') return isModerator.value
        return userStore.role === r
      })
      if (!hasRole) {
        return next({ path: '/403' })
      }
    }

    next()
  })
}
