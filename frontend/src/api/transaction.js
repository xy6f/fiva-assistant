import api from './index'

export function getTransactionList(params) {
  return api.get('/transaction/list', { params })
}

export function getTransactionSummary() {
  return api.get('/transaction/summary')
}

export function createTransaction(data) {
  return api.post('/transaction', data)
}

export function updateTransaction(id, data) {
  return api.put(`/transaction/${id}`, data)
}

export function deleteTransaction(id) {
  return api.delete(`/transaction/${id}`)
}

export function exportTransactionExcel(params) {
  return api.get('/transaction/export/excel', {
    params,
    responseType: 'blob'
  })
}
