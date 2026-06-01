<script setup lang="ts">
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import RegisterForm from '@/components/user/RegisterForm.vue'

const router = useRouter()

function handleRegisterSuccess() {
  ElMessage.success('注册成功，请登录')
  router.push('/login')
}
</script>

<template>
  <RegisterForm @register-success="handleRegisterSuccess" />
</template>
