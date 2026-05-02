import apiClient from './api'

export const documentApi = {
  getRecent: async (limit = 10) => {
    return await apiClient.get('/documents/recent', { params: { limit } })
  },
  
  getByKnowledgeBase: async (kbId) => {
    return await apiClient.get(`/documents/knowledge-base/${kbId}`)
  },
  
  getStsCredential: async (fileName, filePath, hashCode, fileSize) => {
    return await apiClient.post('/documents/sts-credential', { fileName, filePath, hashCode, fileSize })
  },
  
  confirmUpload: async (data) => {
    return await apiClient.post('/documents/confirm-upload', data)
  },
  
  delete: async (id) => {
    return await apiClient.delete(`/documents/${id}`)
  },
}
