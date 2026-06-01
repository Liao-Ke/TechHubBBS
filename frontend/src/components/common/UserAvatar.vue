<script setup lang="ts">
import { ref, watch } from 'vue'
import { UserFilled } from '@element-plus/icons-vue'

const props = withDefaults(defineProps<{
  src?: string
  size?: number
}>(), {
  size: 40,
})

const validSrc = ref(props.src)

watch(() => props.src, (newSrc) => {
  validSrc.value = newSrc
})

function handleError() {
  validSrc.value = ''
}
</script>

<template>
  <el-avatar
    :size="size"
    :src="validSrc"
    class="user-avatar"
    @error="handleError"
  >
    <el-icon v-if="!validSrc" class="user-avatar__fallback">
      <UserFilled />
    </el-icon>
  </el-avatar>
</template>

<style lang="scss" scoped>
.user-avatar {
  flex-shrink: 0;

  &__fallback {
    font-size: calc(var(--th-spacing-5) * 1px);
    color: var(--el-text-color-placeholder);
  }
}
</style>
