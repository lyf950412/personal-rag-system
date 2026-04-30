import apiClient from './api'

export const chatApi = {
  getHistory: async (sessionId) => {
    return await apiClient.get(`/chat/history/${sessionId}`)
  },
  
  chat: async (sessionId, question, knowledgeBaseId) => {
    return await apiClient.post('/chat', {
      sessionId,
      question,
      knowledgeBaseId,
    })
  },
  
  chatStream: async (sessionId, question, knowledgeBaseId, signal) => {
    const params = new URLSearchParams({
      sessionId,
      question,
    })
    
    if (knowledgeBaseId) {
      params.append('knowledgeBaseId', knowledgeBaseId)
    }
    
    const token = localStorage.getItem('token')
    
    const headers = {
      'Accept': 'text/event-stream',
      'Cache-Control': 'no-cache',
    }
    
    if (token) {
      headers['Authorization'] = `Bearer ${token}`
    }
    
    const response = await fetch(`${apiClient.defaults.baseURL}/chat/stream?${params}`, {
      method: 'GET',
      headers: headers,
      credentials: 'include',
      signal: signal,
    })
    
    if (!response.ok) {
      if (response.status === 401) {
        localStorage.removeItem('token')
        localStorage.removeItem('username')
        window.location.href = '/login'
        throw new Error('登录已过期，请重新登录')
      }
      if (response.status === 403) {
        throw new Error('没有权限访问该资源')
      }
      throw new Error(`请求失败: ${response.status}`)
    }
    
    return response
  },
  
  clearSession: async (sessionId) => {
    return await apiClient.delete(`/chat/session/${sessionId}`)
  },
}
