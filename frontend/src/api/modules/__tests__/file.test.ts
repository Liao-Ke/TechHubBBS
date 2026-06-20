import { describe, it, expect, vi, beforeEach } from 'vitest'
import { api } from '@/api'
import { fileApi } from '@/api/modules/file'

vi.mock('@/api', () => ({
  api: vi.fn<(...args: unknown[]) => unknown>(),
}))

function createMockFile(name: string, type: string): File {
  return new File(['x'], name, { type })
}

describe('fileApi', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('upload creates FormData and calls POST /files/upload', () => {
    const file = createMockFile('test.png', 'image/png')
    fileApi.upload(file, 'post_image')
    expect(api).toHaveBeenCalledTimes(1)
    const mockCalls = vi.mocked(api).mock.calls
    const url = mockCalls[0]![0]
    const options = mockCalls[0]![1] as any
    expect(url).toBe('/files/upload')
    expect(options.method).toBe('POST')
    expect(options.body).toBeInstanceOf(FormData)
    expect((options.body as FormData).get('file')).toBe(file)
    expect((options.body as FormData).get('objectType')).toBe('post_image')
  })

  it('upload passes onUploadProgress when callback provided', () => {
    const file = createMockFile('avatar.jpg', 'image/jpeg')
    const onProgress = vi.fn<(...args: unknown[]) => unknown>()
    fileApi.upload(file, 'user_avatar', onProgress)
    expect(api).toHaveBeenCalledTimes(1)
    const mockCalls = vi.mocked(api).mock.calls
    const options = mockCalls[0]![1] as any
    expect(options.onUploadProgress).toBeDefined()
  })

  it('upload does NOT include onUploadProgress when callback omitted', () => {
    const file = createMockFile('pic.png', 'image/png')
    fileApi.upload(file, 'post_image')
    const mockCalls = vi.mocked(api).mock.calls
    const options = mockCalls[0]![1] as any
    expect(options.onUploadProgress).toBeUndefined()
  })

  it('upload progress callback converts 0-1 ratio to 0-100 percent', () => {
    const file = createMockFile('test.png', 'image/png')
    const onProgress = vi.fn<(...args: unknown[]) => unknown>()
    fileApi.upload(file, 'post_image', onProgress)

    const mockCalls = vi.mocked(api).mock.calls
    const options = mockCalls[0]![1] as any
    const progressFn = options.onUploadProgress

    // Simulate ofetch calling with progress = 0.5
    progressFn({ progress: 0.5 })
    expect(onProgress).toHaveBeenCalledWith(50)

    progressFn({ progress: 1 })
    expect(onProgress).toHaveBeenCalledWith(100)
  })
})
