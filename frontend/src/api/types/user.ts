/**
 * User role values as returned by the API
 */
export type UserRole = 'user' | 'moderator' | 'admin'

/**
 * Login request body
 */
export interface LoginRequest {
  username: string
  password: string
}

/**
 * Login response data
 */
export interface LoginResponse {
  token: string
  tokenType: string
  userId: string
  username: string
  role: string
}

/**
 * Register request body
 */
export interface RegisterRequest {
  username: string
  password: string
  email?: string
}

/**
 * Public user profile (returned by GET /users/{id})
 */
export interface UserProfileVO {
  id: string
  username: string
  avatarUrl?: string
  bio?: string
  role: string
  status: number
  createTime: string
  postCount?: number
  followerCount?: number
  followingCount?: number
}

/**
 * Detailed user profile (returned by GET /users/me, includes email)
 */
export interface UserDetailVO extends UserProfileVO {
  email: string
  updateTime: string
}

/**
 * Update profile request body
 */
export interface UserUpdateRequest {
  avatarUrl?: string
  bio?: string
}

/**
 * Follow status between current user and target user
 */
export interface FollowStatusVO {
  following: boolean
}
