<template>
  <template v-if="isLoggedIn">
    <button class="mobile-menu-btn" @click="toggleSidebar">
      <el-icon><Fold /></el-icon>
    </button>
    <div class="mobile-overlay" :class="{ 'is-visible': sidebarOpen }" @click="closeSidebar"></div>
  </template>

  <el-container class="app-shell" :class="{ 'theme-dark': isDark }" v-if="isLoggedIn">
    <el-aside :width="sidebarWidth" class="app-sidebar" :class="{ 'is-open': sidebarOpen, 'is-collapsed': sidebarCollapsed && !sidebarPeek }" @mouseenter="handleSidebarEnter" @mouseleave="handleSidebarLeave">
      <div class="brand-block">
        <div class="brand-mark">F</div>
        <div class="brand-copy">
          <div class="brand-title">FIVA Assistant</div>
          <div class="brand-subtitle">智能财税工作台</div>
        </div>
        <button class="sidebar-collapse-btn" type="button" @click.stop="toggleSidebarCollapsed">
          <el-icon><Fold v-if="!sidebarCollapsed" /><Expand v-else /></el-icon>
        </button>
      </div>
      <el-menu class="app-menu" :default-active="activeMenu" router :collapse="sidebarCollapsed && !sidebarPeek" :collapse-transition="false">
        <el-menu-item index="/dashboard" v-if="canSee('/dashboard')"><el-icon><DataAnalysis /></el-icon><span>财务仪表盘</span></el-menu-item>
        <el-sub-menu index="/voucher">
          <template #title><el-icon><Document /></el-icon><span>凭证管理</span></template>
          <el-menu-item index="/voucher/add" v-if="canSee('/voucher/add')">新增凭证</el-menu-item>
          <el-menu-item index="/voucher/list" v-if="canSee('/voucher/list')">查看凭证</el-menu-item>
          <el-menu-item index="/voucher/template" v-if="canSee('/voucher/template')">凭证模板</el-menu-item>
          <el-menu-item index="/voucher/invoice" v-if="canSee('/voucher/invoice')">发票管理</el-menu-item>
          <el-menu-item index="/account" v-if="canSee('/account')">会计科目</el-menu-item>
        </el-sub-menu>
        <el-sub-menu index="/ledger" v-if="canSee('/ledger/detail')">
          <template #title><el-icon><Document /></el-icon><span>账簿管理</span></template>
          <el-menu-item index="/ledger/detail">明细账</el-menu-item>
          <el-menu-item index="/ledger/general">总账</el-menu-item>
          <el-menu-item index="/ledger/trial-balance">科目余额表</el-menu-item>
        </el-sub-menu>
        <el-menu-item index="/period-close" v-if="canSee('/period-close')"><el-icon><RefreshRight /></el-icon><span>期末结转</span></el-menu-item>
        <el-menu-item index="/tax" v-if="canSee('/tax')"><el-icon><TrendCharts /></el-icon><span>税务测算</span></el-menu-item>
        <el-menu-item index="/expense" v-if="canSee('/expense')"><el-icon><Ticket /></el-icon><span>费用报销</span></el-menu-item>
        <el-menu-item index="/contact" v-if="canSee('/contact')"><el-icon><User /></el-icon><span>客户/供应商</span></el-menu-item>
        <el-menu-item index="/report" v-if="canSee('/report')"><el-icon><Document /></el-icon><span>财务报表</span></el-menu-item>
        <el-menu-item index="/budget" v-if="canSee('/budget')"><el-icon><Coin /></el-icon><span>预算管理</span></el-menu-item>
        <el-menu-item index="/bank-transaction" v-if="canSee('/bank-transaction')"><el-icon><Document /></el-icon><span>银行流水</span></el-menu-item>
        <el-menu-item index="/fixed-asset" v-if="canSee('/fixed-asset')"><el-icon><Coin /></el-icon><span>固定资产</span></el-menu-item>
        <el-menu-item index="/salary" v-if="canSee('/salary')"><el-icon><User /></el-icon><span>工资管理</span></el-menu-item>
        <el-menu-item index="/chat" v-if="canSee('/chat')"><el-icon><ChatDotRound /></el-icon><span>AI情报中心</span></el-menu-item>
        <el-menu-item index="/company-settings" v-if="canSee('/company-settings')"><el-icon><Setting /></el-icon><span>公司设置</span></el-menu-item>
      </el-menu>
    </el-aside>
    <el-container class="workspace">
      <el-header class="app-header">
        <div class="page-context">
          <div class="eyebrow">FIVA Workspace</div>
          <div class="current-title">财税智能工作台</div>
        </div>
        <div class="user-cluster">
          <button class="ai-launcher" type="button" @click="openAiDrawer">
            <span class="ai-launcher-orb"><span></span></span>
            <span class="ai-launcher-text">AI 助手</span>
          </button>
          <button class="theme-toggle" :class="{ active: isDark }" type="button" @click="toggleTheme" :aria-label="isDark ? '切换浅色' : '切换深色'">
            <span class="theme-track-icon sun"><el-icon><Sunny /></el-icon></span>
            <span class="theme-track-icon moon"><el-icon><Moon /></el-icon></span>
            <span class="theme-thumb"><el-icon><Moon v-if="isDark" /><Sunny v-else /></el-icon></span>
          </button>
          <el-dropdown trigger="click" @command="handleUserCommand">
            <button class="profile-button">
              <div class="user-avatar">{{ avatarText }}</div>
              <span class="user-name">{{ displayName }}</span>
              <el-tag class="role-pill" :type="roleType" size="small">{{ roleText }}</el-tag>
              <el-icon class="profile-caret"><ArrowDown /></el-icon>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="settings">公司设置</el-dropdown-item>
                <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="app-main">
        <router-view @open-ai="openAiDrawer" />
      </el-main>
    </el-container>

    <el-dialog v-model="aiDrawerOpen" class="ai-dialog" width="760px" :show-close="false" align-center>
      <div class="ai-panel">
        <div class="ai-panel-head">
          <div class="ai-hero-logo"><span></span><i></i></div>
          <div class="ai-title-block">
            <span>AI 财税助手</span>
            <p>用自然语言分析凭证、税务、报表和经营异常。</p>
          </div>
          <el-button class="icon-tool ai-close" circle size="small" @click="aiDrawerOpen = false"><el-icon><Close /></el-icon></el-button>
        </div>
        <div class="ai-messages" ref="aiMessagesRef">
          <div v-if="aiMessages.length === 0" class="ai-empty">
            <strong>选择一个问题开始</strong>
            <div class="ai-question-marquee">
              <div class="ai-question-track">
                <button v-for="(q, index) in rollingQuestions" :key="`${q}-${index}`" type="button" @click="sendQuick(q)">{{ q }}</button>
              </div>
            </div>
          </div>
          <div v-for="(msg, index) in aiMessages" :key="index" class="ai-message" :class="msg.role">
            <div class="ai-bubble">{{ msg.content }}</div>
          </div>
        </div>
        <div class="ai-input-row">
          <el-input v-model="aiInput" placeholder="输入问题，例如：分析本月利润变化" @keyup.enter="sendAiMessage" :disabled="aiLoading" />
          <el-button type="primary" @click="sendAiMessage" :loading="aiLoading">发送</el-button>
        </div>
      </div>
    </el-dialog>
  </el-container>
  <router-view v-else />
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { streamChat } from './api/chat'

const route = useRoute()
const router = useRouter()
const activeMenu = computed(() => route.path)
const sidebarOpen = ref(false)
const sidebarCollapsed = ref(localStorage.getItem('sidebar_collapsed') === 'true')
const sidebarPeek = ref(false)
const sidebarHoverLocked = ref(false)
const isLoggedIn = ref(!!localStorage.getItem('token'))
const isDark = ref(localStorage.getItem('theme') === 'dark')
const aiDrawerOpen = ref(false)
const aiInput = ref('')
const aiLoading = ref(false)
const aiMessages = ref([])
const aiSessionId = ref(localStorage.getItem('chat_session') || '')
const aiMessagesRef = ref(null)
const quickQuestions = ['分析本月利润表现', '提示税务风险', '生成经营摘要', '检查发票异常', '预测现金流压力', '给出节税建议', '总结费用波动', '生成老板版简报']
const rollingQuestions = computed(() => [...quickQuestions, ...quickQuestions])
const sidebarWidth = computed(() => (sidebarCollapsed.value && !sidebarPeek.value ? '78px' : '248px'))

const currentUser = computed(() => {
  try { return JSON.parse(localStorage.getItem('user') || '{}') } catch { return {} }
})
const displayName = computed(() => currentUser.value.realName || currentUser.value.username || '用户')
const avatarText = computed(() => displayName.value.slice(0, 1).toUpperCase())
const roleText = computed(() => {
  const r = currentUser.value.role
  return r === 'ADMIN' ? '管理员' : r === 'MANAGER' ? '经理' : r === 'ACCOUNTANT' ? '会计' : '查看者'
})
const roleType = computed(() => {
  const r = currentUser.value.role
  return r === 'ADMIN' ? 'danger' : r === 'MANAGER' ? 'warning' : r === 'ACCOUNTANT' ? '' : 'info'
})

// 按角色控制菜单可见性（与路由 meta.roles 一致）
function canSee(path) {
  const resolved = router.resolve(path)
  const allowed = resolved?.meta?.roles
  if (!allowed || !allowed.length) return true
  return allowed.includes(currentUser.value.role || '')
}

function handleLogout() {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  isLoggedIn.value = false
  router.push('/login')
}
function handleUserCommand(command) {
  if (command === 'logout') handleLogout()
  if (command === 'settings') router.push('/company-settings')
}
function toggleSidebar() { sidebarOpen.value = !sidebarOpen.value }
function closeSidebar() { sidebarOpen.value = false }
function toggleSidebarCollapsed() {
  sidebarCollapsed.value = !sidebarCollapsed.value
  sidebarPeek.value = false
  sidebarHoverLocked.value = sidebarCollapsed.value
  localStorage.setItem('sidebar_collapsed', sidebarCollapsed.value ? 'true' : 'false')
}
function handleSidebarEnter() {
  if (sidebarCollapsed.value && !sidebarHoverLocked.value) sidebarPeek.value = true
}
function handleSidebarLeave() {
  sidebarPeek.value = false
  sidebarHoverLocked.value = false
}
function toggleTheme() {
  isDark.value = !isDark.value
  localStorage.setItem('theme', isDark.value ? 'dark' : 'light')
}
function openAiDrawer(seedQuestion = '') {
  aiDrawerOpen.value = true
  if (typeof seedQuestion === 'string' && seedQuestion) {
    aiInput.value = seedQuestion
  }
}
function scrollAiToBottom() {
  nextTick(() => {
    if (aiMessagesRef.value) aiMessagesRef.value.scrollTop = aiMessagesRef.value.scrollHeight
  })
}
function sendQuick(question) {
  aiInput.value = question
  sendAiMessage()
}
function sendAiMessage() {
  const message = aiInput.value.trim()
  if (!message || aiLoading.value) return
  aiInput.value = ''
  aiMessages.value.push({ role: 'user', content: message })
  const assistant = { role: 'assistant', content: '' }
  aiMessages.value.push(assistant)
  aiLoading.value = true
  scrollAiToBottom()
  streamChat(aiSessionId.value, message, {
    onSession(sessionId) {
      aiSessionId.value = sessionId
      localStorage.setItem('chat_session', sessionId)
    },
    onToken(token) {
      assistant.content += token
      scrollAiToBottom()
    },
    onDone() { aiLoading.value = false },
    onError(error) {
      assistant.content = assistant.content || `AI 暂时不可用：${error}`
      aiLoading.value = false
      scrollAiToBottom()
    }
  })
}

onMounted(() => {})
onUnmounted(() => {})
</script>

<style>
* { margin: 0; padding: 0; box-sizing: border-box; }
html, body, #app { min-height: 100%; }
body { color: #172033; font-family: Inter, "PingFang SC", "Microsoft YaHei", Arial, sans-serif; background: #eef2f7; }
.app-shell { min-height: 100vh; background: radial-gradient(circle at 82% 8%, rgba(30, 125, 122, .12), transparent 30%), linear-gradient(180deg, #f7f9fc 0%, #edf2f7 100%); }
.app-shell.theme-dark { --fiva-dark-bg: #0f1726; --fiva-dark-panel: #162033; --fiva-dark-card: #1b2740; --fiva-dark-soft: #22314d; --fiva-dark-line: rgba(220,232,255,.12); --fiva-dark-text: #edf4ff; --fiva-dark-muted: #9aa9bf; background: radial-gradient(circle at 80% 4%, rgba(32,180,134,.16), transparent 28%), radial-gradient(circle at 18% 18%, rgba(43,127,255,.12), transparent 26%), linear-gradient(180deg, #0f1726 0%, #131d2f 100%); color: var(--fiva-dark-text); }
.app-sidebar { position: relative; overflow: hidden; border-right: 1px solid rgba(255,255,255,.12); background: linear-gradient(180deg, #121a2a 0%, #172033 48%, #102c2d 100%); box-shadow: 18px 0 45px rgba(16,32,48,.12); transition: width .28s cubic-bezier(.2,.8,.2,1), box-shadow .28s ease; will-change: width; }
.app-sidebar::after { content: ""; position: absolute; inset: auto 18px 18px; height: 1px; background: linear-gradient(90deg, transparent, rgba(255,255,255,.24), transparent); }
.brand-block { position: relative; display: flex; align-items: center; gap: 12px; min-height: 84px; padding: 22px 18px 16px; transition: padding .24s ease; }
.brand-mark { display: grid; width: 42px; height: 42px; place-items: center; border-radius: 8px; color: #fff; font-size: 22px; font-weight: 800; background: linear-gradient(135deg, #20b486, #2b7fff); box-shadow: 0 12px 28px rgba(32,180,134,.28); }
.brand-title { color: #fff; font-size: 16px; font-weight: 760; }
.brand-subtitle { margin-top: 3px; color: rgba(232,241,255,.58); font-size: 12px; }
.brand-copy { min-width: 0; opacity: 1; transform: translateX(0); transition: opacity .18s ease, transform .22s ease; white-space: nowrap; }
.sidebar-collapse-btn { display: grid; width: 34px; height: 34px; place-items: center; margin-left: auto; border: 1px solid rgba(255,255,255,.12); border-radius: 8px; color: rgba(232,241,255,.78); background: rgba(255,255,255,.08); cursor: pointer; transition: background .2s ease, color .2s ease, transform .2s ease; }
.sidebar-collapse-btn:hover { color: #fff; background: rgba(255,255,255,.14); transform: translateY(-1px); }
.app-sidebar.is-collapsed .brand-block { justify-content: center; padding: 18px 10px 14px; }
.app-sidebar.is-collapsed .brand-mark { width: 48px; height: 48px; border-radius: 12px; box-shadow: 0 16px 38px rgba(32,180,134,.34); }
.app-sidebar.is-collapsed .brand-copy { opacity: 0; pointer-events: none; transform: translateY(-8px); position: absolute; }
.app-sidebar.is-collapsed .sidebar-collapse-btn { opacity: 1; pointer-events: auto; position: absolute; right: 50%; top: 70px; width: 30px; height: 30px; transform: translateX(50%); background: rgba(255,255,255,.12); }
.app-sidebar.is-collapsed .app-menu { padding: 10px 10px 18px; }
.app-sidebar.is-collapsed .app-menu .el-menu-item, .app-sidebar.is-collapsed .app-menu .el-sub-menu__title { justify-content: center; padding: 0 !important; }
.app-sidebar.is-collapsed .app-menu .el-icon { margin-right: 0; font-size: 19px; }
.app-sidebar:not(.is-collapsed) .app-menu { animation: sidebarMenuReveal .22s ease both; }
@keyframes sidebarMenuReveal { from { opacity: .3; transform: translateY(-8px); } to { opacity: 1; transform: translateY(0); } }
.workspace { min-width: 0; }
.app-main > div, .app-main > section { width: 100%; margin-left: auto; margin-right: auto; transition: max-width .28s ease, transform .28s ease; }
.app-sidebar.is-collapsed + .workspace .app-main > div, .app-sidebar.is-collapsed + .workspace .app-main > section { max-width: 1320px; }
.app-header { height: 72px; display: flex; align-items: center; justify-content: space-between; padding: 0 28px; border-bottom: 1px solid rgba(38,54,78,.08); background: rgba(255,255,255,.82); backdrop-filter: blur(16px); }
.theme-dark .app-header { border-bottom-color: var(--fiva-dark-line); background: rgba(15,23,38,.86); }
.page-context { min-width: 0; }
.eyebrow { color: #718096; font-size: 11px; font-weight: 700; text-transform: uppercase; letter-spacing: .12em; }
.theme-dark .eyebrow { color: var(--fiva-dark-muted); }
.current-title { margin-top: 4px; color: #172033; font-size: 20px; font-weight: 760; }
.theme-dark .current-title { color: var(--fiva-dark-text); }
.user-cluster { display: flex; align-items: center; gap: 10px; }
.role-pill { border-radius: 999px; }
.tool-btn, .icon-tool { border-color: rgba(38,54,78,.10) !important; background: rgba(255,255,255,.84) !important; }
.ai-launcher { display: inline-flex; align-items: center; gap: 10px; height: 44px; padding: 0 14px 0 8px; border: 1px solid rgba(30,125,122,.18); border-radius: 999px; color: #14615f; font-weight: 780; background: linear-gradient(135deg, rgba(255,255,255,.95), rgba(232,244,243,.94)); box-shadow: 0 12px 28px rgba(30,125,122,.13); cursor: pointer; transition: transform .2s ease, box-shadow .2s ease; }
.ai-launcher:hover { transform: translateY(-1px); box-shadow: 0 16px 34px rgba(30,125,122,.20); }
.ai-launcher-orb { position: relative; display: grid; width: 32px; height: 32px; place-items: center; border-radius: 50%; background: conic-gradient(from 160deg, #20b486, #2b7fff, #8a5cf6, #20b486); animation: aiSpin 4.5s linear infinite; }
.ai-launcher-orb::before { content: ""; position: absolute; inset: -5px; border-radius: 50%; background: radial-gradient(circle, rgba(32,180,134,.34), transparent 62%); animation: aiPulse 1.9s ease-in-out infinite; }
.ai-launcher-orb span { position: relative; width: 18px; height: 18px; border-radius: 50%; background: #fff; box-shadow: inset 0 0 0 5px rgba(30,125,122,.18); }
.ai-launcher-text { white-space: nowrap; }
.theme-toggle { position: relative; width: 78px; height: 44px; border: 1px solid rgba(38,54,78,.10); border-radius: 999px; background: linear-gradient(135deg, #fff7df, #e8f4f3); box-shadow: 0 12px 28px rgba(17,32,50,.10); cursor: pointer; overflow: hidden; transition: background .25s ease, border-color .25s ease; }
.theme-toggle.active { border-color: rgba(32,180,134,.30); background: linear-gradient(135deg, #172033, #20324f); }
.theme-track-icon { position: absolute; top: 50%; transform: translateY(-50%); display: grid; width: 28px; height: 28px; place-items: center; color: rgba(23,32,51,.48); font-size: 16px; }
.theme-track-icon.sun { left: 7px; color: #c98716; }
.theme-track-icon.moon { right: 7px; color: #9aa9bf; }
.theme-thumb { position: absolute; top: 5px; left: 5px; display: grid; width: 32px; height: 32px; place-items: center; border-radius: 50%; color: #fff; background: linear-gradient(135deg, #f2a51a, #f6c45c); box-shadow: 0 8px 18px rgba(201,135,22,.28); transition: transform .25s ease, background .25s ease, box-shadow .25s ease; }
.theme-toggle.active .theme-thumb { transform: translateX(36px); background: linear-gradient(135deg, #2b7fff, #8a5cf6); box-shadow: 0 8px 20px rgba(43,127,255,.34); }
.profile-button { display: flex; align-items: center; gap: 9px; height: 40px; padding: 0 10px 0 4px; border: 1px solid rgba(38,54,78,.10); border-radius: 999px; background: rgba(255,255,255,.84); cursor: pointer; }
.user-avatar { display: grid; width: 34px; height: 34px; place-items: center; border-radius: 50%; color: #fff; font-weight: 800; background: linear-gradient(135deg, #172033, #1e7d7a 58%, #2b7fff); box-shadow: 0 8px 20px rgba(30,125,122,.22); }
.user-name { color: #2d3748; font-size: 14px; font-weight: 650; }
.profile-caret { color: #718096; }
.app-main { min-width: 0; padding: 26px 28px 32px; }
.theme-dark .app-main { color: var(--fiva-dark-text); }
.theme-dark .tool-btn, .theme-dark .icon-tool, .theme-dark .profile-button { border-color: var(--fiva-dark-line) !important; background: rgba(255,255,255,.08) !important; color: var(--fiva-dark-text) !important; }
.theme-dark .ai-launcher { border-color: rgba(32,180,134,.28); color: #dffcf3; background: linear-gradient(135deg, rgba(32,180,134,.18), rgba(43,127,255,.14)); box-shadow: 0 14px 34px rgba(0,0,0,.22); }
.theme-dark .user-name { color: var(--fiva-dark-text); }
.theme-dark .profile-caret { color: var(--fiva-dark-muted); }
.theme-dark .el-card, .theme-dark .stat-card, .theme-dark .kpi-card, .theme-dark .mini-card, .theme-dark .ai-insight-card { background: linear-gradient(180deg, rgba(27,39,64,.94), rgba(22,32,51,.94)) !important; border-color: var(--fiva-dark-line) !important; color: var(--fiva-dark-text) !important; box-shadow: 0 18px 45px rgba(0,0,0,.24) !important; }
.theme-dark .el-card__header { background: rgba(15,23,38,.72) !important; border-bottom-color: var(--fiva-dark-line) !important; color: var(--fiva-dark-text) !important; }
.theme-dark .app-main h2, .theme-dark .app-main h3, .theme-dark .card-title span, .theme-dark .kpi-card strong, .theme-dark .rank-row strong, .theme-dark .insight-head span { color: var(--fiva-dark-text) !important; }
.theme-dark .app-main p, .theme-dark .card-title small, .theme-dark .kpi-card span, .theme-dark .kpi-card small, .theme-dark .mini-card span, .theme-dark .rank-row span, .theme-dark .ai-insight-card p { color: var(--fiva-dark-muted) !important; }
.theme-dark .el-table { --el-table-bg-color: #162033; --el-table-tr-bg-color: #162033; --el-table-header-bg-color: #101827; --el-table-row-hover-bg-color: rgba(32,180,134,.16); --el-table-border-color: var(--fiva-dark-line); color: var(--fiva-dark-text); }
.theme-dark .el-table th.el-table__cell { background: #101827 !important; color: #b6c5dc !important; }
.theme-dark .el-table td.el-table__cell { border-bottom-color: var(--fiva-dark-line) !important; }
.theme-dark .el-input__wrapper, .theme-dark .el-select__wrapper, .theme-dark .el-textarea__inner, .theme-dark .el-input-number .el-input__wrapper { background: rgba(15,23,38,.78) !important; box-shadow: 0 0 0 1px var(--fiva-dark-line) inset !important; color: var(--fiva-dark-text) !important; }
.theme-dark .el-input__inner, .theme-dark .el-textarea__inner, .theme-dark .el-select__placeholder, .theme-dark .el-input-number__increase, .theme-dark .el-input-number__decrease { color: var(--fiva-dark-text) !important; }
.theme-dark .el-progress-bar__outer { background-color: rgba(232,241,255,.12) !important; }
.theme-dark .el-button.is-plain, .theme-dark .el-button--default { border-color: var(--fiva-dark-line) !important; background: rgba(255,255,255,.07) !important; color: var(--fiva-dark-text) !important; }
.theme-dark .el-tag { border-color: rgba(120,240,194,.22) !important; background: rgba(32,180,134,.12) !important; color: #dffcf3 !important; }
.theme-dark .el-select-dropdown, .theme-dark .el-picker-panel, .theme-dark .el-message-box { border-color: var(--fiva-dark-line) !important; background: #1b2740 !important; color: var(--fiva-dark-text) !important; }
.theme-dark .dashboard-hero { border-color: var(--fiva-dark-line); background: linear-gradient(135deg, #111a2b, #13293b 52%, #125c59); box-shadow: 0 20px 55px rgba(0,0,0,.28); }
.theme-dark .profit-panel, .theme-dark .metric-card, .theme-dark .invoice-strip, .theme-dark .invoice-chip, .theme-dark .brief-card, .theme-dark .radar-side, .theme-dark .intel-card, .theme-dark .ai-command-card, .theme-dark .intel-hero { border-color: var(--fiva-dark-line) !important; background: linear-gradient(180deg, rgba(27,39,64,.94), rgba(22,32,51,.94)) !important; color: var(--fiva-dark-text) !important; }
.theme-dark .profit-panel strong, .theme-dark .metric-card strong, .theme-dark .invoice-chip strong, .theme-dark .brief-card strong, .theme-dark .intel-card strong { color: var(--fiva-dark-text) !important; }
.theme-dark .profit-panel span, .theme-dark .metric-card span, .theme-dark .invoice-chip span, .theme-dark .brief-card span, .theme-dark .brief-card p, .theme-dark .side-title, .theme-dark .intel-card span, .theme-dark .intel-card p { color: var(--fiva-dark-muted) !important; }
.theme-dark .app-main, .theme-dark .app-main * { scrollbar-color: rgba(120,240,194,.28) rgba(15,23,38,.55); }
.theme-dark .app-main :where(.el-card, .el-card__body, .el-descriptions, .el-form, .el-tabs, .el-collapse, .el-timeline, .el-steps) { color: var(--fiva-dark-text) !important; }
.theme-dark .app-main :where(.el-form-item__label, .el-descriptions__label, .el-checkbox__label, .el-radio__label, .el-tabs__item, .el-empty__description, .el-upload__tip) { color: var(--fiva-dark-muted) !important; }
.theme-dark .app-main :where(.el-descriptions__content, .el-table .cell, .el-statistic__content, .el-timeline-item__content) { color: var(--fiva-dark-text) !important; }
.theme-dark .el-table tr, .theme-dark .el-table td.el-table__cell { background: #162033 !important; color: var(--fiva-dark-text) !important; }
.theme-dark .el-table__empty-text { color: var(--fiva-dark-muted) !important; }
.theme-dark .el-pagination, .theme-dark .el-pagination button, .theme-dark .el-pager li { color: var(--fiva-dark-text) !important; background: rgba(255,255,255,.06) !important; }
.theme-dark .el-dialog:not(.ai-dialog), .theme-dark .el-message-box { background: #1b2740 !important; color: var(--fiva-dark-text) !important; }
.theme-dark .el-dialog:not(.ai-dialog) .el-dialog__title, .theme-dark .el-message-box__title, .theme-dark .el-message-box__content { color: var(--fiva-dark-text) !important; }
.theme-dark .app-main [style*="color:#303133"], .theme-dark .app-main [style*="color: #303133"], .theme-dark .app-main [style*="color:#333"], .theme-dark .app-main [style*="color: #333"], .theme-dark .app-main [style*="color:#1f2d3d"], .theme-dark .app-main [style*="color: #1f2d3d"], .theme-dark .app-main [style*="color:#26364f"], .theme-dark .app-main [style*="color: #26364f"], .theme-dark .app-main [style*="color:#172033"], .theme-dark .app-main [style*="color: #172033"] { color: var(--fiva-dark-text) !important; }
.theme-dark .app-main [style*="color:#909399"], .theme-dark .app-main [style*="color: #909399"], .theme-dark .app-main [style*="color:#999"], .theme-dark .app-main [style*="color: #999"], .theme-dark .app-main [style*="color:#718096"], .theme-dark .app-main [style*="color: #718096"], .theme-dark .app-main [style*="color:#53627a"], .theme-dark .app-main [style*="color: #53627a"] { color: var(--fiva-dark-muted) !important; }
.theme-dark .app-main [style*="background:#f5f7fa"], .theme-dark .app-main [style*="background: #f5f7fa"], .theme-dark .app-main [style*="background:#f0f2f5"], .theme-dark .app-main [style*="background: #f0f2f5"], .theme-dark .app-main [style*="background:#f0f9eb"], .theme-dark .app-main [style*="background: #f0f9eb"], .theme-dark .app-main [style*="background:#fdf6ec"], .theme-dark .app-main [style*="background: #fdf6ec"] { background: rgba(255,255,255,.07) !important; color: var(--fiva-dark-text) !important; border-color: var(--fiva-dark-line) !important; }
.theme-dark .profit-panel small, .theme-dark .metric-card small, .theme-dark .profit-subline, .theme-dark .rank-row > div, .theme-dark .token-bar-row, .theme-dark .chat-empty, .theme-dark .chat-empty p { color: var(--fiva-dark-muted) !important; }
.theme-dark .profit-panel strong, .theme-dark .metric-card strong, .theme-dark .invoice-chip strong, .theme-dark .brief-card strong, .theme-dark .card-title span, .theme-dark .rank-row strong, .theme-dark .chat-empty strong, .theme-dark .capability-list span, .theme-dark .chat-bubble strong, .theme-dark .token-bar-row strong { color: var(--fiva-dark-text) !important; }
.theme-dark .capability-list button, .theme-dark .chat-stream, .theme-dark .chat-bubble { border-color: var(--fiva-dark-line) !important; background: linear-gradient(180deg, rgba(15,23,38,.70), rgba(22,32,51,.82)) !important; color: var(--fiva-dark-text) !important; box-shadow: 0 12px 30px rgba(0,0,0,.18) !important; }
.theme-dark .capability-list small { color: var(--fiva-dark-muted) !important; }
.theme-dark .token-track { background: rgba(232,241,255,.12) !important; }
.theme-dark .ai-chat-card, .theme-dark .ai-command-card { color: var(--fiva-dark-text) !important; }
.theme-dark .ai-chat-card .el-card__body, .theme-dark .ai-command-card .el-card__body { color: var(--fiva-dark-text) !important; }
.theme-dark .el-input__inner::placeholder, .theme-dark .el-textarea__inner::placeholder { color: rgba(154,169,191,.76) !important; }
.theme-dark .el-dropdown__popper .el-popper__arrow::before, .theme-dark .el-dropdown-menu { background: #1b2740 !important; border-color: var(--fiva-dark-line) !important; }
.theme-dark .el-dropdown-menu__item { color: var(--fiva-dark-text) !important; }
.theme-dark .el-dropdown-menu__item:hover { background: rgba(32,180,134,.14) !important; color: #dffcf3 !important; }
.theme-dark .el-dialog { background: transparent !important; }
.ai-dialog { --el-dialog-padding-primary: 0; border-radius: 14px !important; background: transparent !important; box-shadow: none !important; animation: aiDialogIn .24s ease-out; }
.ai-dialog .el-dialog__header { display: none; }
.ai-dialog .el-dialog__body { padding: 0 !important; }
.ai-panel { position: relative; height: min(680px, 82vh); display: grid; grid-template-rows: auto 1fr auto; overflow: hidden; border: 1px solid rgba(255,255,255,.18); border-radius: 14px; background: linear-gradient(180deg, rgba(255,255,255,.96), rgba(247,250,252,.96)); box-shadow: 0 32px 90px rgba(9,16,28,.30); }
.ai-panel::before { content: ""; position: absolute; inset: 0 0 auto; height: 180px; pointer-events: none; background: radial-gradient(circle at 18% 0%, rgba(32,180,134,.22), transparent 34%), radial-gradient(circle at 80% 20%, rgba(43,127,255,.20), transparent 32%); }
.theme-dark .ai-panel { border-color: var(--fiva-dark-line); background: linear-gradient(180deg, rgba(22,32,51,.98), rgba(15,23,38,.98)); }
.ai-panel-head { position: relative; display: grid; grid-template-columns: auto 1fr auto; align-items: center; gap: 16px; padding: 24px; color: #fff; background: linear-gradient(135deg, #172033, #1e7d7a 70%, #2b7fff); }
.ai-hero-logo { position: relative; display: grid; width: 74px; height: 74px; place-items: center; border-radius: 18px; background: conic-gradient(from 160deg, #20b486, #2b7fff, #8a5cf6, #20b486); box-shadow: 0 18px 45px rgba(32,180,134,.34); animation: aiFloat 3s ease-in-out infinite; }
.ai-hero-logo::before { content: ""; position: absolute; inset: -10px; border-radius: 24px; background: radial-gradient(circle, rgba(32,180,134,.32), transparent 62%); animation: aiPulse 1.8s ease-in-out infinite; }
.ai-hero-logo span { position: relative; width: 38px; height: 38px; border-radius: 50%; background: #fff; }
.ai-hero-logo i { position: absolute; width: 14px; height: 14px; right: 16px; top: 16px; border-radius: 50%; background: #78f0c2; box-shadow: 0 0 18px #78f0c2; }
.ai-title-block span { font-size: 24px; font-weight: 850; }
.ai-title-block p { margin-top: 8px; color: rgba(232,241,255,.76); font-size: 14px; line-height: 1.6; }
.ai-close { align-self: start; color: #fff !important; }
.ai-messages { overflow: auto; padding: 20px 24px; }
.ai-empty { display: grid; gap: 14px; color: #53627a; overflow: hidden; }
.ai-empty strong { color: #172033; font-size: 15px; }
.ai-question-marquee { position: relative; overflow: hidden; padding: 2px 0 10px; mask-image: linear-gradient(90deg, transparent, #000 8%, #000 92%, transparent); }
.ai-question-track { display: flex; width: max-content; gap: 12px; animation: aiQuestionsRoll 26s linear infinite; }
.ai-question-marquee:hover .ai-question-track { animation-play-state: paused; }
.theme-dark .ai-empty strong { color: var(--fiva-dark-text); }
.ai-empty button { flex: 0 0 auto; min-width: 168px; min-height: 54px; padding: 12px 16px; border: 1px solid rgba(30,125,122,.16); border-radius: 999px; color: #14615f; text-align: left; font-weight: 680; background: linear-gradient(135deg, #fff, #edf8f6); box-shadow: 0 10px 24px rgba(17,32,50,.07); cursor: pointer; transition: transform .2s ease, box-shadow .2s ease; }
.ai-empty button:hover { transform: translateY(-2px) scale(1.03); box-shadow: 0 16px 32px rgba(30,125,122,.16); }
.theme-dark .ai-empty button { border-color: var(--fiva-dark-line); color: #dffcf3; background: rgba(255,255,255,.07); }
.ai-message { display: flex; margin-bottom: 12px; }
.ai-message.user { justify-content: flex-end; }
.ai-bubble { max-width: 78%; padding: 12px 14px; border-radius: 10px; color: #26364f; line-height: 1.75; white-space: pre-wrap; background: #fff; box-shadow: 0 8px 22px rgba(17,32,50,.06); }
.ai-message.user .ai-bubble { color: #fff; background: linear-gradient(135deg, #1e7d7a, #209477); }
.theme-dark .ai-bubble { color: var(--fiva-dark-text); background: rgba(255,255,255,.08); }
.ai-input-row { display: grid; grid-template-columns: 1fr auto; gap: 10px; padding: 16px; border-top: 1px solid rgba(38,54,78,.08); background: rgba(255,255,255,.92); }
.theme-dark .ai-input-row { border-top-color: var(--fiva-dark-line); background: rgba(15,23,38,.92); }
@keyframes aiSpin { to { transform: rotate(360deg); } }
@keyframes aiPulse { 0%,100% { opacity: .55; transform: scale(.92); } 50% { opacity: 1; transform: scale(1.08); } }
@keyframes aiFloat { 0%,100% { transform: translateY(0); } 50% { transform: translateY(-4px); } }
@keyframes aiDialogIn { from { opacity: 0; transform: translateY(12px) scale(.96); } to { opacity: 1; transform: translateY(0) scale(1); } }
@keyframes aiQuestionsRoll { from { transform: translateX(0); } to { transform: translateX(-50%); } }
@media (max-width: 768px) { .app-sidebar { transition: left .25s ease; } .app-sidebar.is-collapsed .brand-copy, .app-sidebar.is-collapsed .sidebar-collapse-btn { opacity: 1; pointer-events: auto; position: static; transform: none; } .app-sidebar.is-collapsed .app-menu .el-menu-item, .app-sidebar.is-collapsed .app-menu .el-sub-menu__title { justify-content: flex-start; padding: 0 20px !important; } .app-header { height: 64px; padding: 0 12px 0 58px; } .eyebrow { display: none; } .current-title { font-size: 17px; } .user-name, .role-pill, .ai-launcher-text { display: none; } .theme-toggle { width: 54px; } .theme-track-icon { display: none; } .theme-toggle.active .theme-thumb { transform: translateX(14px); } .ai-launcher { width: 44px; padding: 0; justify-content: center; } .app-main { padding: 14px !important; } .ai-dialog { width: 94% !important; } .ai-panel { height: 82vh; } .ai-empty { grid-template-columns: 1fr; } }
</style>
