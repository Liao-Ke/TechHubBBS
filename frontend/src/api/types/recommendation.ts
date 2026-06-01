import type { UserProfileVO } from './user'

/**
 * Recommendation item (personalised "for you")
 */
export interface RecommendationVO {
  id: string
  postId: string
  title: string
  summary: string
  author: UserProfileVO
  score: number
  reason: string
}

/**
 * Related post item (similarity-based)
 */
export interface RelatedPostVO {
  id: string
  postId: string
  title: string
  similarity: number
}
