import axios from 'axios'
import { message } from 'antd'

const API_BASE_URL = 'http://localhost:8080/api'

const authApi = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
})

authApi.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

authApi.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response) {
      const { status, data } = error.response
      if (status === 401) {
        message.error('登录已过期，请重新登录')
        localStorage.removeItem('token')
        localStorage.removeItem('username')
        window.location.href = '/login'
      } else if (status === 400) {
        message.error(data.message || '请求失败')
      } else if (status === 500) {
        message.error('服务器错误，请稍后重试')
      }
    }
    return Promise.reject(error)
  }
)

export const authService = {
  register: async (userData) => {
    try {
      const response = await authApi.post('/auth/register', {
        username: userData.username,
        password: userData.password,
        email: userData.email,
      })
      if (response.data.code === 200) {
        const { token, username } = response.data.data
        localStorage.setItem('token', token)
        localStorage.setItem('username', username)
        message.success('注册成功！')
        return response.data
      } else {
        message.error(response.data.message || '注册失败')
        throw new Error(response.data.message)
      }
    } catch (error) {
      console.error('注册错误:', error)
      throw error
    }
  },

  login: async (credentials) => {
    try {
      const response = await authApi.post('/auth/login', {
        username: credentials.username,
        password: credentials.password,
      })
      if (response.data.code === 200) {
        const { token, username } = response.data.data
        localStorage.setItem('token', token)
        localStorage.setItem('username', username)
        message.success('登录成功！')
        return response.data
      } else {
        message.error(response.data.message || '登录失败')
        throw new Error(response.data.message)
      }
    } catch (error) {
      console.error('登录错误:', error)
      throw error
    }
  },

  logout: () => {
    localStorage.removeItem('token')
    localStorage.removeItem('username')
    message.success('已退出登录')
  },

  getCurrentUser: () => {
    return {
      username: localStorage.getItem('username'),
      token: localStorage.getItem('token'),
    }
  },

  isAuthenticated: () => {
    return !!localStorage.getItem('token')
  },
}

export default authService
