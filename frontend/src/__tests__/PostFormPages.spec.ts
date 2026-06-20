import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { ElMessage, ElMessageBox } from 'element-plus'

// ---- Mocks ----

const mockRouterPush = vi.fn<(...args: unknown[]) => unknown>()
const mockRouterReplace = vi.fn<(...args: unknown[]) => unknown>()
const mockRouteParams = { id: '123' }

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: mockRouterPush, replace: mockRouterReplace }),
  useRoute: () => ({ params: mockRouteParams }),
}))

const mockPostApi = {
  create: vi.fn<(...args: unknown[]) => unknown>(),
  update: vi.fn<(...args: unknown[]) => unknown>(),
  getDetail: vi.fn<(...args: unknown[]) => unknown>(),
}

const mockCategoryApi = {
  getList: vi.fn<(...args: unknown[]) => unknown>(),
}

vi.mock('@/api/modules/post', () => ({
  postApi: mockPostApi,
}))

vi.mock('@/api/modules/category', () => ({
  categoryApi: mockCategoryApi,
}))

const mockDraft = {
  currentDraftId: { value: null as string | null },
  draftData: { value: {} as Record<string, unknown> },
  isDirty: { value: false },
  saving: { value: false },
  checkDraft: vi.fn<(...args: unknown[]) => unknown>(),
  restoreDraft: vi.fn<(...args: unknown[]) => unknown>(),
  discardDraft: vi.fn<(...args: unknown[]) => unknown>(),
  saveDraft: vi.fn<(...args: unknown[]) => unknown>(),
  startAutoSave: vi.fn<(...args: unknown[]) => unknown>(),
  stopAutoSave: vi.fn<(...args: unknown[]) => unknown>(),
}

vi.mock('@/composables/useDraft', () => ({
  useDraft: () => mockDraft,
}))

// Mock Element Plus message functions
vi.mock('element-plus', async () => {
  const actual = await vi.importActual('element-plus')
  return {
    ...(actual as object),
    ElMessage: {
      success: vi.fn<(...args: unknown[]) => unknown>(),
      error: vi.fn<(...args: unknown[]) => unknown>(),
      warning: vi.fn<(...args: unknown[]) => unknown>(),
      info: vi.fn<(...args: unknown[]) => unknown>(),
    },
    ElMessageBox: {
      confirm: vi.fn<(...args: unknown[]) => unknown>(),
    },
  }
})

// ---- Stub components ----
const MdEditorStub = {
  name: 'MdEditor',
  template: '<textarea :value="modelValue" @input="onInput" class="md-editor-stub"></textarea>',
  props: ['modelValue'],
  emits: ['update:modelValue'],
  methods: {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    onInput(this: any, event: Event) {
      const target = event.target as HTMLTextAreaElement
      this.$emit('update:modelValue', target.value)
    },
  },
}

const VisibilitySelectorStub = {
  name: 'VisibilitySelector',
  template: '<select :value="modelValue" @change="onChange" class="visibility-stub"><option value="0">公开</option><option value="1">登录可见</option><option value="2">粉丝可见</option><option value="3">私密</option></select>',
  props: ['modelValue'],
  emits: ['update:modelValue'],
  methods: {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    onChange(this: any, event: Event) {
      const target = event.target as HTMLSelectElement
      this.$emit('update:modelValue', Number(target.value))
    },
  },
}

const ElFormStub = {
  name: 'ElForm',
  template: '<form @submit.prevent="handleFormSubmit"><slot /></form>',
  emits: ['submit'],
  methods: {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    validate(this: any) { return Promise.resolve(true) },
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    handleFormSubmit(this: any, event: Event) {
      this.$emit('submit', event)
    },
  },
  inheritAttrs: false,
}

const ElFormItemStub = {
  name: 'ElFormItem',
  template: '<div class="el-form-item"><slot /></div>',
}

const ElInputStub = {
  name: 'ElInput',
  template: '<input :value="modelValue" @input="onInput" class="el-input-stub" />',
  props: ['modelValue', 'placeholder', 'maxlength', 'showWordLimit'],
  emits: ['update:modelValue'],
  methods: {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    onInput(this: any, event: Event) {
      const target = event.target as HTMLInputElement
      this.$emit('update:modelValue', target.value)
    },
  },
}

const ElSelectStub = {
  name: 'ElSelect',
  template: '<select :value="modelValue" @change="onChange" class="el-select-stub"><slot /></select>',
  props: ['modelValue', 'placeholder'],
  emits: ['update:modelValue'],
  methods: {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    onChange(this: any, event: Event) {
      const target = event.target as HTMLSelectElement
      this.$emit('update:modelValue', Number(target.value))
    },
  },
}

const ElOptionStub = {
  name: 'ElOption',
  template: '<option :value="value">{{ label }}</option>',
  props: ['label', 'value'],
}

const ElButtonStub = {
  name: 'ElButton',
  template: '<button :disabled="loading" class="el-button-stub"><slot /></button>',
  props: ['type', 'nativeType', 'loading'],
}

// ---- Helpers ----
function createWrapper(component: Parameters<typeof mount>[0]) {
  return mount(component, {
    global: {
      stubs: {
        MdEditor: MdEditorStub,
        VisibilitySelector: VisibilitySelectorStub,
        'el-form': ElFormStub,
        'el-form-item': ElFormItemStub,
        'el-input': ElInputStub,
        'el-select': ElSelectStub,
        'el-option': ElOptionStub,
        'el-button': ElButtonStub,
      },
    },
  })
}

/** Fill create/edit form fields and submit via form element */
async function fillAndSubmit(wrapper: ReturnType<typeof mount>) {
  const titleInput = wrapper.find('.el-input-stub')
  await titleInput.setValue('测试帖子标题')

  const selectEl = wrapper.find('.el-select-stub')
  await selectEl.setValue(1)

  const editor = wrapper.find('.md-editor-stub')
  await editor.setValue('测试内容')

  // Trigger submit on the native form element inside the el-form stub
  await wrapper.find('form').trigger('submit')
  await flushPromises()
}

beforeEach(() => {
  vi.clearAllMocks()
  mockDraft.currentDraftId.value = null
  mockDraft.draftData.value = {}
  mockDraft.checkDraft.mockResolvedValue(null)

  mockCategoryApi.getList.mockResolvedValue({
    data: [
      { id: 1, name: '前端技术', description: '', sortOrder: 1, status: 1, postCount: 10, createTime: '' },
      { id: 2, name: '后端技术', description: '', sortOrder: 2, status: 1, postCount: 5, createTime: '' },
    ],
  })
})

// ====================================================================
// PostCreatePage
// ====================================================================
describe('PostCreatePage', () => {
  async function mountPage() {
    const { default: PostCreatePage } = await import('@/pages/post/PostCreatePage.vue')
    return createWrapper(PostCreatePage)
  }

  it('renders the create form with all fields and submit button', async () => {
    const wrapper = await mountPage()
    await flushPromises()

    expect(wrapper.find('.post-form-page__title').text()).toBe('发布帖子')
    expect(wrapper.find('.el-input-stub').exists()).toBe(true)
    expect(wrapper.find('.el-select-stub').exists()).toBe(true)
    expect(wrapper.find('.visibility-stub').exists()).toBe(true)
    expect(wrapper.find('.md-editor-stub').exists()).toBe(true)
    expect(wrapper.find('.el-button-stub').exists()).toBe(true)
  })

  it('loads categories on mount', async () => {
    const wrapper = await mountPage()
    await flushPromises()

    expect(mockCategoryApi.getList).toHaveBeenCalledOnce()
    // Only el-select has option children; VisibilitySelector has its own internal options
    const selectOptions = wrapper.find('.el-select-stub').findAll('option')
    expect(selectOptions).toHaveLength(2)
  })

  it('shows error when categories fail to load', async () => {
    mockCategoryApi.getList.mockRejectedValueOnce(new Error('Network error'))
    await mountPage()
    await flushPromises()

    expect(ElMessage.error).toHaveBeenCalledWith('加载版块列表失败')
  })

  it('calls checkDraft on mount', async () => {
    await mountPage()
    await flushPromises()

    expect(mockDraft.checkDraft).toHaveBeenCalledOnce()
  })

  it('shows draft restore dialog when draft exists on mount', async () => {
    mockDraft.checkDraft.mockResolvedValueOnce({ id: 'draft-1', title: '草稿标题', content: '草稿内容' })

    await mountPage()
    await flushPromises()

    expect(ElMessageBox.confirm).toHaveBeenCalledWith(
      '检测到未保存的草稿，是否恢复？',
      '草稿恢复',
      expect.objectContaining({ confirmButtonText: '恢复', cancelButtonText: '放弃', type: 'info' }),
    )
  })

  it('restores draft content into form when user confirms', async () => {
    mockDraft.checkDraft.mockResolvedValueOnce({ id: 'draft-1' })
    mockDraft.restoreDraft.mockResolvedValueOnce({
      id: 'draft-1',
      title: '草稿标题',
      content: '草稿内容',
      categoryId: 2,
      visibility: 1,
    })
    vi.mocked(ElMessageBox.confirm).mockResolvedValueOnce('confirm' as never)

    await mountPage()
    await flushPromises()

    expect(mockDraft.restoreDraft).toHaveBeenCalledWith('draft-1')
  })

  it('discards draft when user cancels restore dialog', async () => {
    mockDraft.checkDraft.mockResolvedValueOnce({ id: 'draft-1' })
    vi.mocked(ElMessageBox.confirm).mockRejectedValueOnce('cancel' as never)

    await mountPage()
    await flushPromises()

    expect(mockDraft.discardDraft).toHaveBeenCalledWith('draft-1')
  })

  it('starts auto-save on mount and stops on unmount', async () => {
    const wrapper = await mountPage()
    await flushPromises()

    expect(mockDraft.startAutoSave).toHaveBeenCalledOnce()

    wrapper.unmount()
    expect(mockDraft.stopAutoSave).toHaveBeenCalledOnce()
  })

  it('submits successfully and redirects to new post', async () => {
    const createdPost = { id: 'new-post-id', title: 'Test' }
    mockPostApi.create.mockResolvedValueOnce({ data: createdPost })

    const wrapper = await mountPage()
    await flushPromises()
    await fillAndSubmit(wrapper)

    expect(mockPostApi.create).toHaveBeenCalledWith(
      expect.objectContaining({
        title: '测试帖子标题',
        content: '测试内容',
        categoryId: 1,
        visibility: 0,
      }),
    )
    expect(ElMessage.success).toHaveBeenCalledWith('发布成功')
    expect(mockRouterPush).toHaveBeenCalledWith('/posts/new-post-id')
  })

  it('shows error message on submit failure', async () => {
    mockPostApi.create.mockRejectedValueOnce(new Error('服务器错误'))

    const wrapper = await mountPage()
    await flushPromises()

    // Fill form
    await wrapper.find('.el-input-stub').setValue('标题')
    await wrapper.find('.el-select-stub').setValue(1)
    await wrapper.find('.md-editor-stub').setValue('内容')

    // Submit
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(ElMessage.error).toHaveBeenCalled()
  })

  it('discards draft after successful publish', async () => {
    mockDraft.currentDraftId.value = 'draft-to-clean'
    mockPostApi.create.mockResolvedValueOnce({ data: { id: 'new-post-id' } })

    const wrapper = await mountPage()
    await flushPromises()
    await fillAndSubmit(wrapper)

    expect(mockDraft.discardDraft).toHaveBeenCalled()
  })
})

// ====================================================================
// PostEditPage
// ====================================================================
describe('PostEditPage', () => {
  const existingPost = {
    id: '123',
    title: '原始标题',
    content: '原始内容',
    categoryId: 1,
    categoryName: '前端技术',
    visibility: 0,
    author: { id: '1', username: 'test', avatarUrl: null, bio: null, role: 'USER' },
    type: 0,
    status: 1,
    viewCount: 10,
    likeCount: 2,
    commentCount: 0,
    favoriteCount: 0,
    divineCommentCount: 0,
    isLiked: false,
    isFavorited: false,
    createTime: '2026-01-01',
    updateTime: '2026-01-01',
  }

  async function mountPage() {
    mockPostApi.getDetail.mockResolvedValue({ data: { ...existingPost } })
    const { default: PostEditPage } = await import('@/pages/post/PostEditPage.vue')
    return createWrapper(PostEditPage)
  }

  it('renders the edit form with correct title', async () => {
    const wrapper = await mountPage()
    await flushPromises()

    expect(wrapper.find('.post-form-page__title').text()).toBe('编辑帖子')
  })

  it('fetches post on mount by route param id', async () => {
    await mountPage()
    await flushPromises()

    expect(mockPostApi.getDetail).toHaveBeenCalledWith('123')
  })

  it('shows error and redirects home on post fetch failure', async () => {
    mockPostApi.getDetail.mockRejectedValueOnce(new Error('Not found'))

    await mountPage()
    await flushPromises()

    expect(ElMessage.error).toHaveBeenCalledWith('加载帖子失败')
    expect(mockRouterReplace).toHaveBeenCalledWith('/')
  })

  it('checks for existing draft on mount', async () => {
    await mountPage()
    await flushPromises()

    expect(mockDraft.checkDraft).toHaveBeenCalledOnce()
  })

  it('restores draft when editing and draft exists', async () => {
    mockDraft.checkDraft.mockResolvedValueOnce({ data: { id: 'draft-edit-1' } })
    mockDraft.restoreDraft.mockResolvedValueOnce({
      id: 'draft-edit-1',
      title: '草稿修改后的标题',
      content: '草稿修改后的内容',
      categoryId: 2,
      visibility: 1,
    })
    vi.mocked(ElMessageBox.confirm).mockResolvedValueOnce('confirm' as never)

    await mountPage()
    await flushPromises()

    expect(mockDraft.restoreDraft).toHaveBeenCalledWith('draft-edit-1')
  })

  it('submits update successfully and redirects to post', async () => {
    mockPostApi.update.mockResolvedValueOnce({ data: { ...existingPost, title: '修改后标题' } })

    const wrapper = await mountPage()
    await flushPromises()
    await fillAndSubmit(wrapper)

    expect(mockPostApi.update).toHaveBeenCalledWith(
      '123',
      expect.objectContaining({
        title: '测试帖子标题',
        content: '测试内容',
        categoryId: 1,
        visibility: 0,
      }),
    )
    expect(ElMessage.success).toHaveBeenCalledWith('更新成功')
    expect(mockRouterPush).toHaveBeenCalledWith('/posts/123')
  })

  it('shows error message on update failure', async () => {
    mockPostApi.update.mockRejectedValueOnce(new Error('更新失败'))

    const wrapper = await mountPage()
    await flushPromises()

    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(ElMessage.error).toHaveBeenCalled()
  })

  it('discards draft after successful update', async () => {
    mockDraft.currentDraftId.value = 'draft-to-clean'
    mockPostApi.update.mockResolvedValueOnce({ data: existingPost })

    const wrapper = await mountPage()
    await flushPromises()
    await fillAndSubmit(wrapper)

    expect(mockDraft.discardDraft).toHaveBeenCalled()
  })
})
