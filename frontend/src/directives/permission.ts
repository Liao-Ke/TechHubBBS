import type { Directive, DirectiveBinding } from 'vue'

export interface PermissionHTMLElement extends HTMLElement {
  __permissionHandler?: () => void
}

/**
 * Role level mapping for hierarchy-based permission checks.
 *
 * Higher level = more privileges.
 * Higher roles inherit lower roles' permissions.
 */
const ROLE_LEVEL: Record<string, number> = {
  USER: 0,
  MODERATOR: 1,
  ADMIN: 2,
}

/**
 * Check whether the current user's role satisfies any of the required roles.
 *
 * @param requiredRoles - Role(s) required to access the element
 * @param currentRole   - The current user's role string (e.g. 'USER', 'ADMIN')
 * @returns true if the user has sufficient permissions
 */
function hasPermission(requiredRoles: string[], currentRole: string | undefined): boolean {
  if (!currentRole) return false
  const currentLevel = ROLE_LEVEL[currentRole] ?? -1
  return requiredRoles.some((role) => {
    const requiredLevel = ROLE_LEVEL[role] ?? 99
    return currentLevel >= requiredLevel
  })
}

/**
 * Evaluate the current permission state and update the element accordingly.
 *
 * If the user has permission → show the element normally.
 * If the user lacks permission → remove the element from the DOM entirely.
 * If the Pinia store is not yet available → hide the element.
 */
async function evaluatePermission(
  el: PermissionHTMLElement,
  binding: DirectiveBinding<string | string[]>,
): Promise<void> {
  const requiredRoles = Array.isArray(binding.value) ? binding.value : [binding.value]

  try {
    const { useUserStore } = await import('@/stores/user')
    const store = useUserStore()

    if (hasPermission(requiredRoles, store.role)) {
      // Has permission — show the element
      el.style.display = ''
      el.style.visibility = ''
      return
    }
  } catch {
    // Store not available (e.g. outside Vue app context) — hide element
  }

  // No permission or store unavailable — remove from DOM
  el.style.display = 'none'
  el.style.visibility = 'hidden'
  if (el.parentNode) {
    el.parentNode.removeChild(el)
  }
}

/**
 * `v-permission` — Role-based element visibility directive.
 *
 * Usage:
 *   v-permission="'ADMIN'"              →  only admin
 *   v-permission="['ADMIN','MODERATOR']" →  admin or moderator
 *   v-permission="'USER'"               →  any authenticated user
 *
 * Role hierarchy (higher inherits lower):
 *   USER(0) → MODERATOR(1) → ADMIN(2)
 *
 * An ADMIN can see elements requiring MODERATOR or USER.
 */
export const vPermission: Directive<PermissionHTMLElement> = {
  async mounted(el, binding) {
    await evaluatePermission(el, binding)
  },
  async updated(el, binding) {
    await evaluatePermission(el, binding)
  },
  unmounted(el) {
    el.__permissionHandler = undefined
  },
}

export default vPermission
