import axios from 'axios'

const BASE = '/api/fixed-asset'

export function getAssets() {
  return axios.get(BASE)
}

export function getAsset(id) {
  return axios.get(`${BASE}/${id}`)
}

export function createAsset(data) {
  return axios.post(BASE, data)
}

export function updateAsset(id, data) {
  return axios.put(`${BASE}/${id}`, data)
}

export function deleteAsset(id) {
  return axios.delete(`${BASE}/${id}`)
}

export function depreciate(period) {
  return axios.post(`${BASE}/depreciate`, null, { params: { period } })
}
