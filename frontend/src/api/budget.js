import api from './index'

export function getBudgetList(year, month) {
  return api.get('/budget', { params: { year, month } })
}

export function getDepartments() {
  return api.get('/budget/departments')
}

export function getBudget(id) {
  return api.get(`/budget/${id}`)
}

export function createBudget(data) {
  return api.post('/budget', data)
}

export function updateBudget(id, data) {
  return api.put(`/budget/${id}`, data)
}

export function updateBudgetUsed(id, usedAmount) {
  return api.put(`/budget/${id}/used`, { usedAmount })
}

export function deleteBudget(id) {
  return api.delete(`/budget/${id}`)
}

export function getOverBudget(year, month) {
  return api.get('/budget/over-budget', { params: { year, month } })
}
