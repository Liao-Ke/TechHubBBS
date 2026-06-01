/**
 * AI summary status
 */
export type AiSummaryStatus = 0 | 1 | 2

/**
 * AI summary response
 */
export interface AiSummaryResponse {
  id: string
  postId: string
  content: string | null
  status: AiSummaryStatus
  errorMessage?: string
  createTime: string
  updateTime: string
}

/**
 * AI QA request body
 */
export interface AiQaRequest {
  question: string
}

/**
 * AI QA response
 */
export interface AiQaResponse {
  id: string
  question: string
  answer: string
  createTime: string
}
