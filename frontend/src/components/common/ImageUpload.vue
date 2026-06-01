<script setup lang="ts">
import { ref } from 'vue'
import { UploadFilled, PictureFilled, Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useFileUpload } from '@/composables/useFileUpload'
import { fileApi } from '@/api/modules/file'
import type { UploadRequestOptions, UploadFile, UploadProgressEvent } from 'element-plus'

const props = defineProps<{
  objectType: 'user_avatar' | 'post_image'
}>()

const emit = defineEmits<{
  'upload-success': [url: string]
}>()

const { validateFile, progress, uploading } = useFileUpload()

const previewUrl = ref('')
const uploadError = ref(false)
const fileList = ref<UploadFile[]>([])

function beforeUpload(file: File): boolean {
  const error = validateFile(file)
  if (error) {
    ElMessage.error(error)
    return false
  }
  uploadError.value = false
  return true
}

async function httpRequest(options: UploadRequestOptions): Promise<void> {
  try {
    const res = await fileApi.upload(options.file, props.objectType, (percent: number) => {
      progress.value = percent
      if (options.onProgress) {
        options.onProgress({ percent } as UploadProgressEvent)
      }
    })

    if (res && res.code === 200 && res.data) {
      const url = res.data.url
      previewUrl.value = url
      emit('upload-success', url)
      if (options.onSuccess) {
        options.onSuccess(res.data)
      }
    } else {
      throw new Error('上传失败')
    }
  } catch {
    uploadError.value = true
  }
}

function handleRetry() {
  uploadError.value = false
  previewUrl.value = ''
  progress.value = 0
  fileList.value = []
}

function handleReset() {
  previewUrl.value = ''
  uploadError.value = false
  progress.value = 0
  fileList.value = []
}
</script>

<template>
  <div class="image-upload">
    <el-upload
      v-model:file-list="fileList"
      :auto-upload="true"
      :show-file-list="false"
      :before-upload="beforeUpload"
      :http-request="httpRequest"
      drag
      class="image-upload__uploader"
      :class="{
        'is-error': uploadError,
        'is-done': previewUrl && !uploadError,
      }"
    >
      <!-- Uploaded preview -->
      <template v-if="previewUrl && !uploadError">
        <el-image
          :src="previewUrl"
          fit="cover"
          class="image-upload__preview"
        />
        <div class="image-upload__overlay">
          <span class="image-upload__overlay-text">重新上传</span>
        </div>
      </template>

      <!-- Error state -->
      <template v-else-if="uploadError">
        <el-icon :size="32" class="image-upload__error-icon">
          <PictureFilled />
        </el-icon>
        <span class="image-upload__error-text">上传失败，请重试</span>
      </template>

      <!-- Uploading state -->
      <template v-else-if="uploading">
        <el-progress
          type="circle"
          :percentage="progress"
          :width="80"
          class="image-upload__progress"
        />
      </template>

      <!-- Idle state (default) -->
      <template v-else>
        <el-icon :size="28" class="image-upload__icon">
          <Plus />
        </el-icon>
        <div class="image-upload__text">
          <span class="image-upload__text-main">点击或拖拽上传</span>
          <span class="image-upload__text-hint">支持 JPG/PNG/GIF/WebP，不超过 5MB</span>
        </div>
      </template>
    </el-upload>

    <!-- Retry button (shown below on error) -->
    <div v-if="uploadError" class="image-upload__actions">
      <el-button size="small" @click="handleRetry">重试</el-button>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.image-upload {
  &__uploader {
    width: 100%;

    :deep(.el-upload-dragger) {
      height: 120px;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      border: 2px dashed var(--el-border-color);
      border-radius: var(--th-radius-lg);
      background: var(--el-fill-color-light);
      transition: border-color var(--th-transition-normal),
                  background-color var(--th-transition-normal);

      &:hover {
        border-color: var(--el-color-primary);
        background: var(--el-fill-color);
      }
    }

    &.is-done :deep(.el-upload-dragger) {
      padding: 0;
      overflow: hidden;
      position: relative;
    }

    &.is-error :deep(.el-upload-dragger) {
      border-color: var(--el-color-danger);
      border-style: dashed;
    }
  }

  &__icon {
    color: var(--el-text-color-placeholder);
    margin-bottom: var(--th-spacing-2);
  }

  &__text {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 4px;

    &-main {
      font-size: 14px;
      color: var(--el-text-color-regular);
    }

    &-hint {
      font-size: 12px;
      color: var(--el-text-color-placeholder);
    }
  }

  &__preview {
    width: 100%;
    height: 100%;
    position: absolute;
    inset: 0;
  }

  &__overlay {
    position: absolute;
    inset: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    background: rgba(0, 0, 0, 0.5);
    opacity: 0;
    transition: opacity var(--th-transition-normal);
    z-index: 1;

    &:hover {
      opacity: 1;
    }

    &-text {
      color: #fff;
      font-size: 14px;
    }
  }

  &__error-icon {
    color: var(--el-color-danger);
    margin-bottom: var(--th-spacing-2);
  }

  &__error-text {
    font-size: 13px;
    color: var(--el-color-danger);
  }

  &__progress {
    :deep(.el-progress-circle__track) {
      stroke: var(--el-border-color);
    }
  }

  &__actions {
    margin-top: var(--th-spacing-3);
    display: flex;
    justify-content: center;
    gap: var(--th-spacing-2);
  }
}
</style>
