/**
 * Draft API module
 */
import { api } from '@/api'
import type { R, PostDraft, DraftSaveRequest } from '@/api/types'

export const draftApi = {
  /** Save (upsert) a draft */
  save: (data: DraftSaveRequest) =>
    api<R<PostDraft>>('/drafts', { method: 'POST', body: data }),

  /** Get all drafts for the current user */
  getList: () => api<R<PostDraft[]>>('/drafts'),

  /** Get draft detail by ID */
  getDetail: (id: string) => api<R<PostDraft>>(`/drafts/${id}`),

  /** Check if a draft exists for the given postId (or most recent draft) */
  check: (postId?: string) =>
    api<R<PostDraft | null>>('/drafts/check', {
      query: postId ? { postId } : {},
    }),

  /** Delete a draft by ID */
  remove: (id: string) => api<R<null>>(`/drafts/${id}`, { method: 'DELETE' }),
}
