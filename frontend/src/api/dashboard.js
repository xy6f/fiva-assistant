import api from './index'

export function getDashboardSummary() {
  return api.get('/dashboard/summary')
}
