/**
 * Comment item shape used in admin comment listing.
 * Backend returns flat username/avatarUrl/postTitle/divineTime fields.
 */
export interface AdminCommentItem {
  id: string
  postId: string
  content: string
  username: string
  avatarUrl: string
  userId: string
  postTitle: string
  likeCount: number
  recommendCount: number
  isDivine: boolean
  divineTime: string | null
  createTime: string
}

/**
 * Full comment view object (supports nested children)
 */
export interface CommentVO {
  id: string
  postId: string
  content: string
  userId: string
  username: string
  avatarUrl?: string
  parentId: number | null
  replyToUserId: number | null
  likeCount: number
  recommendCount: number
  isDivine: boolean
  liked: boolean
  createTime: string
  divineTime?: string
  postTitle?: string
  children: CommentVO[]
}

/**
 * Comment create request body
 */
export interface CommentCreateRequest {
  content: string
  parentId?: string
}
