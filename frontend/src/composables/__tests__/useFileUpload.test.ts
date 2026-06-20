import { describe, it, expect, vi, beforeEach } from 'vitest'
import { useFileUpload } from '@/composables/useFileUpload'

vi.mock('@/api/modules/file', () => ({
  fileApi: {
    upload: vi.fn<(...args: unknown[]) => unknown>(),
  },
}))

import { fileApi } from '@/api/modules/file'

function createMockFile(name: string, size: number, type: string): File {
  const blob = new Blob(['x'.repeat(size)], { type })
  return new File([blob], name, { type })
}

describe('useFileUpload', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('validateFile', () => {
    it('returns null for valid jpg file (5MB or less)', () => {
      const { validateFile } = useFileUpload()
      const file = createMockFile('photo.jpg', 1024 * 1024, 'image/jpeg')
      expect(validateFile(file)).toBeNull()
    })

    it('returns null for valid png file', () => {
      const { validateFile } = useFileUpload()
      const file = createMockFile('icon.png', 512 * 1024, 'image/png')
      expect(validateFile(file)).toBeNull()
    })

    it('returns null for valid gif file', () => {
      const { validateFile } = useFileUpload()
      const file = createMockFile('anim.gif', 256 * 1024, 'image/gif')
      expect(validateFile(file)).toBeNull()
    })

    it('returns null for valid webp file', () => {
      const { validateFile } = useFileUpload()
      const file = createMockFile('img.webp', 1024 * 1024, 'image/webp')
      expect(validateFile(file)).toBeNull()
    })

    it('returns null for exactly 5MB file', () => {
      const { validateFile } = useFileUpload()
      const file = createMockFile('exact.jpg', 5 * 1024 * 1024, 'image/jpeg')
      expect(validateFile(file)).toBeNull()
    })

    it('returns error for PDF file', () => {
      const { validateFile } = useFileUpload()
      const file = createMockFile('doc.pdf', 500 * 1024, 'application/pdf')
      const error = validateFile(file)
      expect(error).toBeTruthy()
      expect(error).toContain('仅支持')
    })

    it('returns error for SVG file', () => {
      const { validateFile } = useFileUpload()
      const file = createMockFile('logo.svg', 10 * 1024, 'image/svg+xml')
      const error = validateFile(file)
      expect(error).toBeTruthy()
    })

    it('returns error for > 5MB file', () => {
      const { validateFile } = useFileUpload()
      const file = createMockFile('big.jpg', 6 * 1024 * 1024, 'image/jpeg')
      const error = validateFile(file)
      expect(error).toBeTruthy()
      expect(error).toContain('5MB')
    })

    it('returns error for file with invalid extension (fallback)', () => {
      const { validateFile } = useFileUpload()
      const file = createMockFile('video.mp4', 1024 * 1024, 'image/jpeg')
      const error = validateFile(file)
      expect(error).toBeTruthy()
      expect(error).toContain('仅支持')
    })

    it('accepts jpeg extension', () => {
      const { validateFile } = useFileUpload()
      const file = createMockFile('photo.jpeg', 1024 * 1024, 'image/jpeg')
      expect(validateFile(file)).toBeNull()
    })

    it('accepts upper-case extension (lowercased in check)', () => {
      const { validateFile } = useFileUpload()
      const file = createMockFile('PHOTO.JPG', 1024 * 1024, 'image/jpeg')
      expect(validateFile(file)).toBeNull()
    })
  })

  describe('upload', () => {
    it('returns FileUploadResult on successful upload', async () => {
      const mockData = { url: 'https://example.com/img.png', filename: 'test.png', size: 1024 }
      vi.mocked(fileApi.upload).mockImplementation(
        (_file, _objectType, onProgress) => {
          if (onProgress) onProgress(50)
          if (onProgress) onProgress(100)
          return Promise.resolve({ data: mockData }) as any
        },
      )

      const { upload, result, progress, uploading } = useFileUpload()
      const file = createMockFile('test.png', 1024, 'image/png')

      const uploadResult = await upload(file, 'post_image')

      expect(uploadResult).toStrictEqual(mockData)
      expect(result.value).toStrictEqual(mockData)
      expect(progress.value).toBe(100)
      expect(uploading.value).toBe(false)
    })

    it('updates progress during upload', async () => {
      vi.mocked(fileApi.upload).mockImplementation(
        (_file, _objectType, onProgress) => {
          if (onProgress) {
            onProgress(25)
            onProgress(50)
            onProgress(75)
            onProgress(100)
          }
          return Promise.resolve({ data: { url: 'url', filename: 'f', size: 0 } }) as any
        },
      )

      const { upload, progress } = useFileUpload()
      const file = createMockFile('img.jpg', 1024, 'image/jpeg')

      await upload(file, 'user_avatar')

      expect(progress.value).toBe(100)
    })

    it('returns null and sets error on invalid file (PDF)', async () => {
      const { upload, error, uploading } = useFileUpload()
      const file = createMockFile('doc.pdf', 500 * 1024, 'application/pdf')

      const result = await upload(file, 'post_image')

      expect(result).toBeNull()
      expect(error.value).toContain('仅支持')
      expect(uploading.value).toBe(false)
      expect(fileApi.upload).not.toHaveBeenCalled()
    })

    it('rejects > 5MB file before upload', async () => {
      const { upload, error } = useFileUpload()
      const file = createMockFile('big.jpg', 6 * 1024 * 1024, 'image/jpeg')

      const result = await upload(file, 'post_image')

      expect(result).toBeNull()
      expect(error.value).toContain('5MB')
      expect(fileApi.upload).not.toHaveBeenCalled()
    })

    it('sets error when fileApi.upload throws', async () => {
      vi.mocked(fileApi.upload).mockRejectedValue(new Error('Network error'))

      const { upload, error, uploading } = useFileUpload()
      const file = createMockFile('img.png', 1024, 'image/png')

      const result = await upload(file, 'post_image')

      expect(result).toBeNull()
      expect(error.value).toBe('Network error')
      expect(uploading.value).toBe(false)
    })

    it('handles non-Error throw in fileApi.upload', async () => {
      vi.mocked(fileApi.upload).mockRejectedValue('String exception')

      const { upload, error } = useFileUpload()
      const file = createMockFile('img.png', 1024, 'image/png')

      await upload(file, 'post_image')
      expect(error.value).toBe('上传失败，请重试')
    })

    it('sets uploading to true during upload', async () => {
      let resolveUpload: (value: unknown) => void
      const uploadPromise = new Promise((resolve) => {
        resolveUpload = resolve
      })
      vi.mocked(fileApi.upload).mockReturnValue(uploadPromise as Promise<any>)

      const { upload, uploading } = useFileUpload()
      const file = createMockFile('img.png', 1024, 'image/png')

      const resultPromise = upload(file, 'post_image')

      expect(uploading.value).toBe(true)

      resolveUpload!({ data: { url: 'done', filename: 'f', size: 0 } })
      await resultPromise

      expect(uploading.value).toBe(false)
    })

    it('passes objectType and progress callback to fileApi.upload', async () => {
      vi.mocked(fileApi.upload).mockResolvedValue({ data: { url: 'x', filename: 'x', size: 0 } } as any)

      const { upload } = useFileUpload()
      const file = createMockFile('avatar.png', 1024, 'image/png')

      await upload(file, 'user_avatar')

      expect(fileApi.upload).toHaveBeenCalledWith(
        file,
        'user_avatar',
        expect.any(Function),
      )
    })

    it('passes post_image as objectType', async () => {
      vi.mocked(fileApi.upload).mockResolvedValue({ data: { url: 'x', filename: 'x', size: 0 } } as any)

      const { upload } = useFileUpload()
      const file = createMockFile('img.jpg', 1024, 'image/jpeg')

      await upload(file, 'post_image')

      expect(fileApi.upload).toHaveBeenCalledWith(
        file,
        'post_image',
        expect.any(Function),
      )
    })
  })

  describe('reset', () => {
    it('clears all state', () => {
      const { uploading, progress, error, result, reset } = useFileUpload()

      uploading.value = true
      progress.value = 50
      error.value = 'Some error'
      result.value = { url: 'url', filename: 'f', size: 0 }

      reset()

      expect(uploading.value).toBe(false)
      expect(progress.value).toBe(0)
      expect(error.value).toBeNull()
      expect(result.value).toBeNull()
    })
  })
})
