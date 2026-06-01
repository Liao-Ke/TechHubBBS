/**
 * Auth API module — login & register
 */
import { api } from '@/api'
import type { R, LoginRequest, LoginResponse, RegisterRequest } from '@/api/types'

export const authApi = {
  /** Login — returns JWT token + user info */
  login: (data: LoginRequest) => api<R<LoginResponse>>('/auth/login', { method: 'POST', body: data }),

  /** Register — creates a new account */
  register: (data: RegisterRequest) => api<R<null>>('/auth/register', { method: 'POST', body: data }),
}
