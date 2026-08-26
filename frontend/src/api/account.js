import api from './index'

export function getAccountList(params) {
  return api.get('/account', { params })
}

export function getAccount(id) {
  return api.get(`/account/${id}`)
}

export function createAccount(data) {
  return api.post('/account', data)
}

export function updateAccount(id, data) {
  return api.put(`/account/${id}`, data)
}

export function deleteAccount(id) {
  return api.delete(`/account/${id}`)
}

export function initDefaultAccounts() {
  return api.post('/account/init')
}
