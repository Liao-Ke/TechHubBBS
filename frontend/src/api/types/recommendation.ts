/**
 * Recommendation item (personalised "for you")
 */
export interface RecommendationVO {
  postId: string
  title: string
  authorName: string
  authorAvatar?: string
  likeCount?: number
  commentCount?: number
  viewCount?: number
  similarityScore: number | null
  reason: string
}

/**
 * Related post item (similarity-based)
 */
export interface RelatedPostVO {
  postId: string
  title: string
  authorName: string
  similarityScore: number | null
}
