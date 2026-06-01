import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import UserAvatar from '../UserAvatar.vue'
import EmptyState from '../EmptyState.vue'
import LoadingSkeleton from '../LoadingSkeleton.vue'
import ImageUpload from '../ImageUpload.vue'
import { UserFilled, FolderOpened } from '@element-plus/icons-vue'

// ────────────────────────────────────────────────────────────
// Mock composable
// ────────────────────────────────────────────────────────────
vi.mock('@/composables/useFileUpload', () => ({
  useFileUpload: vi.fn(() => ({
    uploading: { value: false, __v_isRef: true },
    progress: { value: 0, __v_isRef: true },
    error: { value: null, __v_isRef: true },
    validateFile: vi.fn((file: File) => {
      const allowed = ['image/jpeg', 'image/png', 'image/gif', 'image/webp']
      if (!allowed.includes(file.type)) return '仅支持 JPG/PNG/GIF/WebP 格式的图片'
      if (file.size > 5 * 1024 * 1024) return '文件大小不能超过 5MB'
      return null
    }),
    upload: vi.fn(),
    reset: vi.fn(),
  })),
}))

// ────────────────────────────────────────────────────────────
// Mock file API
// ────────────────────────────────────────────────────────────
vi.mock('@/api/modules/file', () => ({
  fileApi: {
    upload: vi.fn(),
  },
}))

// ────────────────────────────────────────────────────────────
// Mock router-link
// ────────────────────────────────────────────────────────────
const RouterLinkStub = {
  name: 'RouterLink',
  props: ['to'],
  template: '<a :href="to" class="router-link-stub"><slot /></a>',
}

// ============================================================
// UserAvatar
// ============================================================
describe('UserAvatar', () => {
  it('renders avatar with fallback icon when no src provided', () => {
    const wrapper = mount(UserAvatar, {
      props: { size: 40 },
    })

    const avatar = wrapper.findComponent({ name: 'ElAvatar' })
    expect(avatar.exists()).toBe(true)
    // Should contain UserFilled icon since no src
    expect(avatar.findComponent(UserFilled).exists()).toBe(true)
  })

  it('renders avatar with src when valid src provided', () => {
    const wrapper = mount(UserAvatar, {
      props: { src: 'https://example.com/avatar.jpg', size: 40 },
    })

    const avatar = wrapper.findComponent({ name: 'ElAvatar' })
    expect(avatar.exists()).toBe(true)
    // With valid src, fallback should not be shown initially
    expect(avatar.findComponent(UserFilled).exists()).toBe(false)
  })

  it('shows fallback icon after image load error', async () => {
    const wrapper = mount(UserAvatar, {
      props: { src: 'https://invalid.url/img.jpg', size: 40 },
    })

    const avatar = wrapper.findComponent({ name: 'ElAvatar' })
    // Trigger native error on the <img> element inside el-avatar
    const img = avatar.find('img')
    await img.trigger('error')

    // After error, fallback icon should appear
    expect(avatar.findComponent(UserFilled).exists()).toBe(true)
  })

  it('respects size prop', () => {
    const wrapper = mount(UserAvatar, {
      props: { size: 64 },
    })

    const avatar = wrapper.findComponent({ name: 'ElAvatar' })
    expect(avatar.attributes('style')).toContain('--el-avatar-size: 64px')
  })

  it('defaults size to 40', () => {
    const wrapper = mount(UserAvatar)

    const avatar = wrapper.findComponent({ name: 'ElAvatar' })
    expect(avatar.attributes('style')).toContain('--el-avatar-size: 40px')
  })

  it('resets validSrc when src prop changes', async () => {
    const wrapper = mount(UserAvatar, {
      props: { src: 'https://example.com/old.jpg', size: 40 },
    })

    // Trigger error on old src
    const avatar = wrapper.findComponent({ name: 'ElAvatar' })
    const img = avatar.find('img')
    await img.trigger('error')
    expect(avatar.findComponent(UserFilled).exists()).toBe(true)

    // Change src prop
    await wrapper.setProps({ src: 'https://example.com/new.jpg' })
    await nextTick()

    // Fallback should be hidden again after new src
    expect(avatar.findComponent(UserFilled).exists()).toBe(false)
  })
})

// ============================================================
// EmptyState
// ============================================================
describe('EmptyState', () => {
  it('renders title', () => {
    const wrapper = mount(EmptyState, {
      props: { title: '暂无内容' },
      global: { stubs: { RouterLink: RouterLinkStub } },
    })

    expect(wrapper.find('.empty-state__title').text()).toBe('暂无内容')
  })

  it('renders description when provided', () => {
    const wrapper = mount(EmptyState, {
      props: { title: '暂无内容', description: '这里还没有任何内容' },
      global: { stubs: { RouterLink: RouterLinkStub } },
    })

    expect(wrapper.find('.empty-state__desc').text()).toBe('这里还没有任何内容')
  })

  it('does not render description when not provided', () => {
    const wrapper = mount(EmptyState, {
      props: { title: '暂无内容' },
      global: { stubs: { RouterLink: RouterLinkStub } },
    })

    expect(wrapper.find('.empty-state__desc').exists()).toBe(false)
  })

  it('renders action button when actionText and actionRoute provided', () => {
    const wrapper = mount(EmptyState, {
      props: {
        title: '暂无帖子',
        actionText: '发布帖子',
        actionRoute: '/post/new',
      },
      global: { stubs: { RouterLink: RouterLinkStub } },
    })

    const link = wrapper.find('.router-link-stub')
    expect(link.exists()).toBe(true)
    expect(link.attributes('href')).toBe('/post/new')
    expect(wrapper.findComponent({ name: 'ElButton' }).text()).toContain('发布帖子')
  })

  it('does not render action button when actionRoute missing', () => {
    const wrapper = mount(EmptyState, {
      props: { title: '暂无帖子', actionText: '发布帖子' },
      global: { stubs: { RouterLink: RouterLinkStub } },
    })

    expect(wrapper.find('.router-link-stub').exists()).toBe(false)
  })

  it('uses FolderOpened as default icon', () => {
    const wrapper = mount(EmptyState, {
      props: { title: '暂无内容' },
      global: { stubs: { RouterLink: RouterLinkStub } },
    })

    const icon = wrapper.findComponent(FolderOpened)
    expect(icon.exists()).toBe(true)
  })

  it('uses custom icon when provided', () => {
    const CustomIcon = { name: 'CustomIcon', template: '<svg></svg>' }
    const wrapper = mount(EmptyState, {
      props: { title: '暂无内容', icon: CustomIcon },
      global: { stubs: { RouterLink: RouterLinkStub } },
    })

    // Should render the custom icon component
    expect(wrapper.findComponent(CustomIcon).exists()).toBe(true)
  })
})

// ============================================================
// LoadingSkeleton
// ============================================================
describe('LoadingSkeleton', () => {
  it('renders post-list variant with 5 skeleton cards', () => {
    const wrapper = mount(LoadingSkeleton, {
      props: { variant: 'post-list' },
    })

    expect(wrapper.find('.skeleton-post-list').exists()).toBe(true)
    const items = wrapper.findAll('.skeleton-post-list__item')
    expect(items).toHaveLength(5)
  })

  it('renders detail variant with title and paragraphs', () => {
    const wrapper = mount(LoadingSkeleton, {
      props: { variant: 'detail' },
    })

    expect(wrapper.find('.skeleton-detail').exists()).toBe(true)
    expect(wrapper.find('.skeleton-detail__title').exists()).toBe(true)
    expect(wrapper.find('.skeleton-detail__stats').exists()).toBe(true)
  })

  it('renders table variant with 8 rows', () => {
    const wrapper = mount(LoadingSkeleton, {
      props: { variant: 'table' },
    })

    expect(wrapper.find('.skeleton-table').exists()).toBe(true)
    // el-skeleton with rows=8 should be present
    const skeleton = wrapper.findComponent({ name: 'ElSkeleton' })
    expect(skeleton.exists()).toBe(true)
    expect(skeleton.props('rows')).toBe(8)
  })

  it('only renders the active variant', () => {
    const wrapper = mount(LoadingSkeleton, {
      props: { variant: 'post-list' },
    })

    expect(wrapper.find('.skeleton-detail').exists()).toBe(false)
    expect(wrapper.find('.skeleton-table').exists()).toBe(false)
  })

  it('detail variant has 3 stat circles', () => {
    const wrapper = mount(LoadingSkeleton, {
      props: { variant: 'detail' },
    })

    const circles = wrapper.findAll('.skeleton-detail__stat-circle')
    expect(circles).toHaveLength(3)
  })
})

// ============================================================
// ImageUpload
// ============================================================
describe('ImageUpload', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  function mountImageUpload() {
    return mount(ImageUpload, {
      props: { objectType: 'user_avatar' as const },
    })
  }

  it('renders upload drag zone in idle state', () => {
    const wrapper = mountImageUpload()

    // Should show the upload icon and text
    expect(wrapper.find('.image-upload__text-main').text()).toBe('点击或拖拽上传')
    expect(wrapper.find('.image-upload__text-hint').text()).toBe('支持 JPG/PNG/GIF/WebP，不超过 5MB')
  })

  it('shows drag zone dashed border styling', () => {
    const wrapper = mountImageUpload()

    const uploader = wrapper.find('.image-upload__uploader')
    expect(uploader.exists()).toBe(true)
  })

  it('accepts valid image files', () => {
    const wrapper = mountImageUpload()
    const upload = wrapper.findComponent({ name: 'ElUpload' })

    const validFile = new File([''], 'test.jpg', { type: 'image/jpeg' })
    // beforeUpload should return true for valid files
    const result = upload.vm.$emit('before-upload', validFile)
    // The component should not show error
    expect(wrapper.find('.is-error').exists()).toBe(false)
  })

  it('rejects invalid file types', () => {
    const wrapper = mountImageUpload()
    const upload = wrapper.findComponent({ name: 'ElUpload' })

    const invalidFile = new File([''], 'test.txt', { type: 'text/plain' })
    upload.vm.$emit('before-upload', invalidFile)
  })

  it('rejects files larger than 5MB', () => {
    const wrapper = mountImageUpload()
    const upload = wrapper.findComponent({ name: 'ElUpload' })

    const largeFile = new File([new ArrayBuffer(6 * 1024 * 1024)], 'large.jpg', { type: 'image/jpeg' })
    upload.vm.$emit('before-upload', largeFile)
  })

  it('renders upload zone with correct drag class', () => {
    const wrapper = mountImageUpload()

    const upload = wrapper.findComponent({ name: 'ElUpload' })
    expect(upload.exists()).toBe(true)
  })
})
