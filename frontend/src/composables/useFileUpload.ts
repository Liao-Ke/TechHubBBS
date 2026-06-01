import { ref } from 'vue'
import { fileApi } from '@/api/modules/file'
import type { FileUploadResult } from '@/api/types/file'

/**
 * Allowed upload context types matching backend FileController validation.
 */
export type ObjectType = 'user_avatar' | 'post_image'

/** Allowed MIME types */
const ALLOWED_TYPES = ['image/jpeg', 'image/png', 'image/gif', 'image/webp']
/** Allowed file extensions (fallback validation) */
const ALLOWED_EXTENSIONS = ['.jpg', '.jpeg', '.png', '.gif', '.webp']
/** Maximum file size: 5MB */
const MAX_SIZE = 5 * 1024 * 1024

/**
 * Composable for file upload with client-side validation and progress tracking.
 *
 * Validation:
 * - Rejects files larger than 5MB
 * - Rejects non-image types (MIME check + extension fallback)
 *
 * Progress: passes a callback to fileApi.upload which updates progress ref.
 */
export function useFileUpload() {
  const uploading = ref(false)
  const progress = ref(0)
  const error = ref<string | null>(null)
  const result = ref<FileUploadResult | null>(null)

  /**
   * Client-side validation.
   * Returns an error message string or null if valid.
   */
  function validateFile(file: File): string | null {
    if (file.size > MAX_SIZE) {
      return '文件大小不能超过 5MB'
    }
    if (!ALLOWED_TYPES.includes(file.type)) {
      return '仅支持 JPG/PNG/GIF/WebP 格式的图片'
    }
    // Fallback: check file extension
    const ext = '.' + file.name.split('.').pop()?.toLowerCase()
    if (!ALLOWED_EXTENSIONS.includes(ext)) {
      return '仅支持 JPG/PNG/GIF/WebP 格式的图片'
    }
    return null
  }

  /**
   * Upload a file with progress tracking.
   * Returns FileUploadResult on success, null on failure.
   */
  async function upload(
    file: File,
    objectType: ObjectType,
  ): Promise<FileUploadResult | null> {
    const validationError = validateFile(file)
    if (validationError) {
      error.value = validationError
      return null
    }

    uploading.value = true
    progress.value = 0
    error.value = null
    result.value = null

    try {
      const res = await fileApi.upload(file, objectType, (percent: number) => {
        progress.value = percent
      })
      if (res.data) {
        result.value = res.data
        return res.data
      }
      return null
    } catch (e: unknown) {
      const message =
        e instanceof Error ? e.message : '上传失败，请重试'
      error.value = message
      return null
    } finally {
      uploading.value = false
    }
  }

  /** Reset all state to initial values */
  function reset(): void {
    uploading.value = false
    progress.value = 0
    error.value = null
    result.value = null
  }

  return {
    uploading,
    progress,
    error,
    result,
    upload,
    validateFile,
    reset,
  }
}
