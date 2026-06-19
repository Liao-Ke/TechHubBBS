<script setup lang="ts">
import { ref, watch } from 'vue'
import Toolbar from './Toolbar.vue'
import MdViewer from './MdViewer.vue'

const props = defineProps<{
  modelValue: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const localContent = ref(props.modelValue)

watch(() => props.modelValue, (val) => {
  localContent.value = val
})

function handleInput(value: string) {
  emit('update:modelValue', value)
}

function handleInsert(syntax: string) {
  const textarea = document.querySelector('.md-editor__textarea textarea') as HTMLTextAreaElement | null
  if (textarea) {
    const start = textarea.selectionStart
    const end = textarea.selectionEnd
    const before = localContent.value.substring(0, start)
    const after = localContent.value.substring(end)

    // For multi-line syntax templates (code block), place cursor in the middle
    if (syntax.includes('\n')) {
      const lines = syntax.split('\n')
      const opening = lines[0] + '\n'
      const closing = '\n' + lines[lines.length - 1]
      localContent.value = before + opening + closing + after
      emit('update:modelValue', localContent.value)
      // Set cursor position between opening and closing
      requestAnimationFrame(() => {
        textarea.selectionStart = textarea.selectionEnd = before.length + opening.length
        textarea.focus()
      })
    } else {
      localContent.value = before + syntax + after
      emit('update:modelValue', localContent.value)
      // Set cursor position after the inserted syntax
      requestAnimationFrame(() => {
        const newPos = before.length + syntax.length
        textarea.selectionStart = textarea.selectionEnd = newPos
        textarea.focus()
      })
    }
  } else {
    // Fallback: append to end
    localContent.value += syntax
    emit('update:modelValue', localContent.value)
  }
}
</script>

<template>
  <div class="md-editor">
    <Toolbar @insert="handleInsert" />
    <div class="md-editor__panes">
      <div class="md-editor__edit">
        <el-input
          type="textarea"
          :model-value="modelValue"
          @update:model-value="handleInput"
          placeholder="在此输入 Markdown 内容..."
          :autosize="{ minRows: 12 }"
          class="md-editor__textarea"
        />
      </div>
      <div class="md-editor__preview">
        <MdViewer :content="modelValue" />
      </div>
    </div>
    <div class="md-editor__footer">
      <span class="md-editor__count">{{ modelValue.length }} 字</span>
    </div>
  </div>
</template>

<style lang="scss" scoped>
$bp-md: 768px;

.md-editor {
  width: 100%;
  border: 1px solid var(--el-border-color);
  border-radius: var(--th-radius-md, 6px);
  overflow: hidden;
}

.md-editor__panes {
  display: flex;
  overflow: hidden;
  min-height: 300px;

  @media (min-width: $bp-md) {
    min-height: 400px;
  }

  @media (max-width: #{$bp-md - 1}) {
    flex-direction: column;
    min-height: auto;
  }
}

.md-editor__edit {
  flex: 1;
  min-width: 0;
  overflow-x: auto;
  border-right: 1px solid var(--el-border-color);

  @media (max-width: #{$bp-md - 1}) {
    border-right: none;
    border-bottom: 1px solid var(--el-border-color);
  }
}

.md-editor__textarea {
  height: 100%;

  :deep(.el-textarea__inner) {
    min-height: 300px;
    height: 100%;
    border: none;
    border-radius: 0;
    font-family: var(--th-font-mono, monospace);
    font-size: 14px;
    line-height: 1.6;
    resize: none;
    box-shadow: none;
    padding: var(--th-spacing-4, 16px);
    background: var(--el-bg-color);
    color: var(--el-text-color-primary);
    word-break: break-all;
    overflow-wrap: break-word;
    overflow-x: hidden;

    &:focus {
      box-shadow: none;
    }

    @media (min-width: $bp-md) {
      min-height: 400px;
    }
  }
}

.md-editor__preview {
  flex: 1;
  min-width: 0;
  padding: var(--th-spacing-4, 16px);
  overflow: auto;
  background: var(--el-bg-color-overlay);

  @media (min-width: $bp-md) {
    padding: var(--th-spacing-4, 16px) var(--th-spacing-6, 24px);
  }

  @media (max-width: #{$bp-md - 1}) {
    max-height: 50vh;
  }
}

.md-editor__footer {
  display: flex;
  justify-content: flex-end;
  padding: var(--th-spacing-2, 8px) var(--th-spacing-4, 16px);
  background: var(--el-fill-color-light);
  border-top: 1px solid var(--el-border-color);
}

.md-editor__count {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  font-variant-numeric: tabular-nums;
}
</style>
