<script setup lang="ts">
defineProps<{
  variant: 'post-list' | 'detail' | 'table'
}>()
</script>

<template>
  <!-- Post list variant: 5 PostCard skeletons with avatar + 3 paragraph lines -->
  <div v-if="variant === 'post-list'" class="skeleton-post-list">
    <div
      v-for="i in 5"
      :key="i"
      class="skeleton-post-list__item"
    >
      <div class="skeleton-post-list__card">
        <el-skeleton animated class="skeleton-post-list__skeleton">
          <template #template>
            <div class="skeleton-post-list__row">
              <el-skeleton-item variant="circle" class="skeleton-post-list__avatar" />
              <div class="skeleton-post-list__lines">
                <el-skeleton-item variant="text" />
                <el-skeleton-item variant="text" style="width: 60%" />
                <el-skeleton-item variant="text" style="width: 40%" />
              </div>
            </div>
          </template>
        </el-skeleton>
      </div>
    </div>
  </div>

  <!-- Detail variant: title bar + 3 content paragraphs + stats row -->
  <div v-else-if="variant === 'detail'" class="skeleton-detail">
    <el-skeleton animated>
      <template #template>
        <!-- Title bar -->
        <el-skeleton-item variant="text" class="skeleton-detail__title" />
        <!-- Content paragraphs -->
        <el-skeleton-item variant="text" class="skeleton-detail__para" />
        <el-skeleton-item variant="text" class="skeleton-detail__para" style="width: 90%" />
        <el-skeleton-item variant="text" class="skeleton-detail__para" style="width: 40%" />
        <!-- Stats row -->
        <div class="skeleton-detail__stats">
          <el-skeleton-item variant="circle" class="skeleton-detail__stat-circle" />
          <el-skeleton-item variant="circle" class="skeleton-detail__stat-circle" />
          <el-skeleton-item variant="circle" class="skeleton-detail__stat-circle" />
        </div>
      </template>
    </el-skeleton>
  </div>

  <!-- Table variant: el-skeleton with 8 rows -->
  <div v-else-if="variant === 'table'" class="skeleton-table">
    <el-skeleton :rows="8" animated />
  </div>
</template>

<style lang="scss" scoped>
/* ======== Post List Skeleton ======== */
.skeleton-post-list {
  display: flex;
  flex-direction: column;

  &__item {
    padding: var(--th-spacing-4) 0;

    &:not(:last-child) {
      border-bottom: 1px solid var(--el-border-color-light);
    }
  }

  &__card {
    width: 100%;
  }

  &__skeleton {
    width: 100%;
  }

  &__row {
    display: flex;
    align-items: flex-start;
    gap: var(--th-spacing-3);
  }

  &__avatar {
    width: 40px;
    height: 40px;
    flex-shrink: 0;
  }

  &__lines {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: var(--th-spacing-2);
  }
}

/* ======== Detail Skeleton ======== */
.skeleton-detail {
  padding: var(--th-spacing-6) 0;

  &__title {
    width: 60%;
    height: 28px;
    margin-bottom: var(--th-spacing-6);
  }

  &__para {
    height: 16px;
    margin-bottom: var(--th-spacing-3);
  }

  &__stats {
    display: flex;
    align-items: center;
    gap: var(--th-spacing-3);
    margin-top: var(--th-spacing-6);
  }

  &__stat-circle {
    width: 32px;
    height: 32px;
  }
}

/* ======== Table Skeleton ======== */
.skeleton-table {
  padding: var(--th-spacing-4) 0;
}
</style>
