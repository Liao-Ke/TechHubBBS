<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { User, Lock, Message } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { useUserStore } from '@/stores/user'

const emit = defineEmits<{
  (e: 'register-success'): void
}>()

const router = useRouter()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)

interface RegisterFormData {
  username: string
  password: string
  confirmPassword: string
  email: string
}

const form = reactive<RegisterFormData>({
  username: '',
  password: '',
  confirmPassword: '',
  email: '',
})

const validateConfirmPassword = (_rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (!value) {
    callback(new Error('请再次输入密码'))
  } else if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules: FormRules<RegisterFormData> = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 50, message: '用户名长度需在3-50个字符之间', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 100, message: '密码长度需在6-100个字符之间', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, validator: validateConfirmPassword, trigger: 'blur' },
  ],
  email: [
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' },
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
    const email = form.email.trim() || undefined
    await userStore.register(form.username, form.password, email)
    emit('register-success')
  } catch (err: unknown) {
    const message = err instanceof Error ? err.message : '注册失败，请稍后重试'
    ElMessage.error(message)
  } finally {
    loading.value = false
  }
}

function goToLogin() {
  router.push('/login')
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
        autocomplete="new-password"
        maxlength="100"
      />
    </el-form-item>

    <el-form-item prop="confirmPassword">
      <el-input
        v-model="form.confirmPassword"
        type="password"
        show-password
        placeholder="确认密码"
        :prefix-icon="Lock"
        autocomplete="new-password"
        maxlength="100"
      />
    </el-form-item>

    <el-form-item prop="email">
      <el-input
        v-model="form.email"
        placeholder="邮箱（选填）"
        :prefix-icon="Message"
        autocomplete="email"
        maxlength="100"
        clearable
      />
    </el-form-item>

    <el-form-item>
      <el-button
        type="primary"
        native-type="submit"
        :loading="loading"
        class="register-form__submit"
      >
        {{ loading ? '注册中...' : '注 册' }}
      </el-button>
    </el-form-item>
  </el-form>

  <div class="register-form__footer">
    <span class="register-form__footer-text">已有账号？</span>
    <el-link type="primary" :underline="false" @click="goToLogin">
      立即登录
    </el-link>
  </div>
</template>

<style lang="scss" scoped>
.register-form__submit {
  width: 100%;
  font-weight: 600;
  letter-spacing: 2px;
}

.register-form__footer {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--th-spacing-1);
  margin-top: var(--th-spacing-4);
}

.register-form__footer-text {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}
</style>
