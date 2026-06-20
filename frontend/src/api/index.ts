import { ofetch } from 'ofetch'
import type { ApiResponse } from '@/api/types/response'

// Augment ofetch FetchOptions to support upload progress callback
declare module 'ofetch' {
  interface FetchOptions {
    onUploadProgress?: (progress: { progress?: number }) => void
  }
}

const API_BASE = import.meta.env.VITE_API_BASE_URL || '/api/v1'

// ---------------------------------------------------------------------------
// ApiError — structured error with numeric code
// ---------------------------------------------------------------------------
export class ApiError extends Error {
  code: number

  constructor(code: number, message: string) {
    super(message)
    this.code = code
    this.name = 'ApiError'
  }
}

// ---------------------------------------------------------------------------
// Interceptor functions (exported for unit-testability)
// ---------------------------------------------------------------------------

// eslint-disable-next-line @typescript-eslint/no-explicit-any
export async function onRequestInterceptor(context: any): Promise<void> {
  // Dynamic import to avoid circular dependency with stores / utils
  const { getToken } = await import('@/utils/token')
  const token = getToken()

  const headers: Record<string, string> = {
    ...(context.options.headers as Record<string, string> | undefined),
  }

  if (token) {
    headers.Authorization = `Bearer ${token}`
  }

  // For FormData requests, let the browser set the correct multipart Content-Type
  // with boundary — otherwise the default application/json header can break uploads.
  if (context.options.body instanceof FormData) {
    delete headers['Content-Type']
  }

  context.options.headers = headers
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
export async function onResponseInterceptor(context: any): Promise<void> {
  const body = context.response._data as ApiResponse<unknown> | null
  // If the HTTP status is 2xx but the API returned a business-error code
  if (body && typeof body.code === 'number' && body.code !== 200) {
    throw new ApiError(body.code, body.message || '请求失败')
  }
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
export async function onResponseErrorInterceptor(context: any): Promise<never> {
  const response = context.response
  const status = response.status

  if (status === 401) {
    // Unauthorized: clear token and redirect to login
    const { removeToken } = await import('@/utils/token')
    removeToken()
    const router = (await import('@/router')).default
    const currentPath = router.currentRoute.value.fullPath
    if (currentPath !== '/login' && currentPath !== '/register') {
      router.push(`/login?redirect=${encodeURIComponent(currentPath)}`)
    }
    throw new ApiError(401, '登录已过期，请重新登录')
  }

  if (status === 403) {
    throw new ApiError(403, '无权限访问')
  }

  if (status === 404) {
    throw new ApiError(404, '资源不存在')
  }

  if (status >= 500) {
    throw new ApiError(status, '服务器错误，请稍后重试')
  }

  throw new ApiError(status, `请求失败 (${status})`)
}

// ---------------------------------------------------------------------------
// ofetch instance used by every API module
// ---------------------------------------------------------------------------
export const api = ofetch.create({
  baseURL: API_BASE,
  timeout: 15_000,
  headers: { 'Content-Type': 'application/json' },

  onRequest: onRequestInterceptor,
  onResponse: onResponseInterceptor,
  onResponseError: onResponseErrorInterceptor,
})

// Convenience re-export so API modules can import type from the same place
export type { ApiResponse }
