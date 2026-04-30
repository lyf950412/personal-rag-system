import apiClient from './api'

export const dashboardApi = {
  getStats: async () => {
    return await apiClient.get('/dashboard/stats')
  },
}
