/**
 * Comment API module — CRUD, like, recommend, divine
 */
import { api } from '@/api'
import type { R, CommentVO, CommentCreateRequest, PageResult } from '@/api/types'

export const commentApi = {
  /** Get paginated comments for a post */
  getList: (postId: string, params?: { page?: number; size?: number }) =>
    api<R<PageResult<CommentVO>>>(`/posts/${postId}/comments`, { query: params }),

  /** Create a comment on a post */
  create: (postId: string, data: CommentCreateRequest) =>
    api<R<CommentVO>>(`/posts/${postId}/comments`, { method: 'POST', body: data }),

  /** Delete a comment (author / moderator) */
  remove: (id: string) => api<R<null>>(`/comments/${id}`, { method: 'DELETE' }),

  /** Like a comment */
  like: (id: string) => api<R<null>>(`/comments/${id}/like`, { method: 'POST' }),

  /** Unlike a comment */
  unlike: (id: string) => api<R<null>>(`/comments/${id}/like`, { method: 'DELETE' }),

  /** Recommend a comment as divine */
  recommend: (id: string) => api<R<null>>(`/comments/${id}/recommend`, { method: 'POST' }),

  /** Unrecommend a comment */
  unrecommend: (id: string) => api<R<null>>(`/comments/${id}/recommend`, { method: 'DELETE' }),

  /** Get divine comments for a post */
  getDivineComments: (postId: string) => api<R<CommentVO[]>>(`/posts/${postId}/comments/divine`),
}
