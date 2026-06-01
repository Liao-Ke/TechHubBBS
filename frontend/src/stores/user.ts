import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '@/api/modules/auth'
import { userApi } from '@/api/modules/user'
import { getToken, setToken, removeToken } from '@/utils/token'
import type { UserProfileVO, LoginRequest, RegisterRequest } from '@/api/types'

export const useUserStore = defineStore('user', () => {
  const token = ref<string | null>(getToken())
  const userInfo = ref<UserProfileVO | null>(null)

  const isLoggedIn = computed(() => !!token.value)
  const role = computed(() => userInfo.value?.role || '')
  const isAdmin = computed(() => role.value === 'ADMIN')
  const isModerator = computed(() => role.value === 'ADMIN' || role.value === 'MODERATOR')

  async function login(username: string, password: string) {
    const res = await authApi.login({ username, password })
    if (res.data) {
      token.value = res.data.token
      setToken(res.data.token)
      // Fetch user info after login
      await fetchUserInfo()
    }
  }

  async function register(username: string, password: string, email?: string) {
    const data: RegisterRequest = { username, password }
    if (email) data.email = email
    await authApi.register(data)
  }

  async function fetchUserInfo() {
    if (!isLoggedIn.value) return
    try {
      const res = await userApi.getMe()
      if (res.data) {
        userInfo.value = res.data
      }
    } catch {
      // If token is invalid, logout
      logout()
    }
  }

  function logout() {
    token.value = null
    userInfo.value = null
    removeToken()
  }

  return {
    token, userInfo,
    isLoggedIn, role, isAdmin, isModerator,
    login, register, fetchUserInfo, logout,
  }
})
