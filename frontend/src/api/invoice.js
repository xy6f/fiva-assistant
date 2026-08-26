import api from './index'

export function uploadInvoice(file) {
  const fd = new FormData()
  fd.append('file', file)
  return api.post('/invoice/upload', fd)
}

export function getInvoices() {
  return api.get('/invoices')
}

export function getInvoice(id) {
  return api.get(`/invoices/${id}`)
}

export function updateInvoice(id, data) {
  return api.put(`/invoices/${id}`, data)
}

export function deleteInvoice(id) {
  return api.delete(`/invoices/${id}`)
}

export function createVoucherFromInvoice(id, data) {
  return api.post(`/invoices/${id}/create-voucher`, null, { params: data })
}

export function verifyInvoice(id) {
  return api.post(`/invoices/${id}/verify`)
}

export function batchVerify() {
  return api.post('/invoices/batch-verify')
}
