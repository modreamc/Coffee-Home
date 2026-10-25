import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useThemeStore = defineStore('theme', () => {
  const theme = ref('light')

  function apply(t) {
    theme.value = t
    const root = document.documentElement
    if (t === 'dark') {
      root.classList.add('dark')
      root.setAttribute('data-theme', 'dark')
    } else {
      root.classList.remove('dark')
      root.setAttribute('data-theme', 'light')
    }
    localStorage.setItem('theme', t)
  }

  function init() {
    // 支持 ?theme=light / ?theme=dark 直接锁定主题（方便演示与截图）
    const param = new URLSearchParams(window.location.search).get('theme')
    const saved = (param === 'light' || param === 'dark') ? param : localStorage.getItem('theme')
    const prefersDark = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches
    apply(saved || (prefersDark ? 'dark' : 'light'))
  }

  function toggle() {
    apply(theme.value === 'dark' ? 'light' : 'dark')
  }

  return { theme, init, apply, toggle }
})
