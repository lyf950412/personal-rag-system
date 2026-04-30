import apiClient from './api'

export const documentApi = {
  getAll: async () => {
    return await apiClient.get('/documents')
  },
  
  getRecent: async (limit = 10) => {
    return await apiClient.get('/documents/recent', { params: { limit } })
  },
  
  getById: async (id) => {
    return await apiClient.get(`/documents/${id}`)
  },
  
  getByKnowledgeBase: async (kbId) => {
    return await apiClient.get(`/documents/knowledge-base/${kbId}`)
  },
  
  upload: async (file, knowledgeBaseId) => {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('knowledgeBaseId', knowledgeBaseId)
    
    return await apiClient.post('/documents/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    })
  },
  
  delete: async (id) => {
    return await apiClient.delete(`/documents/${id}`)
  },
}
