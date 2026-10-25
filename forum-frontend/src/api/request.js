import axios from 'axios'
import { ElMessage } from 'element-plus'

// 注意：这里不再 import router。
// 旧写法 request.js → router → stores/user → api/auth → request.js 形成循环依赖，
// 且拦截器里调用尚未安装的 router.push 会在极端时序下抛 "Router is not installed"。
const request = axios.create({
  baseURL: '/api',
  timeout: 15000,
})

request.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

function clearSession() {
  localStorage.removeItem('token')
  localStorage.removeItem('userInfo')
}

request.interceptors.response.use(
  response => {
    const data = response.data
    if (data && data.code !== 200) {
      ElMessage.error(data.message || '请求失败')
      return Promise.reject(new Error(data.message || '请求失败'))
    }
    return data
  },
  error => {
    const status = error.response?.status
    const message = error.response?.data?.message
    // 元信息抓取这类“增强型”请求失败时不应该把用户踢回登录页
    if (error.config?.skipAuthRedirect) {
      return Promise.reject(error)
    }
    if (status === 401) {
      // 后端现在会正确返回 401（不再一律 403），过期后清理会话并回登录页
      clearSession()
      ElMessage.error(message || '登录已过期，请重新登录')
      if (!window.location.pathname.startsWith('/login')) {
        const redirect = encodeURIComponent(window.location.pathname + window.location.search)
        window.location.href = `/login?redirect=${redirect}`
      }
    } else if (status === 429) {
      ElMessage.error(message || '操作过于频繁，请稍后再试')
    } else if (status === 403) {
      ElMessage.error(message || '权限不足')
    } else if (error.code === 'ECONNABORTED') {
      ElMessage.error('请求超时，请稍后重试')
    } else {
      ElMessage.error(message || (error.response ? '请求失败' : '网络错误'))
    }
    return Promise.reject(error)
  }
)

export default request
