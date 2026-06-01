import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import PostList from '../PostList.vue'
import type { PostVO } from '@/api/types/post'

function makePost(id: string, title: string): PostVO {
  return {
    id,
    title,
    content: '# Content',
    summary: 'Summary',
    author: {
      id: 'u1',
      username: 'user' + id,
      avatarUrl: '',
      role: 'user',
      status: 1,
      createTime: '2026-01-01T00:00:00Z',
    },
    categoryId: 1,
    categoryName: 'Test',
    visibility: 0,
    type: 0,
    status: 1,
    viewCount: 10,
    likeCount: 5,
    commentCount: 3,
    favoriteCount: 1,
    divineCommentCount: 0,
    isLiked: false,
    isFavorited: false,
    createTime: '2026-05-28T10:00:00Z',
    updateTime: '2026-05-28T10:00:00Z',
  }
}

const mountList = (props: {
  posts: PostVO[]
  loading: boolean
  currentPage?: number
  totalPages?: number
  total?: number
}) =>
  mount(PostList, {
    props: {
      currentPage: 1,
      totalPages: 1,
      total: 0,
      ...props,
    },
    global: {
      stubs: {
        'router-link': {
          template: '<a :href="to"><slot /></a>',
          props: ['to'],
        },
      },
    },
  })

describe('PostList.vue', () => {
  // ── Loading ──
  describe('loading state', () => {
    it('shows skeleton cards when loading', () => {
      const wrapper = mountList({ posts: [], loading: true })
      const cards = wrapper.findAll('.post-card--skeleton')
      expect(cards.length).toBe(3)
    })

    it('does not show empty state while loading (even if posts empty)', () => {
      const wrapper = mountList({ posts: [], loading: true })
      expect(wrapper.find('.post-list__empty').exists()).toBe(false)
    })
  })

  // ── Empty ──
  describe('empty state', () => {
    it('shows empty message when no posts and not loading', () => {
      const wrapper = mountList({ posts: [], loading: false })
      expect(wrapper.find('.post-list__empty').exists()).toBe(true)
      expect(wrapper.text()).toContain('暂无帖子')
    })

    it('shows empty hint text', () => {
      const wrapper = mountList({ posts: [], loading: false })
      expect(wrapper.text()).toContain('这里还没有任何内容发布')
    })
  })

  // ── Posts ──
  describe('post list rendering', () => {
    it('renders PostCard for each post', () => {
      const posts = [makePost('1', 'Post A'), makePost('2', 'Post B')]
      const wrapper = mountList({ posts, loading: false })
      const cards = wrapper.findAllComponents({ name: 'PostCard' })
      expect(cards.length).toBe(2)
    })

    it('renders post titles', () => {
      const posts = [makePost('1', 'Post A'), makePost('2', 'Post B')]
      const wrapper = mountList({ posts, loading: false })
      expect(wrapper.text()).toContain('Post A')
      expect(wrapper.text()).toContain('Post B')
    })

    it('does not show empty state when posts exist', () => {
      const wrapper = mountList({ posts: [makePost('1', 'A')], loading: false })
      expect(wrapper.find('.post-list__empty').exists()).toBe(false)
    })

    it('does not show skeleton when posts exist and not loading', () => {
      const wrapper = mountList({ posts: [makePost('1', 'A')], loading: false })
      expect(wrapper.find('.post-card--skeleton').exists()).toBe(false)
    })
  })

  // ── Pagination ──
  describe('pagination', () => {
    it('shows pagination when totalPages > 1', () => {
      const posts = [makePost('1', 'A')]
      const wrapper = mountList({
        posts,
        loading: false,
        currentPage: 1,
        totalPages: 5,
        total: 50,
      })
      expect(wrapper.find('.post-list__pagination').exists()).toBe(true)
    })

    it('hides pagination when totalPages <= 1', () => {
      const posts = [makePost('1', 'A')]
      const wrapper = mountList({
        posts,
        loading: false,
        currentPage: 1,
        totalPages: 1,
        total: 1,
      })
      expect(wrapper.find('.post-list__pagination').exists()).toBe(false)
    })

    it('emits page-change when pagination page changes', async () => {
      const posts = [makePost('1', 'A')]
      const wrapper = mountList({
        posts,
        loading: false,
        currentPage: 1,
        totalPages: 3,
        total: 30,
      })

      const pagination = wrapper.findComponent({ name: 'ElPagination' })
      await pagination.vm.$emit('current-change', 2)

      expect(wrapper.emitted('page-change')).toBeTruthy()
      expect(wrapper.emitted('page-change')![0]).toEqual([2])
    })
  })
})
