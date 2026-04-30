import apiClient from './api'

export const knowledgeBaseApi = {
  getAll: async () => {
    return await apiClient.get('/knowledge-bases')
  },
  
  getById: async (id) => {
    return await apiClient.get(`/knowledge-bases/${id}`)
  },
  
  create: async (data) => {
    return await apiClient.post('/knowledge-bases', data)
  },
  
  update: async (id, data) => {
    return await apiClient.put(`/knowledge-bases/${id}`, data)
  },
  
  delete: async (id) => {
    return await apiClient.delete(`/knowledge-bases/${id}`)
  },
  
  search: async (keyword) => {
    return await apiClient.get('/knowledge-bases/search', { params: { keyword } })
  },
}
