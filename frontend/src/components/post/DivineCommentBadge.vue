<script setup lang="ts">
import { Medal } from '@element-plus/icons-vue'

withDefaults(defineProps<{
  isDivine?: boolean
}>(), {
  isDivine: false,
})
</script>

<template>
  <el-tag v-if="isDivine" type="warning" effect="dark" size="small" class="divine-badge">
    <el-icon :size="14"><Medal /></el-icon>
    <span>神评</span>
  </el-tag>
</template>

<style lang="scss" scoped>
.divine-badge {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  font-weight: 500;
}
</style>
