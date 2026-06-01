<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { User, Lock } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { useUserStore } from '@/stores/user'

const emit = defineEmits<{
  (e: 'login-success'): void
}>()

const router = useRouter()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)

interface LoginFormData {
  username: string
  password: string
}

const form = reactive<LoginFormData>({
  username: '',
  password: '',
})

const rules: FormRules<LoginFormData> = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' },
  ],
}

async function handleSubmit() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  loading.value = true
  try {
    await userStore.login(form.username, form.password)
    emit('login-success')
  } catch (err: unknown) {
    const message = err instanceof Error ? err.message : '登录失败，请稍后重试'
    ElMessage.error(message)
  } finally {
    loading.value = false
  }
}

function goToRegister() {
  router.push('/register')
}
</script>

<template>
  <el-form
    ref="formRef"
    :model="form"
    :rules="rules"
    label-position="top"
    hide-required-asterisk
    size="large"
    @submit.prevent="handleSubmit"
  >
    <el-form-item prop="username">
      <el-input
        v-model="form.username"
        placeholder="用户名"
        :prefix-icon="User"
        autocomplete="username"
        maxlength="50"
        clearable
      />
    </el-form-item>

    <el-form-item prop="password">
      <el-input
        v-model="form.password"
        type="password"
        show-password
        placeholder="密码"
        :prefix-icon="Lock"
        autocomplete="current-password"
        maxlength="100"
      />
    </el-form-item>

    <el-form-item>
      <el-button
        type="primary"
        native-type="submit"
        :loading="loading"
        class="login-form__submit"
      >
        {{ loading ? '登录中...' : '登 录' }}
      </el-button>
    </el-form-item>
  </el-form>

  <div class="login-form__footer">
    <span class="login-form__footer-text">还没有账号？</span>
    <el-link type="primary" :underline="false" @click="goToRegister">
      立即注册
    </el-link>
  </div>
</template>

<style lang="scss" scoped>
.login-form__submit {
  width: 100%;
  font-weight: 600;
  letter-spacing: 2px;
}

.login-form__footer {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--th-spacing-1);
  margin-top: var(--th-spacing-4);
}

.login-form__footer-text {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}
</style>
