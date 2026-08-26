import axios from 'axios'

const BASE = '/api/bank-transaction'

export function importFile(file) {
  const formData = new FormData()
  formData.append('file', file)
  return axios.post(`${BASE}/import`, formData)
}

export function getTransactions(status) {
  return axios.get(BASE, { params: status ? { status } : {} })
}

export function matchTransaction(id) {
  return axios.post(`${BASE}/${id}/match`)
}

export function batchMatch(ids) {
  return axios.post(`${BASE}/batch-match`, { ids })
}

export function ignoreTransaction(id) {
  return axios.post(`${BASE}/${id}/ignore`)
}

export function deleteTransaction(id) {
  return axios.delete(`${BASE}/${id}`)
}
