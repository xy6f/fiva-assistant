import api from './index'

export function login(data) {
  return api.post('/auth/login', data)
}

export function register(data) {
  return api.post('/auth/register', data)
}

export function getCaptcha() {
  return api.get('/auth/captcha')
}

export function getCompany() {
  return api.get('/auth/company')
}

export function updateCompany(data) {
  return api.put('/auth/company', data)
}

export function getCurrentUser() {
  return api.get('/auth/me')
}

export function getUserList() {
  return api.get('/auth/users')
}

export function createUser(data) {
  return api.post('/auth/users', data)
}

export function updateUser(id, data) {
  return api.put(`/auth/users/${id}`, data)
}

export function toggleUser(id) {
  return api.post(`/auth/users/${id}/toggle`)
}

export function resetPassword(id, newPassword) {
  return api.post(`/auth/users/${id}/reset-password`, { newPassword })
}

export function deleteUser(id) {
  return api.delete(`/auth/users/${id}`)
}
