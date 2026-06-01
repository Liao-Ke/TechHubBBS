/**
 * Unified API response wrapper
 */
export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

/** Convenience alias */
export type R<T> = ApiResponse<T>

/**
 * Paginated result wrapper
 */
export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}
