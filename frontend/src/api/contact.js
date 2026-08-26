import api from './index'

export function getContactList(type, keyword) {
  return api.get('/contact', { params: { type, keyword } })
}

export function getContact(id) {
  return api.get(`/contact/${id}`)
}

export function createContact(data) {
  return api.post('/contact', data)
}

export function updateContact(id, data) {
  return api.put(`/contact/${id}`, data)
}

export function deleteContact(id) {
  return api.delete(`/contact/${id}`)
}
