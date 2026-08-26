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
