import { createRouter, createWebHistory } from 'vue-router'
import { getRole } from '../utils/auth'

const routes = [
  { path: '/login', name: 'Login', component: () => import('../views/Login.vue'), meta: { public: true, title: '登录' } },
  { path: '/register', name: 'Register', component: () => import('../views/Register.vue'), meta: { public: true, title: '注册' } },
  { path: '/', redirect: '/login' },
  { path: '/dashboard', name: 'Dashboard', component: () => import('../views/Dashboard.vue'), meta: { title: '财务仪表盘', section: '工作台', roles: ['ADMIN', 'MANAGER', 'ACCOUNTANT', 'VIEWER'] } },
  { path: '/voucher/add', name: 'AddVoucher', component: () => import('../views/AddVoucher.vue'), meta: { title: '新增凭证', section: '凭证管理', roles: ['ADMIN', 'MANAGER', 'ACCOUNTANT'] } },
  { path: '/voucher/list', name: 'ViewVoucher', component: () => import('../views/ViewVoucher.vue'), meta: { title: '查看凭证', section: '凭证管理', roles: ['ADMIN', 'MANAGER', 'ACCOUNTANT', 'VIEWER'] } },
  { path: '/voucher/template', name: 'VoucherTemplate', component: () => import('../views/VoucherTemplate.vue'), meta: { title: '凭证模板', section: '凭证管理', roles: ['ADMIN', 'MANAGER', 'ACCOUNTANT'] } },
  { path: '/voucher/invoice', name: 'ManageInvoice', component: () => import('../views/ManageInvoice.vue'), meta: { title: '发票管理', section: '凭证管理', roles: ['ADMIN', 'MANAGER', 'ACCOUNTANT', 'VIEWER'] } },
  { path: '/account', name: 'AccountList', component: () => import('../views/AccountList.vue'), meta: { title: '会计科目', section: '凭证管理', roles: ['ADMIN', 'MANAGER', 'ACCOUNTANT'] } },
  { path: '/ledger/detail', name: 'DetailLedger', component: () => import('../views/DetailLedger.vue'), meta: { title: '明细账', section: '账簿管理', roles: ['ADMIN', 'MANAGER', 'ACCOUNTANT', 'VIEWER'] } },
  { path: '/ledger/general', name: 'GeneralLedger', component: () => import('../views/GeneralLedger.vue'), meta: { title: '总账', section: '账簿管理', roles: ['ADMIN', 'MANAGER', 'ACCOUNTANT', 'VIEWER'] } },
  { path: '/ledger/trial-balance', name: 'TrialBalance', component: () => import('../views/TrialBalance.vue'), meta: { title: '科目余额表', section: '账簿管理', roles: ['ADMIN', 'MANAGER', 'ACCOUNTANT', 'VIEWER'] } },
  { path: '/period-close', name: 'PeriodClose', component: () => import('../views/PeriodClose.vue'), meta: { title: '期末结转', section: '账务处理', roles: ['ADMIN', 'MANAGER', 'ACCOUNTANT'] } },
  { path: '/tax', name: 'TaxCalc', component: () => import('../views/TaxCalc.vue'), meta: { title: '税务测算', section: '税务', roles: ['ADMIN', 'MANAGER', 'ACCOUNTANT', 'VIEWER'] } },
  { path: '/tax/analysis', name: 'TaxAnalysis', component: () => import('../views/TaxAnalysis.vue'), meta: { title: '税务分析报告', section: '税务', roles: ['ADMIN', 'MANAGER', 'ACCOUNTANT', 'VIEWER'] } },
  { path: '/expense', name: 'ExpenseList', component: () => import('../views/ExpenseList.vue'), meta: { title: '费用报销', section: '业务管理', roles: ['ADMIN', 'MANAGER', 'ACCOUNTANT'] } },
  { path: '/contact', name: 'ContactList', component: () => import('../views/ContactList.vue'), meta: { title: '客户/供应商', section: '业务管理', roles: ['ADMIN', 'MANAGER', 'ACCOUNTANT', 'VIEWER'] } },
  { path: '/report', name: 'FinancialReport', component: () => import('../views/FinancialReport.vue'), meta: { title: '财务报表', section: '报表', roles: ['ADMIN', 'MANAGER', 'ACCOUNTANT', 'VIEWER'] } },
  { path: '/budget', name: 'Budget', component: () => import('../views/BudgetList.vue'), meta: { title: '预算管理', section: '业务管理', roles: ['ADMIN', 'MANAGER', 'ACCOUNTANT'] } },
  { path: '/chat', name: 'AiChat', component: () => import('../views/AiChat.vue'), meta: { title: '智能助手', section: '工作台', roles: ['ADMIN', 'MANAGER', 'ACCOUNTANT', 'VIEWER'] } },
  { path: '/company-settings', name: 'CompanySettings', component: () => import('../views/CompanySettings.vue'), meta: { title: '公司设置', section: '系统', roles: ['ADMIN', 'MANAGER'] } },
  { path: '/bank-transaction', name: 'BankTransaction', component: () => import('../views/BankTransaction.vue'), meta: { title: '银行流水', section: '业务管理', roles: ['ADMIN', 'MANAGER', 'ACCOUNTANT'] } },
  { path: '/fixed-asset', name: 'FixedAsset', component: () => import('../views/FixedAsset.vue'), meta: { title: '固定资产', section: '业务管理', roles: ['ADMIN', 'MANAGER', 'ACCOUNTANT'] } },
  { path: '/salary', name: 'SalaryManagement', component: () => import('../views/SalaryManagement.vue'), meta: { title: '工资管理', section: '业务管理', roles: ['ADMIN', 'MANAGER'] } },
  { path: '/users', name: 'UserList', component: () => import('../views/UserList.vue'), meta: { title: '用户管理', section: '系统', roles: ['ADMIN'] } },
  { path: '/:pathMatch(.*)*', name: 'NotFound', component: () => import('../views/NotFound.vue'), meta: { public: true, title: '页面不存在' } }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  document.title = to.meta.title ? `${to.meta.title} · FIVA 财税` : 'FIVA 财税系统'
  if (to.meta.public) {
    if (token && to.name === 'Login') return next('/dashboard')
    next()
  } else if (!token) {
    next('/login')
  } else if (to.meta.roles && !to.meta.roles.includes(getRole())) {
    console.warn('无权访问:', to.path)
    next('/dashboard')
  } else {
    next()
  }
})

export default router
