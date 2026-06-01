/**
 * Notice (category announcement) API module
 */
import { api } from '@/api'
import type {
  R,
  CategoryNoticeVO,
  CategoryNoticeCreateRequest,
  CategoryNoticeUpdateRequest,
  PageResult,
} from '@/api/types'

export const noticeApi = {
  /** Get notice list (paginated, filterable by category / type) */
  getList: (params?: { categoryId?: string; type?: string; page?: number; size?: number }) =>
    api<R<PageResult<CategoryNoticeVO>>>('/notices', { query: params }),

  /** Get notice detail by ID */
  getDetail: (id: string) =>
    api<R<CategoryNoticeVO>>(`/notices/${id}`),

  /** Create a notice under a category (ADMIN/MODERATOR) */
  create: (categoryId: string, data: CategoryNoticeCreateRequest) =>
    api<R<CategoryNoticeVO>>(`/categories/${categoryId}/notices`, { method: 'POST', body: data }),

  /** Update a notice */
  update: (id: string, data: CategoryNoticeUpdateRequest) =>
    api<R<null>>(`/notices/${id}`, { method: 'PATCH', body: data }),

  /** Delete a notice */
  delete: (id: string) =>
    api<R<null>>(`/notices/${id}`, { method: 'DELETE' }),
}
