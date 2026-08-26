import api from './index'

export function getTemplates(type) {
  return api.get('/voucher-template', { params: { type } })
}

export function getTemplate(id) {
  return api.get(`/voucher-template/${id}`)
}

export function createTemplate(data) {
  return api.post('/voucher-template', data)
}

export function updateTemplate(id, data) {
  return api.put(`/voucher-template/${id}`, data)
}

export function deleteTemplate(id) {
  return api.delete(`/voucher-template/${id}`)
}

export function applyTemplate(id, params) {
  return api.post(`/voucher-template/${id}/apply`, null, { params })
}

export function initTemplates() {
  return api.post('/voucher-template/init')
}
