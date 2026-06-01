/**
 * Post draft view object
 */
export interface PostDraft {
  id: string
  postId?: string
  title?: string
  content?: string
  categoryId?: number
  categoryName?: string
  visibility?: number
  lastSavedAt: string
  createTime: string
  updateTime: string
}

/**
 * Draft save request body (upsert semantics)
 */
export interface DraftSaveRequest {
  title?: string
  content?: string
  categoryId?: number
  visibility?: number
  postId?: string
}
