/**
 * Recommendation API module
 *
 * IMPORTANT: getRecommendations returns a List (RecommendationVO[]),
 * NOT a PageResult. Do NOT wrap with PageResult.
 */
import { api } from '@/api'
import type { R, RecommendationVO, RelatedPostVO } from '@/api/types'

export const recommendationApi = {
  /** Get personalised recommendations (returns List, NOT PageResult) */
  getRecommendations: (params?: { page?: number; size?: number }) =>
    api<R<RecommendationVO[]>>('/recommendations', { query: params }),

  /** Get related posts for a given post */
  getRelatedPosts: (postId: string, limit?: number) =>
    api<R<RelatedPostVO[]>>(`/posts/${postId}/related`, { query: { limit } }),
}
