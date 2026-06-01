import { describe, it, expect, beforeEach, vi } from 'vitest'
import { ApiError, onRequestInterceptor, onResponseInterceptor, onResponseErrorInterceptor } from '../index'

// ---------------------------------------------------------------------------
// Mocks
// ---------------------------------------------------------------------------

const mockGetToken = vi.fn()
const mockRemoveToken = vi.fn()
const mockRouterPush = vi.fn()
let mockFullPath = '/'

vi.mock('@/utils/token', () => ({
  getToken: mockGetToken,
  removeToken: mockRemoveToken,
}))

vi.mock('@/router', () => ({
  default: {
    currentRoute: {
      value: { fullPath: '/' },
    },
    push: mockRouterPush,
  },
}))

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function mockOptions(headers?: Record<string, string>): Record<string, unknown> {
  return { headers: headers ?? {} }
}

function mockResponse(status: number, data: unknown): { _data: unknown; status: number } {
  return { _data: data, status }
}

beforeEach(() => {
  vi.clearAllMocks()
  mockFullPath = '/'
  vi.mocked(mockGetToken).mockReset()
  vi.mocked(mockRemoveToken).mockReset()
  mockRouterPush.mockReset()

  // Sync the router mock's currentRoute with our variable
  const routerModule = vi.mocked(vi.importActual('@/router') as any)
  // Re-mock doesn't work here; we'll handle it in tests via vi.mocked
})

// ===========================================================================
// ApiError
// ===========================================================================
describe('ApiError', () => {
  it('creates an error with code and message', () => {
    const err = new ApiError(400, 'bad request')
    expect(err).toBeInstanceOf(Error)
    expect(err.code).toBe(400)
    expect(err.message).toBe('bad request')
    expect(err.name).toBe('ApiError')
  })
})

// ===========================================================================
// onRequestInterceptor
// ===========================================================================
describe('onRequestInterceptor', () => {
  it('injects Authorization header when token exists', async () => {
    mockGetToken.mockReturnValue('my-jwt-token')

    const opts = mockOptions()
    await onRequestInterceptor({ options: opts })

    expect(opts.headers).toHaveProperty('Authorization', 'Bearer my-jwt-token')
  })

  it('does not add Authorization header when token is null', async () => {
    mockGetToken.mockReturnValue(null)

    const opts = mockOptions()
    await onRequestInterceptor({ options: opts })

    expect(opts.headers).not.toHaveProperty('Authorization')
  })

  it('preserves existing headers when adding token', async () => {
    mockGetToken.mockReturnValue('token-123')

    const opts = mockOptions({ 'X-Custom': 'value' })
    await onRequestInterceptor({ options: opts })

    expect(opts.headers).toHaveProperty('X-Custom', 'value')
    expect(opts.headers).toHaveProperty('Authorization', 'Bearer token-123')
  })
})

// ===========================================================================
// onResponseInterceptor
// ===========================================================================
describe('onResponseInterceptor', () => {
  it('throws ApiError when API returns error code', async () => {
    const resp = mockResponse(200, { code: 400, message: '参数错误', data: null })

    await expect(onResponseInterceptor({ response: resp })).rejects.toThrow(ApiError)
    await expect(onResponseInterceptor({ response: resp })).rejects.toMatchObject({
      code: 400,
      message: '参数错误',
    })
  })

  it('does not throw when API returns code 200', async () => {
    const resp = mockResponse(200, { code: 200, message: 'success', data: { id: 1 } })

    await expect(onResponseInterceptor({ response: resp })).resolves.toBeUndefined()
  })

  it('does not throw when response body is missing code field', async () => {
    const resp = mockResponse(200, { foo: 'bar' })

    await expect(onResponseInterceptor({ response: resp })).resolves.toBeUndefined()
  })

  it('uses default message when response message is missing', async () => {
    const resp = mockResponse(200, { code: 500, data: null })

    await expect(onResponseInterceptor({ response: resp })).rejects.toMatchObject({
      code: 500,
      message: '请求失败',
    })
  })
})

// ===========================================================================
// onResponseErrorInterceptor
// ===========================================================================
describe('onResponseErrorInterceptor', () => {
  it('handles 401: clears token, redirects to login, throws ApiError', async () => {
    const resp = mockResponse(401, {})

    await expect(onResponseErrorInterceptor({ response: resp })).rejects.toMatchObject({
      code: 401,
      message: '登录已过期，请重新登录',
    })

    expect(mockRemoveToken).toHaveBeenCalledTimes(1)
    expect(mockRouterPush).toHaveBeenCalledWith('/login?redirect=%2F')
  })

  it('handles 401 on login page: does NOT redirect', async () => {
    // Override the currentRoute mock for this test by re-mocking
    // We need currentRoute.fullPath to return '/login'
    const routerMock = (await import('@/router')).default
    vi.spyOn(routerMock.currentRoute.value, 'fullPath' as any, 'get').mockReturnValue('/login')

    const resp = mockResponse(401, {})
    await expect(onResponseErrorInterceptor({ response: resp })).rejects.toThrow(ApiError)

    expect(mockRemoveToken).toHaveBeenCalledTimes(1)
    // Should NOT redirect when already on login/register
    expect(mockRouterPush).not.toHaveBeenCalled()
  })

  it('handles 401 on register page: does NOT redirect', async () => {
    const routerMock = (await import('@/router')).default
    vi.spyOn(routerMock.currentRoute.value, 'fullPath' as any, 'get').mockReturnValue('/register')

    const resp = mockResponse(401, {})
    await expect(onResponseErrorInterceptor({ response: resp })).rejects.toThrow(ApiError)

    expect(mockRemoveToken).toHaveBeenCalledTimes(1)
    expect(mockRouterPush).not.toHaveBeenCalled()
  })

  it('handles 403: throws ApiError without redirect', async () => {
    const resp = mockResponse(403, {})

    await expect(onResponseErrorInterceptor({ response: resp })).rejects.toMatchObject({
      code: 403,
      message: '无权限访问',
    })

    expect(mockRemoveToken).not.toHaveBeenCalled()
    expect(mockRouterPush).not.toHaveBeenCalled()
  })

  it('handles 404: throws ApiError', async () => {
    const resp = mockResponse(404, {})

    await expect(onResponseErrorInterceptor({ response: resp })).rejects.toMatchObject({
      code: 404,
      message: '资源不存在',
    })
  })

  it('handles 500: throws ApiError', async () => {
    const resp = mockResponse(500, {})

    await expect(onResponseErrorInterceptor({ response: resp })).rejects.toMatchObject({
      code: 500,
      message: '服务器错误，请稍后重试',
    })
  })

  it('handles 502: throws generic ApiError', async () => {
    const resp = mockResponse(502, {})

    await expect(onResponseErrorInterceptor({ response: resp })).rejects.toMatchObject({
      code: 502,
      message: '服务器错误，请稍后重试',
    })
  })

  it('handles other status codes with generic message', async () => {
    const resp = mockResponse(418, {})

    await expect(onResponseErrorInterceptor({ response: resp })).rejects.toMatchObject({
      code: 418,
      message: '请求失败 (418)',
    })
  })

  it('always throws (never resolves)', async () => {
    const resp = mockResponse(500, {})
    const result = onResponseErrorInterceptor({ response: resp })

    // It should be a rejected promise, never a fulfilled one
    await expect(result).rejects.toThrow()
  })
})
