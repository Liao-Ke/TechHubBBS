import type { UserProfileVO } from './user'

/**
 * Post visibility: 0=public, 1=login-only, 2=followers-only, 3=private
 */
export type PostVisibility = 0 | 1 | 2 | 3

/**
 * Post type: 0=normal, 1=featured, 2=pinned
 */
export type PostType = 0 | 1 | 2

/**
 * Post status: 0=locked, 1=active
 */
export type PostStatus = 0 | 1

/**
 * Full post view object
 */
export interface PostVO {
  id: string
  title: string
  content: string
  summary?: string
  author: UserProfileVO
  categoryId: number
  categoryName: string
  visibility: PostVisibility
  type: PostType
  status: PostStatus
  viewCount: number
  likeCount: number
  commentCount: number
  favoriteCount: number
  divineCommentCount: number
  isLiked: boolean
  isFavorited: boolean
  createTime: string
  updateTime: string
}

/**
 * Post list query parameters
 */
export interface PostListParams {
  page?: number
  size?: number
  keyword?: string
  categoryId?: number
  sort?: string
  userId?: string
}

/**
 * Post create request body
 */
export interface PostCreateRequest {
  title: string
  content: string
  categoryId: number
  visibility: PostVisibility
  draftPostId?: string
}

/**
 * Post update request body (partial)
 */
export interface PostUpdateRequest {
  title?: string
  content?: string
  categoryId?: number
  visibility?: PostVisibility
}

/**
 * Interaction state (likes / favorites)
 */
export interface PostInteractionStatus {
  liked: boolean
  favorited: boolean
}
