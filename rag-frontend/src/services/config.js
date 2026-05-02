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

export const tosConfig = {
  endpoint: import.meta.env.VITE_TOS_ENDPOINT || 'tos-cn-beijing.volces.com',
  region: import.meta.env.VITE_TOS_REGION || 'cn-beijing',
  bucketName: import.meta.env.VITE_TOS_BUCKET_NAME || '',
}
