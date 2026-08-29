import api from './index'

export function getDetailLedger(params) {
  return api.get('/ledger/detail', { params })
}

export function getGeneralLedger(params) {
  return api.get('/ledger/general', { params })
}

export function getTrialBalance(params) {
  return api.get('/ledger/trial-balance', { params })
}

export function exportTrialBalanceExcel(params) {
  return api.get('/ledger/trial-balance/export/excel', { params, responseType: 'blob' })
}

export function exportDetailLedgerExcel(params) {
  return api.get('/ledger/detail/export/excel', { params, responseType: 'blob' })
}

export function exportGeneralLedgerExcel(params) {
  return api.get('/ledger/general/export/excel', { params, responseType: 'blob' })
}
