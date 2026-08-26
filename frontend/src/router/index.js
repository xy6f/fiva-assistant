import { createRouter, createWebHistory } from 'vue-router'
import { getRole } from '../utils/auth'

const routes = [
  { path: '/login', name: 'Login', component: () => import('../views/Login.vue'), meta: { public: true } },
  { path: '/register', name: 'Register', component: () => import('../views/Register.vue'), meta: { public: true } },
  { path: '/', redirect: '/dashboard' },
  { path: '/dashboard', name: 'Dashboard', component: () => import('../views/Dashboard.vue'), meta: { roles: ['ADMIN', 'MANAGER', 'ACCOUNTANT', 'VIEWER'] } },
  { path: '/voucher/add', name: 'AddVoucher', component: () => import('../views/AddVoucher.vue'), meta: { roles: ['ADMIN', 'MANAGER', 'ACCOUNTANT'] } },
  { path: '/voucher/list', name: 'ViewVoucher', component: () => import('../views/ViewVoucher.vue'), meta: { roles: ['ADMIN', 'MANAGER', 'ACCOUNTANT', 'VIEWER'] } },
  { path: '/voucher/template', name: 'VoucherTemplate', component: () => import('../views/VoucherTemplate.vue'), meta: { roles: ['ADMIN', 'MANAGER', 'ACCOUNTANT'] } },
  { path: '/voucher/invoice', name: 'ManageInvoice', component: () => import('../views/ManageInvoice.vue'), meta: { roles: ['ADMIN', 'MANAGER', 'ACCOUNTANT', 'VIEWER'] } },
  { path: '/account', name: 'AccountList', component: () => import('../views/AccountList.vue'), meta: { roles: ['ADMIN', 'MANAGER', 'ACCOUNTANT'] } },
  { path: '/ledger/detail', name: 'DetailLedger', component: () => import('../views/DetailLedger.vue'), meta: { roles: ['ADMIN', 'MANAGER', 'ACCOUNTANT', 'VIEWER'] } },
  { path: '/ledger/general', name: 'GeneralLedger', component: () => import('../views/GeneralLedger.vue'), meta: { roles: ['ADMIN', 'MANAGER', 'ACCOUNTANT', 'VIEWER'] } },
  { path: '/ledger/trial-balance', name: 'TrialBalance', component: () => import('../views/TrialBalance.vue'), meta: { roles: ['ADMIN', 'MANAGER', 'ACCOUNTANT', 'VIEWER'] } },
  { path: '/period-close', name: 'PeriodClose', component: () => import('../views/PeriodClose.vue'), meta: { roles: ['ADMIN', 'MANAGER', 'ACCOUNTANT'] } },
  { path: '/tax', name: 'TaxCalc', component: () => import('../views/TaxCalc.vue'), meta: { roles: ['ADMIN', 'MANAGER', 'ACCOUNTANT', 'VIEWER'] } },
  { path: '/tax/analysis', name: 'TaxAnalysis', component: () => import('../views/TaxAnalysis.vue'), meta: { roles: ['ADMIN', 'MANAGER', 'ACCOUNTANT', 'VIEWER'] } },
  { path: '/expense', name: 'ExpenseList', component: () => import('../views/ExpenseList.vue'), meta: { roles: ['ADMIN', 'MANAGER', 'ACCOUNTANT'] } },
  { path: '/contact', name: 'ContactList', component: () => import('../views/ContactList.vue'), meta: { roles: ['ADMIN', 'MANAGER', 'ACCOUNTANT', 'VIEWER'] } },
  { path: '/report', name: 'FinancialReport', component: () => import('../views/FinancialReport.vue'), meta: { roles: ['ADMIN', 'MANAGER', 'ACCOUNTANT', 'VIEWER'] } },
  { path: '/budget', name: 'Budget', component: () => import('../views/BudgetList.vue'), meta: { roles: ['ADMIN', 'MANAGER', 'ACCOUNTANT'] } },
  { path: '/chat', name: 'AiChat', component: () => import('../views/AiChat.vue'), meta: { roles: ['ADMIN', 'MANAGER', 'ACCOUNTANT', 'VIEWER'] } },
  { path: '/company-settings', name: 'CompanySettings', component: () => import('../views/CompanySettings.vue'), meta: { roles: ['ADMIN', 'MANAGER'] } },
  { path: '/bank-transaction', name: 'BankTransaction', component: () => import('../views/BankTransaction.vue'), meta: { roles: ['ADMIN', 'MANAGER', 'ACCOUNTANT'] } },
  { path: '/fixed-asset', name: 'FixedAsset', component: () => import('../views/FixedAsset.vue'), meta: { roles: ['ADMIN', 'MANAGER', 'ACCOUNTANT'] } },
  { path: '/salary', name: 'SalaryManagement', component: () => import('../views/SalaryManagement.vue'), meta: { roles: ['ADMIN', 'MANAGER'] } }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫 - 检查登录状态 + 角色权限
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.meta.public) {
    // 公开页面（登录页）直接放行
    next()
  } else if (!token) {
    // 未登录跳转到登录页
    next('/login')
  } else if (to.meta.roles && !to.meta.roles.includes(getRole())) {
    // 已登录但无权访问该页面，跳回仪表盘
    console.warn('无权访问:', to.path)
    next('/dashboard')
  } else {
    next()
  }
})

export default router
