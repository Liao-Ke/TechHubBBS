import { describe, it, expect, vi, beforeEach } from 'vitest'
import { api } from '@/api'
import { noticeApi } from '@/api/modules/notice'

vi.mock('@/api', () => ({
  api: vi.fn(),
}))

const makeCreateReq = () => ({ title: '标题', content: '内容', type: 0, isPinned: 0 })
const makeUpdateReq = () => ({ title: '新标题' })

describe('noticeApi', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('getList calls /categories/{id}/notices when categoryId is provided', () => {
    noticeApi.getList({ categoryId: 'cat-1', type: 'NOTICE', page: 1, size: 20 })
    expect(api).toHaveBeenCalledWith('/categories/cat-1/notices', {
      query: { type: 'NOTICE' },
    })
  })

  it('getList calls /notices when no categoryId', () => {
    noticeApi.getList({ type: 'NOTICE', page: 1, size: 20 })
    expect(api).toHaveBeenCalledWith('/notices', {
      query: { type: 'NOTICE', page: 1, size: 20 },
    })
  })

  it('getList works without params', () => {
    noticeApi.getList()
    expect(api).toHaveBeenCalledWith('/notices', {
      query: {},
    })
  })

  it('getDetail calls GET /notices/{id}', () => {
    noticeApi.getDetail('notice-1')
    expect(api).toHaveBeenCalledWith('/notices/notice-1')
  })

  it('create calls POST /categories/{categoryId}/notices', () => {
    const req = makeCreateReq()
    noticeApi.create('cat-10', req)
    expect(api).toHaveBeenCalledWith('/categories/cat-10/notices', {
      method: 'POST',
      body: req,
    })
  })

  it('update calls PATCH /notices/{id}', () => {
    const req = makeUpdateReq()
    noticeApi.update('notice-1', req)
    expect(api).toHaveBeenCalledWith('/notices/notice-1', {
      method: 'PATCH',
      body: req,
    })
  })

  it('delete calls DELETE /notices/{id}', () => {
    noticeApi.delete('notice-1')
    expect(api).toHaveBeenCalledWith('/notices/notice-1', {
      method: 'DELETE',
    })
  })
})
