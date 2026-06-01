import { computed } from 'vue'
import { useUserStore } from '@/stores/user'

/**
 * Composable for permission checks based on user role.
 * Provides reactive booleans and a role-matching function.
 */
export function usePermission() {
  const userStore = useUserStore()

  /** True if the user has ADMIN role */
  const isAdmin = computed(() => userStore.role === 'ADMIN')

  /**
   * True if user has MODERATOR or ADMIN role.
   * ADMIN implicitly inherits all MODERATOR permissions.
   */
  const isModerator = computed(() =>
    userStore.role === 'MODERATOR' || userStore.role === 'ADMIN',
  )

  /**
   * Check if the current user's role satisfies the required role(s).
   * @param required - Single role string or array of roles.
   *   ADMIN passes any check that includes MODERATOR (inheritance).
   */
  function canAccess(required: string | string[]): boolean {
    const roles = Array.isArray(required) ? required : [required]
    const userRole = userStore.role || 'USER'
    return roles.some((role) => {
      // ADMIN inherits MODERATOR access
      if (userRole === 'ADMIN' && role === 'MODERATOR') return true
      // GUEST and any role can pass USER-level checks (baseline access)
      if (role === 'USER') return true
      return userRole === role
    })
  }

  return { isAdmin, isModerator, canAccess }
}
