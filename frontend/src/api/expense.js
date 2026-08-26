import api from './index'

export function getExpenseList(keyword) {
  return api.get('/expense', { params: { keyword } })
}

export function getExpense(id) {
  return api.get(`/expense/${id}`)
}

export function createExpense(data) {
  return api.post('/expense', data)
}

export function updateExpense(id, data) {
  return api.put(`/expense/${id}`, data)
}

export function approveExpense(id, data) {
  return api.post(`/expense/${id}/approve`, data)
}

export function deleteExpense(id) {
  return api.delete(`/expense/${id}`)
}

export function getExpenseStatistics() {
  return api.get('/expense/statistics')
}

export function exportExpenseExcel(keyword) {
  return api.get('/expense/export/excel', {
    params: { keyword },
    responseType: 'blob'
  })
}
