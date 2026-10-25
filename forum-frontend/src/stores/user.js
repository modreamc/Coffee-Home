import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '../api/auth'

function readUserInfo() {
  try {
    return JSON.parse(localStorage.getItem('userInfo') || 'null')
  } catch {
    // localStorage 里存过脏数据时不要让整个应用起不来
    localStorage.removeItem('userInfo')
    return null
  }
}

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(readUserInfo())

  const isLoggedIn = computed(() => !!token.value)
  const username = computed(() => userInfo.value?.username || '')
  const nickname = computed(() => userInfo.value?.nickname || '')
  const role = computed(() => userInfo.value?.role || '')
  const avatar = computed(() => userInfo.value?.avatar || '')

  function persist(res) {
    token.value = res.token
    userInfo.value = res.user
    localStorage.setItem('token', res.token)
    localStorage.setItem('userInfo', JSON.stringify(res.user))
  }

  async function login(loginForm) {
    persist(await authApi.login(loginForm))
  }

  async function register(registerForm) {
    persist(await authApi.register(registerForm))
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
  }

  async function fetchUserInfo() {
    if (!token.value) return
    try {
      const res = await authApi.me()
      userInfo.value = res
      localStorage.setItem('userInfo', JSON.stringify(res))
    } catch {
      logout()
    }
  }

  return { token, userInfo, isLoggedIn, username, nickname, role, avatar, login, register, logout, fetchUserInfo }
})
