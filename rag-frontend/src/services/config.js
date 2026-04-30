import apiClient from './api'

export const configApi = {
  getAll: async () => {
    return await apiClient.get('/config')
  },
  
  update: async (configs) => {
    return await apiClient.put('/config', configs)
  },
  
  updateSingle: async (key, value) => {
    return await apiClient.put(`/config/${key}`, { value })
  },
}
