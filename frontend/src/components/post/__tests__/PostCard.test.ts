import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import PostCard from '../PostCard.vue'
import type { PostVO } from '@/api/types/post'

function makePost(overrides: Partial<PostVO> = {}): PostVO {
  return {
    id: '1',
    title: '测试帖子标题',
    content: '# Markdown content',
    summary: '这是摘要内容',
    author: {
      id: 'u1',
      username: 'testuser',
      avatarUrl: 'https://example.com/avatar.jpg',
      role: 'user',
      status: 1,
      createTime: '2026-01-01T00:00:00Z',
    },
    categoryId: 1,
    categoryName: '技术讨论',
    visibility: 0,
    type: 0,
    status: 1,
    viewCount: 128,
    likeCount: 32,
    commentCount: 15,
    favoriteCount: 3,
    divineCommentCount: 0,
    isLiked: false,
    isFavorited: false,
    createTime: '2026-05-28T10:00:00Z',
    updateTime: '2026-05-28T10:00:00Z',
    ...overrides,
  }
}

const mountCard = (post: PostVO | null) =>
  mount(PostCard, {
    props: { post },
    global: {
      stubs: {
        'router-link': {
          template: '<a :href="to"><slot /></a>',
          props: ['to'],
        },
      },
    },
  })

describe('PostCard.vue', () => {
  // ── Skeleton / loading ──
  describe('loading skeleton', () => {
    it('renders skeleton when post is null', () => {
      const wrapper = mountCard(null)
      const article = wrapper.find('article')
      expect(article.classes()).toContain('post-card--skeleton')
      expect(article.attributes('aria-busy')).toBe('true')
    })

    it('renders skeleton placeholder elements', () => {
      const wrapper = mountCard(null)
      expect(wrapper.find('.post-card__skeleton-avatar').exists()).toBe(true)
      expect(wrapper.findAll('.post-card__skeleton-line').length).toBeGreaterThan(0)
    })
  })

  // ── Normal post ──
  describe('normal post rendering', () => {
    it('renders author username', () => {
      const wrapper = mountCard(makePost())
      expect(wrapper.text()).toContain('testuser')
    })

    it('renders title as link to post detail', () => {
      const wrapper = mountCard(makePost({ id: '42', title: 'Vue 3 实战' }))
      const link = wrapper.find('.post-card__title-link')
      expect(link.text()).toBe('Vue 3 实战')
      expect(link.attributes('href')).toBe('/posts/42')
    })

    it('renders summary text', () => {
      const wrapper = mountCard(makePost({ summary: '一段摘要' }))
      expect(wrapper.find('.post-card__summary').text()).toContain('一段摘要')
    })

    it('does not render summary when not provided', () => {
      const wrapper = mountCard(makePost({ summary: undefined }))
      expect(wrapper.find('.post-card__summary').exists()).toBe(false)
    })

    it('renders category tag with link', () => {
      const wrapper = mountCard(makePost({ categoryId: 5, categoryName: '前端开发' }))
      const cat = wrapper.find('.post-card__category')
      expect(cat.text()).toBe('前端开发')
      expect(cat.attributes('href')).toBe('/categories/5')
    })

    it('renders stats: views, likes, comments', () => {
      const wrapper = mountCard(makePost({ viewCount: 100, likeCount: 20, commentCount: 5 }))
      const text = wrapper.text()
      expect(text).toContain('100')
      expect(text).toContain('20')
      expect(text).toContain('5')
    })

    it('renders divine count when > 0', () => {
      const wrapper = mountCard(makePost({ divineCommentCount: 3 }))
      expect(wrapper.find('.post-card__stat--divine').exists()).toBe(true)
      expect(wrapper.find('.post-card__stat--divine').text()).toContain('3')
    })

    it('does not render divine stat when 0', () => {
      const wrapper = mountCard(makePost({ divineCommentCount: 0 }))
      expect(wrapper.find('.post-card__stat--divine').exists()).toBe(false)
    })

    it('renders relative time', () => {
      const wrapper = mountCard(makePost({ createTime: new Date().toISOString() }))
      expect(wrapper.find('.post-card__time').text()).toBe('刚刚')
    })
  })

  // ── Pinned post ──
  describe('pinned post (type=2)', () => {
    it('adds pinned class', () => {
      const wrapper = mountCard(makePost({ type: 2 }))
      expect(wrapper.find('.post-card--pinned').exists()).toBe(true)
    })

    it('shows pinned badge', () => {
      const wrapper = mountCard(makePost({ type: 2 }))
      expect(wrapper.text()).toContain('置顶')
    })
  })

  // ── Featured post ──
  describe('featured post (type=1)', () => {
    it('adds featured class', () => {
      const wrapper = mountCard(makePost({ type: 1 }))
      expect(wrapper.find('.post-card--featured').exists()).toBe(true)
    })

    it('shows featured badge', () => {
      const wrapper = mountCard(makePost({ type: 1 }))
      expect(wrapper.text()).toContain('精华')
    })
  })

  // ── Visibility ──
  describe('visibility indicator', () => {
    it('renders visibility icon for public posts', () => {
      const wrapper = mountCard(makePost({ visibility: 0 }))
      expect(wrapper.find('.post-card__visibility').exists()).toBe(true)
    })

    it('renders visibility icon for private posts', () => {
      const wrapper = mountCard(makePost({ visibility: 3 }))
      expect(wrapper.find('.post-card__visibility').exists()).toBe(true)
    })
  })
})
