import axios from 'axios'
import { ElMessage } from 'element-plus'

const api = axios.create({
  baseURL: '/api',
  timeout: 120000
})

// 请求拦截器 - 添加 JWT Token
api.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 响应拦截器
let isRedirecting = false
api.interceptors.response.use(
  res => res,
  err => {
    if (err.response?.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      if (!isRedirecting && window.location.pathname !== '/login') {
        isRedirecting = true
        ElMessage.error('登录已过期，请重新登录')
        window.location.href = '/login'
      }
    } else if (err.response?.status === 403) {
      ElMessage.warning('当前角色无权限执行此操作')
    } else {
      ElMessage.error(err.response?.data?.error || '请求失败')
    }
    return Promise.reject(err)
  }
)

export default api
