import { describe, it, expect, vi, beforeEach } from 'vitest'
import type { R, LoginRequest, LoginResponse } from '@/api/types'

const mockApi = vi.fn()
vi.mock('@/api', () => ({
  api: mockApi,
}))

const { authApi } = await import('../auth')

describe('authApi', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('login sends POST /auth/login with body', async () => {
    const mockData: LoginRequest = { username: 'alice', password: 'secret' }
    const mockResponse: R<LoginResponse> = {
      code: 200,
      message: 'success',
      data: { token: 'jwt', tokenType: 'Bearer', userId: '1', username: 'alice', role: 'user' },
    }
    mockApi.mockResolvedValue(mockResponse)

    const result = await authApi.login(mockData)
    expect(mockApi).toHaveBeenCalledWith('/auth/login', { method: 'POST', body: mockData })
    expect(result).toEqual(mockResponse)
  })

  it('register sends POST /auth/register with body', async () => {
    const mockData = { username: 'bob', password: '123456', email: 'bob@test.com' }
    const mockResponse: R<null> = { code: 200, message: '注册成功', data: null }
    mockApi.mockResolvedValue(mockResponse)

    const result = await authApi.register(mockData)
    expect(mockApi).toHaveBeenCalledWith('/auth/register', { method: 'POST', body: mockData })
    expect(result).toEqual(mockResponse)
  })
})
