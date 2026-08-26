export const ROLES = {
  ADMIN: '管理员',
  MANAGER: '经理',
  ACCOUNTANT: '会计',
  VIEWER: '只读用户'
}

export function getUser() {
  try {
    return JSON.parse(localStorage.getItem('user') || '{}')
  } catch {
    return {}
  }
}

export function getRole() {
  return getUser().role || ''
}

export function hasRole(...roles) {
  return roles.includes(getRole())
}

/** 是否可执行写操作（新增/修改/删除） */
export function canWrite() {
  return hasRole('ADMIN', 'MANAGER', 'ACCOUNTANT')
}

export function roleText(role) {
  return ROLES[role] || role
}
