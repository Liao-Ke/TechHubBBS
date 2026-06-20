<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { userApi } from '@/api/modules/user'
import { api } from '@/api'
import { ElMessage } from 'element-plus'
import UserAvatar from '@/components/common/UserAvatar.vue'
import ImageUpload from '@/components/common/ImageUpload.vue'
import LoadingSkeleton from '@/components/common/LoadingSkeleton.vue'
import { ArrowDown, ArrowUp, Lock } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

// ── Loading state ──
const pageLoading = ref(true)

// ── Auth guard ──
if (!userStore.isLoggedIn) {
  router.replace('/login?redirect=/settings')
}

// ── Avatar ──
const avatarUrl = ref(userStore.userInfo?.avatarUrl || '')

function onUploadSuccess(url: string) {
  avatarUrl.value = url
}

// ── Form ──
const formRef = ref<FormInstance>()
const saving = ref(false)

const form = reactive({
  bio: userStore.userInfo?.bio || '',
})

const rules: FormRules = {
  bio: [
    { max: 500, message: '简介不能超过500字', trigger: 'blur' },
  ],
}

// ── Password change (expandable) ──
const showPasswordSection = ref(false)
const passwordFormRef = ref<FormInstance>()

const passwordForm = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const passwordRules: FormRules = {
  currentPassword: [
    { required: true, message: '请输入当前密码', trigger: 'blur' },
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('两次密码输入不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
}


// ── Save ──
async function handleSave() {
  if (saving.value) return

  // If password section is open, validate it too
  if (showPasswordSection.value) {
    const pwValid = await passwordFormRef.value?.validate().catch(() => false)
    if (!pwValid) return
  }

  const profileValid = await formRef.value?.validate().catch(() => false)
  if (!profileValid) return

  saving.value = true
  try {
    await userApi.updateMe({
      avatarUrl: avatarUrl.value || undefined,
      bio: form.bio || undefined,
    })

    // Handle password change if filled
    if (showPasswordSection.value && passwordForm.currentPassword) {
      await changePassword(passwordForm.currentPassword, passwordForm.newPassword)
    }

    // Refresh user info in store
    await userStore.fetchUserInfo()

    // Reset password fields
    passwordForm.currentPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
    showPasswordSection.value = false

    ElMessage.success('更新成功')
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : "保存失败，请稍后重试")
  } finally {
    saving.value = false
  }
}

async function changePassword(oldPw: string, newPw: string) {
  await api('/users/me/password', {
    method: 'PATCH',
    body: { oldPassword: oldPw, newPassword: newPw },
  })
}

// ── Lifecycle ──
onMounted(async () => {
  if (!userStore.userInfo) {
    await userStore.fetchUserInfo()
  }
  if (userStore.userInfo) {
    form.bio = userStore.userInfo.bio || ''
    avatarUrl.value = userStore.userInfo.avatarUrl || ''
  }
  pageLoading.value = false
})
</script>

<template>
  <div class="settings-page">
    <h1 class="settings-page__title">编辑资料</h1>

    <!-- Guard: not logged in -->
    <div v-if="!userStore.isLoggedIn" class="settings-page__guard">
      <el-alert type="warning" title="请先登录" :closable="false" show-icon />
    </div>

    <!-- Loading skeleton -->
    <LoadingSkeleton v-else-if="pageLoading" variant="detail" />

    <!-- Form content -->
    <template v-else>
      <!-- ======== Avatar ======== -->
      <section class="settings-page__section">
        <h2 class="settings-page__section-title">头像</h2>
        <div class="settings-page__avatar-row">
          <UserAvatar :src="avatarUrl" :size="80" />
          <ImageUpload
            object-type="user_avatar"
            class="settings-page__avatar-upload"
            @upload-success="onUploadSuccess"
          />
        </div>
      </section>

      <!-- ======== Bio ======== -->
      <section class="settings-page__section">
        <h2 class="settings-page__section-title">简介</h2>
        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
        >
          <el-form-item prop="bio">
            <el-input
              v-model="form.bio"
              type="textarea"
              :rows="4"
              maxlength="500"
              show-word-limit
              placeholder="介绍一下自己..."
              class="settings-page__bio-input"
            />
          </el-form-item>
        </el-form>
      </section>

      <!-- ======== Password Change ======== -->
      <section class="settings-page__section">
        <button
          type="button"
          class="settings-page__section-toggle"
          @click="showPasswordSection = !showPasswordSection"
        >
          <el-icon :size="16" class="settings-page__section-toggle-icon">
            <Lock />
          </el-icon>
          <span>修改密码</span>
          <el-icon :size="14" class="settings-page__section-toggle-arrow">
            <ArrowDown v-if="!showPasswordSection" />
            <ArrowUp v-else />
          </el-icon>
        </button>

        <el-collapse-transition>
          <div v-if="showPasswordSection" class="settings-page__password-section">
            <el-form
              ref="passwordFormRef"
              :model="passwordForm"
              :rules="passwordRules"
              label-position="top"
            >
              <el-form-item label="当前密码" prop="currentPassword">
                <el-input
                  v-model="passwordForm.currentPassword"
                  type="password"
                  placeholder="输入当前密码"
                  show-password
                />
              </el-form-item>
              <el-form-item label="新密码" prop="newPassword">
                <el-input
                  v-model="passwordForm.newPassword"
                  type="password"
                  placeholder="输入新密码（至少6位）"
                  show-password
                />
              </el-form-item>
              <el-form-item label="确认新密码" prop="confirmPassword">
                <el-input
                  v-model="passwordForm.confirmPassword"
                  type="password"
                  placeholder="再次输入新密码"
                  show-password
                />
              </el-form-item>
            </el-form>
          </div>
        </el-collapse-transition>
      </section>

      <!-- ======== Save ======== -->
      <section class="settings-page__actions">
        <el-button
          type="primary"
          size="large"
          :loading="saving"
          @click="handleSave"
        >
          {{ saving ? '保存中...' : '保存修改' }}
        </el-button>
      </section>
    </template>
  </div>
</template>

<style lang="scss" scoped>
.settings-page {
  max-width: var(--th-content-max-width);
  margin: 0 auto;
  padding-top: var(--th-spacing-6);
  padding-bottom: var(--th-spacing-12);

  // ── Title ──
  &__title {
    font-size: 24px;
    font-weight: 700;
    color: var(--el-text-color-primary);
    margin: 0 0 var(--th-spacing-8);
    line-height: 1.3;
  }

  // ── Guard ──
  &__guard {
    padding: var(--th-spacing-8) 0;
  }

  // ── Sections ──
  &__section {
    margin-bottom: var(--th-spacing-8);

    &-title {
      font-size: 14px;
      font-weight: 600;
      color: var(--el-text-color-regular);
      margin: 0 0 var(--th-spacing-4);
      line-height: 1.4;
    }
  }

  // ── Section toggle (password) ──
  &__section-toggle {
    display: inline-flex;
    align-items: center;
    gap: var(--th-spacing-2);
    padding: 0;
    border: none;
    background: none;
    font-size: 14px;
    font-weight: 600;
    color: var(--el-text-color-regular);
    cursor: pointer;
    line-height: 1.4;
    transition: color var(--th-transition-fast);

    &:hover {
      color: var(--th-brand-primary);
    }

    &-icon {
      color: var(--el-text-color-secondary);
    }

    &-arrow {
      color: var(--el-text-color-placeholder);
    }
  }

  &__password-section {
    margin-top: var(--th-spacing-4);
    padding: var(--th-spacing-4);
    background: var(--el-fill-color-light);
    border-radius: var(--th-radius-lg);
  }

  // ── Avatar row ──
  &__avatar-row {
    display: flex;
    align-items: flex-start;
    gap: var(--th-spacing-6);
  }

  &__avatar-upload {
    flex: 1;
    max-width: 320px;
  }

  // ── Bio input ──
  &__bio-input {
    :deep(textarea) {
      resize: vertical;
    }
  }

  // ── Actions ──
  &__actions {
    padding-top: var(--th-spacing-4);
    border-top: 1px solid var(--el-border-color-light);
  }
}
</style>
