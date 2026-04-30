import axios from 'axios'
import { message } from 'antd'

const API_BASE_URL = '/api'

const TIME_OUT_CONFIG = {
  chat: 120000,
  upload: 300000,
  default: 30000,
}

const getTimeout = (url) => {
  if (url.includes('/chat')) return TIME_OUT_CONFIG.chat
  if (url.includes('/upload') || url.includes('/documents')) return TIME_OUT_CONFIG.upload
  return TIME_OUT_CONFIG.default
}

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: TIME_OUT_CONFIG.default,
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: false,
})

apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }

    config.timeout = getTimeout(config.url)

    if (config.method === 'get') {
      config.params = {
        ...config.params,
        _t: Date.now(),
      }
    }

    return config
  },
  (error) => {
    console.error('Request error:', error)
    return Promise.reject(error)
  }
)

apiClient.interceptors.response.use(
  (response) => {
    return response.data
  },
  (error) => {
    const errorInfo = {
      code: error.response?.status,
      message: error.response?.data?.message || error.message || '请求失败',
      details: error.response?.data?.errors,
    }

    if (error.response?.status === 401) {
      message.error('登录已过期，请重新登录')
      localStorage.removeItem('token')
      localStorage.removeItem('username')
      window.location.href = '/login'
      return Promise.reject(errorInfo)
    }

    if (error.response?.status === 403) {
      message.error('没有权限访问该资源')
      return Promise.reject(errorInfo)
    }

    if (error.response?.status === 429) {
      message.error('请求过于频繁，请稍后再试')
      return Promise.reject(errorInfo)
    }

    if (error.code === 'ECONNABORTED') {
      message.error('请求超时，请稍后再试')
      return Promise.reject(errorInfo)
    }

    if (error.response?.status === 400 && errorInfo.details) {
      const errorMessages = Object.values(errorInfo.details).join(', ')
      message.error(`参数错误: ${errorMessages}`)
    } else if (errorInfo.message && errorInfo.message !== '请求失败') {
      message.error(errorInfo.message)
    }

    return Promise.reject(errorInfo)
  }
)

export const request = async (method, url, data = null, config = {}) => {
  try {
    const response = await apiClient({
      method,
      url,
      data,
      ...config,
    })
    return response
  } catch (error) {
    throw error
  }
}

export default apiClient
