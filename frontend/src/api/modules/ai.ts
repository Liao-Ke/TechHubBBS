/**
 * AI API module — summaries and Q&A
 */
import { api } from '@/api'
import type { R, AiSummaryResponse, AiQaRequest, AiQaResponse, PageResult } from '@/api/types'

export const aiApi = {
  /** Get existing AI summary for a post */
  getSummary: (postId: string) =>
    api<R<AiSummaryResponse>>(`/posts/${postId}/ai/summary`),

  /** Generate (or regenerate) AI summary for a post */
  generateSummary: (postId: string) =>
    api<R<AiSummaryResponse>>(`/posts/${postId}/ai/summary`, { method: 'POST' }),

  /** Ask a question about a post's content */
  askQuestion: (postId: string, data: AiQaRequest) =>
    api<R<AiQaResponse>>(`/posts/${postId}/ai/qa`, { method: 'POST', body: data }),

  /** Get Q&A history for a post */
  getQaHistory: (postId: string, params?: { page?: number; size?: number }) =>
    api<R<PageResult<AiQaResponse>>>(`/posts/${postId}/ai/qa`, { query: params }),
}
