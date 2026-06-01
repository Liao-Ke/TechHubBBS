<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import LoginForm from '@/components/user/LoginForm.vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

onMounted(() => {
  // Redirect if already logged in
  if (userStore.isLoggedIn) {
    router.replace('/')
  }
})

function handleLoginSuccess() {
  const redirect = route.query.redirect
  const target = typeof redirect === 'string' && redirect ? redirect : '/'
  router.replace(target)
}
</script>

<template>
  <LoginForm @login-success="handleLoginSuccess" />
</template>
