import axios from 'axios'

const BASE = '/api/salary'

export function getSalaryByMonth(month) {
  return axios.get(BASE, { params: { month } })
}

export function batchSave(records) {
  return axios.post(`${BASE}/batch`, records)
}

export function generateVoucher(month) {
  return axios.post(`${BASE}/generate-voucher`, null, { params: { month } })
}

export function generatePayment(month) {
  return axios.post(`${BASE}/generate-payment`, null, { params: { month } })
}

export function deleteRecord(id) {
  return axios.delete(`${BASE}/${id}`)
}
