import api from './index'

export function getFinancialReport(period) {
  return api.get('/report', { params: { period } })
}

export function exportReportExcel(period) {
  return api.get('/report/export/excel', {
    params: { period },
    responseType: 'blob'
  })
}

export function exportReportPdf(period) {
  return api.get('/report/export/pdf', {
    params: { period },
    responseType: 'blob'
  })
}
