import api from './index'

export function getBalanceSheet(params) {
  return api.get('/financial-statement/balance-sheet', { params })
}

export function getIncomeStatement(params) {
  return api.get('/financial-statement/income-statement', { params })
}

export function getCashFlowStatement(params) {
  return api.get('/financial-statement/cash-flow', { params })
}
