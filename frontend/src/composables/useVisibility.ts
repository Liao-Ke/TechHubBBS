/**
 * Visibility level constants matching backend VisibilityEnum.
 */
export const VisibilityLevel = {
  PUBLIC: 0,
  LOGIN: 1,
  FOLLOWERS: 2,
  PRIVATE: 3,
} as const

export type VisibilityLevel = (typeof VisibilityLevel)[keyof typeof VisibilityLevel]

/**
 * Return a human-readable label for a visibility level.
 */
export function visibilityLabel(code: number): string {
  switch (code) {
    case VisibilityLevel.PUBLIC:    return '公开'
    case VisibilityLevel.LOGIN:     return '登录可见'
    case VisibilityLevel.FOLLOWERS: return '粉丝可见'
    case VisibilityLevel.PRIVATE:   return '私密'
    default:                        return '未知'
  }
}

/**
 * Return an Element Plus icon name for a visibility level.
 */
export function visibilityIcon(code: number): string {
  switch (code) {
    case VisibilityLevel.PUBLIC:    return 'View'
    case VisibilityLevel.LOGIN:     return 'Lock'
    case VisibilityLevel.FOLLOWERS: return 'User'
    case VisibilityLevel.PRIVATE:   return 'Hide'
    default:                        return 'QuestionFilled'
  }
}

/**
 * Determine whether the current viewer can see a post.
 *
 * @param visibility  - Post visibility code (0-3)
 * @param isLoggedIn  - Viewer is authenticated
 * @param isFollowing - Viewer follows the author
 * @param isAuthor    - Viewer is the post author
 * @param isAdmin     - Viewer is an admin (overrides all)
 */
export function canView(
  visibility: number,
  isLoggedIn: boolean,
  isFollowing: boolean,
  isAuthor: boolean,
  isAdmin = false,
): boolean {
  // Admin can see everything
  if (isAdmin) return true

  // Author can always see own posts
  if (isAuthor) return true

  switch (visibility) {
    case VisibilityLevel.PUBLIC:
      return true
    case VisibilityLevel.LOGIN:
      return isLoggedIn
    case VisibilityLevel.FOLLOWERS:
      return isLoggedIn && isFollowing
    case VisibilityLevel.PRIVATE:
      return false
    default:
      return false
  }
}

/**
 * Composable that re-exports visibility helpers.
 * Used by views/components that need them.
 */
export function useVisibility() {
  return { visibilityLabel, visibilityIcon, canView }
}
