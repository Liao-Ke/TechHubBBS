<script setup lang="ts">
const emit = defineEmits<{
  insert: [syntax: string]
}>()

interface ToolbarButton {
  label: string
  syntax: string
  title: string
}

const buttons: ToolbarButton[] = [
  { label: 'B', syntax: '**text**', title: '粗体' },
  { label: 'I', syntax: '*text*', title: '斜体' },
  { label: 'S', syntax: '~~text~~', title: '删除线' },
  { label: 'H1', syntax: '# ', title: '一级标题' },
  { label: 'H2', syntax: '## ', title: '二级标题' },
  { label: 'H3', syntax: '### ', title: '三级标题' },
  { label: '🔗', syntax: '[text](url)', title: '链接' },
  { label: '🖼', syntax: '![alt](url)', title: '图片' },
  { label: '`', syntax: '`code`', title: '行内代码' },
  { label: '{ }', syntax: '```\n\n```', title: '代码块' },
  { label: '❝', syntax: '> ', title: '引用' },
  { label: '•', syntax: '- ', title: '无序列表' },
]

function handleInsert(syntax: string) {
  emit('insert', syntax)
}
</script>

<template>
  <div class="md-toolbar">
    <el-button
      v-for="btn in buttons"
      :key="btn.title"
      :title="btn.title"
      text
      size="small"
      class="md-toolbar__btn"
      @click="handleInsert(btn.syntax)"
    >
      {{ btn.label }}
    </el-button>
  </div>
</template>

<style lang="scss" scoped>
.md-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: var(--th-spacing-1, 4px);
  padding: var(--th-spacing-2, 8px);
  background: var(--el-fill-color-light);
  border: 1px solid var(--el-border-color);
  border-radius: var(--th-radius-md, 6px) var(--th-radius-md, 6px) 0 0;
  border-bottom: 1px solid var(--el-border-color);
}

.md-toolbar__btn {
  min-width: 28px;
  height: 28px;
  padding: 0 6px;
  font-family: var(--th-font-mono, monospace);
  font-size: 13px;
  font-weight: 500;
  color: var(--el-text-color-regular);

  &:hover {
    color: var(--el-color-primary);
    background: var(--el-color-primary-light-9);
  }
}
</style>
