import api from './index'

export function getVoucherList(params) {
  return api.get('/voucher/list', { params })
}

/**
 * 分页查询凭证（返回 {list,total,page,pageSize}）
 */
export function getVoucherPage(params) {
  return api.get('/voucher/page', { params })
}

export function getVoucherSummary() {
  return api.get('/voucher/summary')
}

export function createVoucher(data) {
  return api.post('/voucher', data)
}

export function updateVoucher(id, data) {
  return api.put(`/voucher/${id}`, data)
}

export function deleteVoucher(id) {
  return api.delete(`/voucher/${id}`)
}

export function getVoucher(id) {
  return api.get(`/voucher/${id}`)
}

export function exportVoucherExcel(params) {
  return api.get('/voucher/export/excel', {
    params,
    responseType: 'blob'
  })
}

export function submitForReview(id) {
  return api.post(`/voucher/${id}/submit`)
}

export function approveVoucher(id) {
  return api.post(`/voucher/${id}/approve`)
}

export function rejectVoucher(id) {
  return api.post(`/voucher/${id}/reject`)
}
