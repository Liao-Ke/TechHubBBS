<script setup lang="ts">
import { computed } from 'vue'
import { View, Lock, User, Hide } from '@element-plus/icons-vue'
import type { Component } from 'vue'

const props = defineProps<{
  modelValue: number
}>()

const emit = defineEmits<{
  'update:modelValue': [value: number]
}>()

interface VisibilityOption {
  value: number
  label: string
  icon: Component
  hint: string
}

const options: VisibilityOption[] = [
  { value: 0, label: '公开', icon: View, hint: '所有人可见' },
  { value: 1, label: '登录可见', icon: Lock, hint: '仅登录用户可见' },
  { value: 2, label: '粉丝可见', icon: User, hint: '仅粉丝可见' },
  { value: 3, label: '私密', icon: Hide, hint: '仅自己可见' },
]

const selectedHint = computed(
  () => options.find((o) => o.value === props.modelValue)?.hint ?? '',
)

function handleChange(val: number) {
  emit('update:modelValue', val)
}
</script>

<template>
  <div class="visibility-selector">
    <el-select
      :model-value="modelValue"
      :model-value-str="String(modelValue)"
      placeholder="选择可见范围"
      style="width: 180px"
      @update:model-value="handleChange"
    >
      <el-option
        v-for="opt in options"
        :key="opt.value"
        :label="opt.label"
        :value="opt.value"
      >
        <div class="visibility-selector__option">
          <el-icon :size="16"><component :is="opt.icon" /></el-icon>
          <span class="visibility-selector__option-label">{{ opt.label }}</span>
          <span class="visibility-selector__option-hint">{{ opt.hint }}</span>
        </div>
      </el-option>
    </el-select>
    <span class="visibility-selector__hint">{{ selectedHint }}</span>
  </div>
</template>

<style lang="scss" scoped>
.visibility-selector {
  display: flex;
  flex-direction: column;
  gap: var(--th-spacing-xs);

  &__option {
    display: flex;
    align-items: center;
    gap: var(--th-spacing-sm);
  }

  &__option-label {
    flex: 1;
  }

  &__option-hint {
    font-size: 12px;
    color: var(--el-text-color-placeholder);
  }

  &__hint {
    font-size: 13px;
    color: var(--el-text-color-secondary);
    padding-left: 2px;
  }
}
</style>
