/**
 * Post API module — CRUD, like, favorite
 */
import { api } from '@/api'
import type { R, PostVO, PostListParams, PostCreateRequest, PostUpdateRequest, PageResult } from '@/api/types'

export const postApi = {
  /** Get paginated post list (with filters & sorting) */
  getList: (params?: PostListParams) => api<R<PageResult<PostVO>>>('/posts', { query: params }),

  /** Get post detail by ID */
  getDetail: (id: string) => api<R<PostVO>>(`/posts/${id}`),

  /** Create a new post */
  create: (data: PostCreateRequest) => api<R<PostVO>>('/posts', { method: 'POST', body: data }),

  /** Update an existing post (partial) */
  update: (id: string, data: PostUpdateRequest) =>
    api<R<PostVO>>(`/posts/${id}`, { method: 'PATCH', body: data }),

  /** Soft-delete a post */
  remove: (id: string) => api<R<null>>(`/posts/${id}`, { method: 'DELETE' }),

  /** Like a post */
  like: (id: string) => api<R<null>>(`/posts/${id}/like`, { method: 'POST' }),

  /** Unlike a post */
  unlike: (id: string) => api<R<null>>(`/posts/${id}/like`, { method: 'DELETE' }),

  /** Favorite (bookmark) a post */
  favorite: (id: string) => api<R<null>>(`/posts/${id}/favorite`, { method: 'POST' }),

  /** Unfavorite a post */
  unfavorite: (id: string) => api<R<null>>(`/posts/${id}/favorite`, { method: 'DELETE' }),
}
