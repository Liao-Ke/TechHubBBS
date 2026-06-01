/**
 * File upload API module
 */
import { api } from '@/api'
import type { R, FileUploadResult } from '@/api/types'

export const fileApi = {
  /**
   * Upload a file with optional progress callback.
   *
   * @param file        - File to upload
   * @param objectType  - Upload context type ('user_avatar' | 'post_image')
   * @param onProgress  - Callback receiving upload percentage (0-100)
   */
  upload: (
    file: File,
    objectType: string,
    onProgress?: (percent: number) => void,
  ) => {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('objectType', objectType)

    return api<R<FileUploadResult>>('/files/upload', {
      method: 'POST',
      body: formData,
      onUploadProgress: onProgress
        ? ({ progress }: { progress?: number }) => {
            if (progress) onProgress(Math.round(progress * 100))
          }
        : undefined,
    })
  },
}
