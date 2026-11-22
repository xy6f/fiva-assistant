<template>
  <template v-if="isLoggedIn">
    <button class="mobile-menu-btn" @click="toggleSidebar">
      <el-icon><Fold /></el-icon>
    </button>
    <div class="mobile-overlay" :class="{ 'is-visible': sidebarOpen }" @click="closeSidebar"></div>
  </template>

  <el-container class="app-shell" :class="{ 'theme-dark': isDark }" v-if="isLoggedIn">
    <div class="sidebar-rail" :class="{ 'is-collapsed': sidebarCollapsed }">
    <el-aside :width="sidebarWidth" class="app-sidebar" :class="{ 'is-open': sidebarOpen, 'is-collapsed': sidebarCollapsed }">
      <div class="brand-block">
        <div class="brand-mark">F</div>
        <div class="brand-copy">
          <div class="brand-title">FIVA</div>
          <div class="brand-subtitle">企业财税系统</div>
        </div>
      </div>
      <el-menu class="app-menu" :default-active="activeMenu" router :collapse="sidebarCollapsed" :collapse-transition="false" popper-class="sidebar-popper">
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
          <template #title><el-icon><Notebook /></el-icon><span>账簿管理</span></template>
          <el-menu-item index="/ledger/detail">明细账</el-menu-item>
          <el-menu-item index="/ledger/general">总账</el-menu-item>
          <el-menu-item index="/ledger/trial-balance">科目余额表</el-menu-item>
        </el-sub-menu>
        <el-menu-item index="/period-close" v-if="canSee('/period-close')"><el-icon><RefreshRight /></el-icon><span>期末结转</span></el-menu-item>
        <el-sub-menu index="/tax" v-if="canSee('/tax')">
          <template #title><el-icon><TrendCharts /></el-icon><span>税务</span></template>
          <el-menu-item index="/tax">税务测算</el-menu-item>
          <el-menu-item index="/tax/analysis">税务分析报告</el-menu-item>
        </el-sub-menu>
        <el-menu-item index="/expense" v-if="canSee('/expense')"><el-icon><Ticket /></el-icon><span>费用报销</span></el-menu-item>
        <el-menu-item index="/contact" v-if="canSee('/contact')"><el-icon><User /></el-icon><span>客户/供应商</span></el-menu-item>
        <el-menu-item index="/report" v-if="canSee('/report')"><el-icon><Document /></el-icon><span>财务报表</span></el-menu-item>
        <el-menu-item index="/budget" v-if="canSee('/budget')"><el-icon><Coin /></el-icon><span>预算管理</span></el-menu-item>
        <el-menu-item index="/bank-transaction" v-if="canSee('/bank-transaction')"><el-icon><CreditCard /></el-icon><span>银行流水</span></el-menu-item>
        <el-menu-item index="/fixed-asset" v-if="canSee('/fixed-asset')"><el-icon><OfficeBuilding /></el-icon><span>固定资产</span></el-menu-item>
        <el-menu-item index="/salary" v-if="canSee('/salary')"><el-icon><UserFilled /></el-icon><span>工资管理</span></el-menu-item>
        <el-menu-item index="/chat" v-if="canSee('/chat')"><el-icon><ChatDotRound /></el-icon><span>智能助手</span></el-menu-item>
        <el-menu-item index="/users" v-if="canSee('/users')"><el-icon><Avatar /></el-icon><span>用户管理</span></el-menu-item>
        <el-menu-item index="/company-settings" v-if="canSee('/company-settings')"><el-icon><Setting /></el-icon><span>公司设置</span></el-menu-item>
      </el-menu>
      <button class="sidebar-edge-toggle" type="button" @click="toggleSidebarCollapsed" :aria-label="sidebarCollapsed ? '展开侧栏' : '收起侧栏'" :title="sidebarCollapsed ? '展开侧栏' : '收起侧栏'">
        <el-icon :size="13"><DArrowRight v-if="sidebarCollapsed" /><DArrowLeft v-else /></el-icon>
      </button>
    </el-aside>
    </div>
    <el-container class="workspace">
      <el-header class="app-header">
        <div class="page-context">
          <div class="current-title">{{ pageTitle }}</div>
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
        <div class="ai-messages-wrap">
          <div class="ai-messages" ref="aiMessagesRef" @scroll.passive="onAiScroll">
            <div v-if="aiMessages.length === 0" class="ai-empty">
              <strong>选择一个问题开始</strong>
              <div class="ai-question-marquee">
                <div class="ai-question-track">
                  <button v-for="(q, index) in rollingQuestions" :key="`${q}-${index}`" type="button" @click="sendQuick(q)">{{ q }}</button>
                </div>
              </div>
            </div>
            <div v-for="(msg, index) in aiMessages" :key="index" class="ai-message" :class="msg.role">
              <div v-if="msg.role === 'assistant' && msg.thinking && !msg.content" class="thinking-bubble">
                <span class="thinking-dots"><i></i><i></i><i></i></span>
                <span class="thinking-text">{{ msg.thinkingText || '正在思考…' }}</span>
              </div>
              <div v-else-if="msg.role === 'assistant'" class="assistant-text md-content" v-html="msg.html"></div>
              <div v-else class="ai-bubble">{{ msg.content }}</div>
              <span v-if="msg.streaming && msg.content" class="typing-cursor"></span>
              <span v-if="msg.stopped" class="stopped-tag">已中断</span>
              <div v-if="msg.role === 'assistant' && msg.toolSteps?.length" class="tool-status-line">
                <span v-if="msg.streaming" class="thinking-dots"><i></i><i></i><i></i></span>
                <span>{{ msg.streaming ? ('正在' + msg.toolSteps[msg.toolSteps.length - 1] + '…') : ('已查询: ' + msg.toolSteps.join('、')) }}</span>
              </div>
            </div>
          </div>
          <button v-if="aiShowScrollBtn" class="scroll-bottom-btn" type="button" aria-label="回到底部" title="回到底部" @click="scrollAiToBottomForce">
            <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"><path d="M12 5v14" /><path d="M19 12l-7 7-7-7" /></svg>
          </button>
        </div>
        <div class="ai-input-row">
          <el-input v-model="aiInput" placeholder="输入问题，例如：分析本月利润变化" @keyup.enter="sendAiMessage" />
          <button
            class="send-toggle"
            :class="{ 'is-active': !!aiInput.trim() && !aiLoading, 'is-stop': aiLoading }"
            type="button"
            :aria-label="aiLoading ? '停止生成' : '发送'"
            :title="aiLoading ? '停止生成' : '发送'"
            @click="onSendToggle"
          >
            <svg v-if="!aiLoading" viewBox="0 0 24 24" width="19" height="19" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"><path d="M12 19V5" /><path d="M5 12l7-7 7 7" /></svg>
            <span v-else class="stop-square"></span>
          </button>
        </div>
      </div>
    </el-dialog>
  </el-container>
  <router-view v-else />
</template>

<script setup>
import { ref, computed, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { streamChat } from './api/chat'
import { renderMarkdown } from './utils/markdown'

const route = useRoute()
const router = useRouter()
const activeMenu = computed(() => route.path)
const pageTitle = computed(() => route.meta.title || '财务工作台')
const sidebarOpen = ref(false)
const sidebarCollapsed = ref(false)
const isLoggedIn = ref(!!localStorage.getItem('token'))
const isDark = ref(localStorage.getItem('theme') === 'dark')
const aiDrawerOpen = ref(false)
const aiInput = ref('')
const aiLoading = ref(false)
const aiMessages = ref([])
const aiSessionId = ref(localStorage.getItem('chat_session') || '')
const aiMessagesRef = ref(null)
const aiAbortController = ref(null)
// 自动跟随:用户在底部时才随流式滚动;上翻阅读历史时暂停跟随并显示"回到底部"按钮
const aiAutoFollow = ref(true)
const aiShowScrollBtn = ref(false)

function onAiScroll() {
  const el = aiMessagesRef.value
  if (!el) return
  const dist = el.scrollHeight - el.scrollTop - el.clientHeight
  aiAutoFollow.value = dist < 80
  aiShowScrollBtn.value = !aiAutoFollow.value
}
function scrollAiToBottomForce() {
  aiAutoFollow.value = true
  aiShowScrollBtn.value = false
  nextTick(() => {
    if (aiMessagesRef.value) aiMessagesRef.value.scrollTo({ top: aiMessagesRef.value.scrollHeight, behavior: 'smooth' })
  })
}
const quickQuestions = ['分析本月利润表现', '提示税务风险', '生成经营摘要', '检查发票异常', '预测现金流压力', '给出节税建议', '总结费用波动', '生成经营简报']
const rollingQuestions = computed(() => [...quickQuestions, ...quickQuestions])
const sidebarWidth = computed(() => (sidebarCollapsed.value ? '72px' : '248px'))

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
}
function applyTheme() {
  const dark = isLoggedIn.value && isDark.value
  document.documentElement.classList.toggle('theme-dark', dark)
  window.dispatchEvent(new CustomEvent('fiva-theme-change', { detail: dark }))
}
function toggleTheme() {
  isDark.value = !isDark.value
  localStorage.setItem('theme', isDark.value ? 'dark' : 'light')
}
watch([isDark, isLoggedIn], applyTheme, { immediate: true })
function openAiDrawer(seedQuestion = '') {
  aiDrawerOpen.value = true
  if (typeof seedQuestion === 'string' && seedQuestion) {
    aiInput.value = seedQuestion
  }
}
function scrollAiToBottom() {
  if (!aiAutoFollow.value) return
  nextTick(() => {
    if (aiMessagesRef.value) aiMessagesRef.value.scrollTop = aiMessagesRef.value.scrollHeight
  })
}
function sendQuick(question) {
  aiInput.value = question
  sendAiMessage()
}
function finalizeAiInterrupted() {
  const last = aiMessages.value[aiMessages.value.length - 1]
  if (last && last.role === 'assistant' && last.streaming) {
    last.streaming = false
    last.thinking = false
    last.stopped = true
    if (!last.content) last.content = '（已停止生成）'
    last.html = renderMarkdown(last.content)
  }
  aiLoading.value = false
  aiAbortController.value = null
  scrollAiToBottom()
}

/** 中断当前正在生成的回复 */
function stopAiGeneration() {
  if (aiAbortController.value) aiAbortController.value.abort()
  finalizeAiInterrupted()
}

/** 输入行右侧的主按钮：未在生成时是发送键，生成中变为方形停止键（与 DeepSeek/ChatGLM 一致） */
function onSendToggle() {
  if (aiLoading.value) {
    stopAiGeneration()
    return
  }
  sendAiMessage()
}

function sendAiMessage() {
  const message = aiInput.value.trim()
  // 生成中回车不发送（与主流 agent 一致）；中断请点击方形停止键
  if (!message || aiLoading.value) return
  aiAutoFollow.value = true
  aiShowScrollBtn.value = false
  aiInput.value = ''
  aiMessages.value.push({ role: 'user', content: message })
  aiMessages.value.push({ role: 'assistant', content: '', html: '', streaming: true, thinking: true, thinkingText: '' })
  const idx = aiMessages.value.length - 1
  aiLoading.value = true
  scrollAiToBottom()
  aiAbortController.value = streamChat(aiSessionId.value, message, {
    onSession(sessionId) {
      aiSessionId.value = sessionId
      localStorage.setItem('chat_session', sessionId)
    },
    onTool(label) {
      // 工具调用阶段的实时进度:思考气泡显示"正在xx…",完成后保留"已查询: xx、xx"记录
      const msg = aiMessages.value[idx]
      if (!msg) return
      msg.toolSteps = [...(msg.toolSteps || []), label]
      msg.thinkingText = '正在' + label + '…'
      scrollAiToBottom()
    },
    onToken(token) {
      const msg = aiMessages.value[idx]
      if (msg.thinking) msg.thinking = false
      msg.content += token
      msg.html = renderMarkdown(msg.content)
      scrollAiToBottom()
    },
    onDone() {
      const msg = aiMessages.value[idx]
      msg.streaming = false
      msg.thinking = false
      aiLoading.value = false
      aiAbortController.value = null
    },
    onError(error) {
      const safe = /SocketException|ConnectException|refused|timeout|reset/i.test(error)
        ? '助手暂时不可用，请稍后再试'
        : `助手暂时不可用：${error}`
      const msg = aiMessages.value[idx]
      msg.content = msg.content || safe
      msg.html = renderMarkdown(msg.content)
      msg.streaming = false
      msg.thinking = false
      aiLoading.value = false
      aiAbortController.value = null
      scrollAiToBottom()
    }
  })
}

</script>

<style>
* { margin: 0; padding: 0; box-sizing: border-box; }
html.theme-dark {
  --fiva-bg: #101826;
  --fiva-surface: #1b2740;
  --fiva-surface-soft: #162033;
  --fiva-text: #edf4ff;
  --fiva-muted: #9aa9bf;
  --fiva-line: rgba(220,232,255,.12);
  --fiva-shadow: 0 18px 45px rgba(0,0,0,.24);
  --fiva-dark-bg: #0f1726;
  --fiva-dark-panel: #162033;
  --fiva-dark-card: #1b2740;
  --fiva-dark-soft: #22314d;
  --fiva-dark-line: rgba(220,232,255,.12);
  --fiva-dark-text: #edf4ff;
  --fiva-dark-muted: #9aa9bf;
  color: var(--fiva-dark-text);
  background: #101826;
}
html.theme-dark, html.theme-dark body, html.theme-dark #app { background: #101826; color: var(--fiva-dark-text); }
.app-shell { min-height: 100vh; position: relative; background: radial-gradient(circle at 82% 8%, rgba(26,107,104,.12), transparent 30%), linear-gradient(180deg, #f7f9fc 0%, #edf2f7 100%); }
.app-shell.theme-dark { background: radial-gradient(circle at 80% 4%, rgba(26,107,104,.16), transparent 28%), linear-gradient(180deg, #0f1726 0%, #131d2f 100%); color: var(--fiva-dark-text); }
.sidebar-rail { position: sticky; top: 0; z-index: 30; flex: 0 0 248px; width: 248px; height: 100vh; align-self: flex-start; overflow: visible; transition: flex-basis .22s ease, width .22s ease; }
.sidebar-rail.is-collapsed { flex-basis: 72px; width: 72px; }
.app-sidebar { position: relative; display: flex; flex-direction: column; height: 100%; overflow: visible !important; border-right: 1px solid rgba(255,255,255,.12); background: linear-gradient(180deg, #121a2a 0%, #172033 48%, #102c2d 100%); box-shadow: 18px 0 45px rgba(16,32,48,.12); }
.app-sidebar::after { content: ""; position: absolute; inset: auto 18px 18px; height: 1px; background: linear-gradient(90deg, transparent, rgba(255,255,255,.24), transparent); }
.brand-block { position: relative; display: flex; align-items: center; justify-content: center; gap: 12px; min-height: 84px; padding: 22px 18px 16px; }
.brand-mark { display: grid; width: 42px; height: 42px; place-items: center; flex-shrink: 0; border-radius: 8px; color: #fff; font-size: 22px; font-weight: 800; background: linear-gradient(135deg, #1a6b68, #209477); box-shadow: 0 12px 28px rgba(26,107,104,.28); }
.brand-title { color: #fff; font-size: 16px; font-weight: 700; }
.brand-subtitle { margin-top: 3px; color: rgba(232,241,255,.58); font-size: 12px; }
.brand-copy { min-width: 0; text-align: left; white-space: nowrap; }
.sidebar-edge-toggle {
  position: absolute;
  top: 50%;
  left: 100%;
  z-index: 40;
  width: 30px;
  height: 30px;
  padding: 0;
  border: 1px solid rgba(255,255,255,.16);
  border-radius: 50%;
  color: rgba(255,255,255,.92);
  background: linear-gradient(180deg, #223050, #172233);
  box-shadow: 0 6px 18px rgba(9,16,28,.38), inset 0 1px 0 rgba(255,255,255,.10);
  backdrop-filter: blur(6px);
  transform: translate(-50%, -50%);
  cursor: pointer;
  display: grid;
  place-items: center;
  opacity: .85;
  transition: transform .22s cubic-bezier(.34,1.4,.44,1), background .22s ease, box-shadow .22s ease, opacity .22s ease, border-color .22s ease;
}
.sidebar-edge-toggle:hover {
  opacity: 1;
  border-color: transparent;
  background: linear-gradient(135deg, #1a6b68, #209477);
  box-shadow: 0 10px 26px rgba(26,107,104,.45), inset 0 1px 0 rgba(255,255,255,.18);
  transform: translate(-50%, -50%) scale(1.14);
}
.sidebar-edge-toggle:active { transform: translate(-50%, -50%) scale(1.02); }
.sidebar-edge-toggle:focus-visible { outline: 2px solid rgba(120,240,194,.7); outline-offset: 2px; }
.sidebar-edge-toggle .el-icon { transition: transform .22s ease; }
.app-sidebar.is-collapsed .brand-block { padding: 18px 0 12px; }
.app-sidebar.is-collapsed .brand-copy { display: none; }
.app-sidebar.is-collapsed .brand-mark { width: 42px; height: 42px; }
.sidebar-rail.is-collapsed:hover .app-sidebar { box-shadow: 18px 0 45px rgba(16,32,48,.22), 4px 0 18px rgba(26,107,104,.14); }
.app-sidebar.is-collapsed .app-menu .el-menu-item,
.app-sidebar.is-collapsed .app-menu .el-sub-menu__title { transition: background .18s ease, box-shadow .18s ease; }
.app-sidebar.is-collapsed .app-menu .el-menu-item .el-icon,
.app-sidebar.is-collapsed .app-menu .el-sub-menu__title .el-icon { transition: transform .18s ease, color .18s ease; }
.app-sidebar.is-collapsed .app-menu .el-menu-item:hover,
.app-sidebar.is-collapsed .app-menu .el-sub-menu__title:hover { background: rgba(255,255,255,.12); box-shadow: 0 6px 16px rgba(9,16,28,.28), inset 0 0 0 1px rgba(255,255,255,.08); }
.app-sidebar.is-collapsed .app-menu .el-menu-item:hover .el-icon,
.app-sidebar.is-collapsed .app-menu .el-sub-menu__title:hover .el-icon { transform: scale(1.18); color: #78f0c2; }
.app-sidebar.is-collapsed .app-menu .el-menu-item.is-active { box-shadow: 0 10px 22px rgba(26,107,104,.34), inset 0 0 0 1px rgba(255,255,255,.14); }
.workspace { min-width: 0; }
.app-main > div, .app-main > section { width: 100%; margin-left: auto; margin-right: auto; }
.sidebar-rail.is-collapsed + .workspace .app-main > div, .sidebar-rail.is-collapsed + .workspace .app-main > section { max-width: 1320px; }
.app-header { height: 72px; display: flex; align-items: center; justify-content: space-between; padding: 0 28px; border-bottom: 1px solid rgba(38,54,78,.08); background: rgba(255,255,255,.82); backdrop-filter: blur(16px); }
.theme-dark .app-header { border-bottom-color: var(--fiva-dark-line); background: rgba(15,23,38,.86); }
.page-context { min-width: 0; }
.current-title { color: #172033; font-size: 20px; font-weight: 700; }
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
.theme-toggle.active { border-color: rgba(26,107,104,.30); background: linear-gradient(135deg, #172033, #20324f); }
.theme-track-icon { position: absolute; top: 50%; transform: translateY(-50%); display: grid; width: 28px; height: 28px; place-items: center; color: rgba(23,32,51,.48); font-size: 16px; }
.theme-track-icon.sun { left: 7px; color: #c98716; }
.theme-track-icon.moon { right: 7px; color: #9aa9bf; }
.theme-thumb { position: absolute; top: 5px; left: 5px; display: grid; width: 32px; height: 32px; place-items: center; border-radius: 50%; color: #fff; background: linear-gradient(135deg, #f2a51a, #f6c45c); box-shadow: 0 8px 18px rgba(201,135,22,.28); transition: transform .25s ease, background .25s ease, box-shadow .25s ease; }
.theme-toggle.active .theme-thumb { transform: translateX(36px); background: linear-gradient(135deg, #1a6b68, #2b5f8a); box-shadow: 0 8px 20px rgba(26,107,104,.34); }
.profile-button { display: flex; align-items: center; gap: 9px; height: 40px; padding: 0 10px 0 4px; border: 1px solid rgba(38,54,78,.10); border-radius: 999px; background: rgba(255,255,255,.84); cursor: pointer; }
.user-avatar { display: grid; width: 34px; height: 34px; place-items: center; border-radius: 50%; color: #fff; font-weight: 800; background: linear-gradient(135deg, #172033, #1a6b68 58%, #209477); box-shadow: 0 8px 20px rgba(26,107,104,.22); }
.user-name { color: #2d3748; font-size: 14px; font-weight: 650; }
.profile-caret { color: #718096; }
.app-main { min-width: 0; padding: 26px 28px 32px; }
.theme-dark .app-main { color: var(--fiva-dark-text); }
.theme-dark .ai-launcher { border-color: rgba(32,180,134,.28); color: #dffcf3; background: linear-gradient(135deg, rgba(32,180,134,.18), rgba(43,127,255,.14)); box-shadow: 0 14px 34px rgba(0,0,0,.22); }
.theme-dark .user-name { color: var(--fiva-dark-text); }
.theme-dark .profile-caret { color: var(--fiva-dark-muted); }
.theme-dark .profile-button { border-color: var(--fiva-dark-line) !important; background: rgba(255,255,255,.08) !important; }
.theme-dark .el-card, .theme-dark .stat-card, .theme-dark .kpi-card, .theme-dark .mini-card, .theme-dark .ai-insight-card { background: linear-gradient(180deg, rgba(27,39,64,.94), rgba(22,32,51,.94)) !important; border-color: var(--fiva-dark-line) !important; color: var(--fiva-dark-text) !important; box-shadow: 0 18px 45px rgba(0,0,0,.24) !important; }
.theme-dark .el-card__header { background: rgba(15,23,38,.72) !important; border-bottom-color: var(--fiva-dark-line) !important; color: var(--fiva-dark-text) !important; }
.theme-dark .app-main h2, .theme-dark .app-main h3, .theme-dark .card-title span, .theme-dark .kpi-card strong, .theme-dark .rank-row strong, .theme-dark .insight-head span { color: var(--fiva-dark-text) !important; }
.theme-dark .app-main p, .theme-dark .card-title small, .theme-dark .kpi-card span, .theme-dark .kpi-card small, .theme-dark .mini-card span, .theme-dark .rank-row span, .theme-dark .ai-insight-card p { color: var(--fiva-dark-muted) !important; }
.theme-dark .el-table { --el-table-bg-color: #162033; --el-table-tr-bg-color: #162033; --el-table-header-bg-color: #101827; --el-table-row-hover-bg-color: rgba(26,107,104,.16); --el-table-border-color: var(--fiva-dark-line); color: var(--fiva-dark-text); }
.theme-dark .el-table th.el-table__cell { background: #101827 !important; color: #b6c5dc !important; }
.theme-dark .el-table td.el-table__cell { border-bottom-color: var(--fiva-dark-line) !important; }
.theme-dark .el-input__wrapper, .theme-dark .el-select__wrapper, .theme-dark .el-textarea__inner, .theme-dark .el-input-number .el-input__wrapper { background: rgba(15,23,38,.78) !important; box-shadow: 0 0 0 1px var(--fiva-dark-line) inset !important; color: var(--fiva-dark-text) !important; }
.theme-dark .el-input__inner, .theme-dark .el-textarea__inner, .theme-dark .el-select__placeholder, .theme-dark .el-input-number__increase, .theme-dark .el-input-number__decrease { color: var(--fiva-dark-text) !important; }
.theme-dark .el-progress-bar__outer { background-color: rgba(232,241,255,.12) !important; }
.theme-dark .el-button.is-plain, .theme-dark .el-button--default { border-color: var(--fiva-dark-line) !important; background: rgba(255,255,255,.07) !important; color: var(--fiva-dark-text) !important; }
.theme-dark .el-tag { border-color: rgba(120,240,194,.22) !important; background: rgba(26,107,104,.18) !important; color: #dffcf3 !important; }
.theme-dark .el-select-dropdown, .theme-dark .el-picker-panel, .theme-dark .el-message-box { border-color: var(--fiva-dark-line) !important; background: #1b2740 !important; color: var(--fiva-dark-text) !important; }
.theme-dark .dashboard-hero { border-color: var(--fiva-dark-line); background: linear-gradient(135deg, #111a2b, #13293b 52%, #145754); box-shadow: 0 20px 55px rgba(0,0,0,.28); }
.theme-dark .dashboard-hero h2, .theme-dark .intel-copy h2, .theme-dark .intel-score strong { color: #fff !important; }
.theme-dark .intel-copy p, .theme-dark .intel-score span, .theme-dark .intel-score small { color: rgba(232,241,255,.76) !important; }
.theme-dark .intel-hero { border-color: rgba(255,255,255,.12); background: radial-gradient(circle at 16% 12%, rgba(120,240,194,.30), transparent 28%), linear-gradient(135deg, #111a2b, #17333a 58%, #1a6b68) !important; color: #fff !important; }
.theme-dark .intel-score { background: rgba(255,255,255,.12) !important; color: #fff !important; }
.theme-dark .profit-panel, .theme-dark .metric-card, .theme-dark .invoice-strip, .theme-dark .invoice-chip, .theme-dark .brief-card, .theme-dark .radar-side, .theme-dark .intel-card { border-color: var(--fiva-dark-line) !important; background: var(--fiva-dark-card) !important; color: var(--fiva-dark-text) !important; }
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
.theme-dark .capability-list button, .theme-dark .chat-stream, .theme-dark .chat-bubble { border-color: var(--fiva-dark-line) !important; background: rgba(15,23,38,.70) !important; color: var(--fiva-dark-text) !important; box-shadow: none !important; }
.theme-dark .chat-message.user .chat-bubble, .theme-dark .ai-message.user .ai-bubble { color: #fff !important; background: linear-gradient(135deg, #1a6b68, #209477) !important; }
.theme-dark .capability-list small { color: var(--fiva-dark-muted) !important; }
.theme-dark .token-track { background: rgba(232,241,255,.12) !important; }
.theme-dark .ai-chat-card, .theme-dark .ai-command-card { color: var(--fiva-dark-text) !important; }
.theme-dark .ai-chat-card .el-card__body, .theme-dark .ai-command-card .el-card__body { color: var(--fiva-dark-text) !important; }
.theme-dark .el-input__inner::placeholder, .theme-dark .el-textarea__inner::placeholder { color: rgba(154,169,191,.76) !important; }
.theme-dark .el-dropdown__popper .el-popper__arrow::before, .theme-dark .el-dropdown-menu { background: #1b2740 !important; border-color: var(--fiva-dark-line) !important; }
.theme-dark .el-dropdown-menu__item { color: var(--fiva-dark-text) !important; }
.theme-dark .el-dropdown-menu__item:hover { background: rgba(26,107,104,.18) !important; color: #dffcf3 !important; }
.theme-dark .el-dialog.ai-dialog { background: transparent !important; box-shadow: none !important; }
html.theme-dark .el-popper.is-light,
html.theme-dark .el-select-dropdown,
html.theme-dark .el-picker-panel,
html.theme-dark .el-dropdown-menu,
html.theme-dark .el-popover.el-popper,
html.theme-dark .el-autocomplete-suggestion,
html.theme-dark .el-message-box {
  background: #1b2740 !important;
  border-color: var(--fiva-dark-line) !important;
  color: var(--fiva-dark-text) !important;
}
html.theme-dark .el-select-dropdown__item,
html.theme-dark .el-dropdown-menu__item,
html.theme-dark .el-picker-panel__icon-btn,
html.theme-dark .el-date-picker__header-label,
html.theme-dark .el-date-table th {
  color: var(--fiva-dark-text) !important;
}
html.theme-dark .el-select-dropdown__item.is-hovering,
html.theme-dark .el-select-dropdown__item.hover,
html.theme-dark .el-dropdown-menu__item:hover {
  background: rgba(26,107,104,.18) !important;
  color: #dffcf3 !important;
}
html.theme-dark .el-popper.is-light .el-popper__arrow::before {
  background: #1b2740 !important;
  border-color: var(--fiva-dark-line) !important;
}
.ai-dialog { --el-dialog-padding-primary: 0; border-radius: 14px !important; background: transparent !important; box-shadow: none !important; animation: aiDialogIn .24s ease-out; }
.ai-dialog .el-dialog__header { display: none; }
.ai-dialog .el-dialog__body { padding: 0 !important; }
.ai-panel { position: relative; height: min(680px, 82vh); display: grid; grid-template-rows: auto 1fr auto; overflow: hidden; border: 1px solid rgba(255,255,255,.18); border-radius: 14px; background: linear-gradient(180deg, rgba(255,255,255,.96), rgba(247,250,252,.96)); box-shadow: 0 32px 90px rgba(9,16,28,.30); }
.ai-panel::before { content: ""; position: absolute; inset: 0 0 auto; height: 180px; pointer-events: none; background: radial-gradient(circle at 18% 0%, rgba(26,107,104,.22), transparent 34%), radial-gradient(circle at 80% 20%, rgba(43,95,138,.20), transparent 32%); }
.theme-dark .ai-panel { border-color: var(--fiva-dark-line); background: linear-gradient(180deg, rgba(22,32,51,.98), rgba(15,23,38,.98)); }
.ai-panel-head { position: relative; display: grid; grid-template-columns: auto 1fr auto; align-items: center; gap: 16px; padding: 24px; color: #fff; background: linear-gradient(135deg, #172033, #1a6b68 70%, #209477); }
.ai-hero-logo { position: relative; display: grid; width: 74px; height: 74px; place-items: center; border-radius: 18px; background: conic-gradient(from 160deg, #1a6b68, #209477, #2b5f8a, #1a6b68); box-shadow: 0 18px 45px rgba(26,107,104,.34); animation: aiFloat 3s ease-in-out infinite; }
.ai-hero-logo::before { content: ""; position: absolute; inset: -10px; border-radius: 24px; background: radial-gradient(circle, rgba(26,107,104,.32), transparent 62%); animation: aiPulse 1.8s ease-in-out infinite; }
.ai-hero-logo span { position: relative; width: 38px; height: 38px; border-radius: 50%; background: #fff; }
.ai-hero-logo i { position: absolute; width: 14px; height: 14px; right: 16px; top: 16px; border-radius: 50%; background: #78f0c2; box-shadow: 0 0 18px #78f0c2; }
.ai-title-block span { font-size: 24px; font-weight: 800; }
.ai-title-block p { margin-top: 8px; color: rgba(232,241,255,.76); font-size: 14px; line-height: 1.6; }
.ai-close { align-self: start; color: #fff !important; }
.ai-messages-wrap { position: relative; display: grid; min-height: 0; }
.ai-messages { overflow: auto; height: 100%; padding: 20px 24px; }
.scroll-bottom-btn { position: absolute; right: 16px; bottom: 14px; z-index: 6; display: grid; place-items: center; width: 34px; height: 34px; padding: 0; border: 1px solid rgba(38,54,78,.14); border-radius: 50%; color: #26364f; background: rgba(255,255,255,.95); box-shadow: 0 8px 22px rgba(17,32,50,.18); cursor: pointer; transition: transform .15s ease, color .2s ease; }
.scroll-bottom-btn:hover { transform: translateY(-2px); color: #1a6b68; }
.theme-dark .scroll-bottom-btn { border-color: var(--fiva-dark-line); color: var(--fiva-dark-text); background: rgba(27,39,64,.95); }
.ai-empty { display: grid; gap: 14px; color: #53627a; overflow: hidden; }
.ai-empty strong { color: #172033; font-size: 15px; }
.ai-question-marquee { position: relative; overflow: hidden; padding: 2px 0 10px; mask-image: linear-gradient(90deg, transparent, #000 8%, #000 92%, transparent); }
.ai-question-track { display: flex; width: max-content; gap: 12px; animation: aiQuestionsRoll 26s linear infinite; }
.ai-question-marquee:hover .ai-question-track { animation-play-state: paused; }
.theme-dark .ai-empty strong { color: var(--fiva-dark-text); }
.ai-empty button { flex: 0 0 auto; min-width: 168px; min-height: 54px; padding: 12px 16px; border: 1px solid rgba(26,107,104,.16); border-radius: 999px; color: #145754; text-align: left; font-weight: 680; background: linear-gradient(135deg, #fff, #edf8f6); box-shadow: 0 10px 24px rgba(17,32,50,.07); cursor: pointer; transition: transform .2s ease, box-shadow .2s ease; }
.ai-empty button:hover { transform: translateY(-2px) scale(1.03); box-shadow: 0 16px 32px rgba(26,107,104,.16); }
.theme-dark .ai-empty button { border-color: var(--fiva-dark-line); color: #dffcf3; background: rgba(255,255,255,.07); }
.ai-message { display: flex; align-items: flex-end; gap: 6px; margin-bottom: 12px; }
.ai-message.user { justify-content: flex-end; }
.ai-bubble { max-width: 76%; padding: 10px 14px; border-radius: 14px; color: #fff; line-height: 1.7; white-space: pre-wrap; background: linear-gradient(135deg, #1a6b68, #209477); }
.assistant-text { flex: 1; min-width: 0; padding: 2px 4px; }
/* 抽屉内正文排版:与智能助手页同一套规则(助手消息平铺,用户消息气泡) */
.ai-messages .md-content { color: #26364f; font-size: 14.5px; line-height: 1.8; word-break: break-word; }
.ai-messages .md-content > *:first-child { margin-top: 0; }
.ai-messages .md-content > *:last-child { margin-bottom: 0; }
.ai-messages .md-content p { margin: 8px 0; }
.ai-messages .md-content strong { color: inherit; font-weight: 700; }
.ai-messages .md-content h1, .ai-messages .md-content h2, .ai-messages .md-content h3, .ai-messages .md-content h4 { margin: 18px 0 8px; color: #172033; font-weight: 700; line-height: 1.45; }
.ai-messages .md-content h1 { font-size: 18px; }
.ai-messages .md-content h2 { font-size: 16.5px; }
.ai-messages .md-content h3 { font-size: 15px; }
.ai-messages .md-content h4 { font-size: 14.5px; }
.ai-messages .md-content ul, .ai-messages .md-content ol { margin: 8px 0; padding-left: 24px; }
.ai-messages .md-content li { margin: 5px 0; }
.ai-messages .md-content li::marker { color: #7c9a97; font-weight: 600; }
.ai-messages .md-content code { background: rgba(26,107,104,.09); padding: 2px 6px; border-radius: 5px; font-size: .88em; font-family: 'JetBrains Mono', Consolas, 'Courier New', monospace; color: #145754; }
.ai-messages .md-content pre { background: #101828; color: #dbe5f1; padding: 14px 16px; border-radius: 10px; overflow-x: auto; margin: 10px 0; line-height: 1.65; }
.ai-messages .md-content pre code { background: none; padding: 0; color: inherit; font-size: 13px; }
.ai-messages .md-content table { width: 100%; margin: 12px 0; font-size: 13.5px; line-height: 1.6; border-collapse: separate; border-spacing: 0; border: 1px solid rgba(38,54,78,.14); border-radius: 10px; overflow: hidden; }
.ai-messages .md-content th { background: rgba(26,107,104,.08); color: #172033; font-weight: 700; text-align: left; padding: 9px 14px; border-bottom: 1px solid rgba(38,54,78,.16); }
.ai-messages .md-content td { padding: 9px 14px; border-bottom: 1px solid rgba(38,54,78,.08); }
.ai-messages .md-content th:first-child { border-top-left-radius: 9px; }
.ai-messages .md-content th:last-child { border-top-right-radius: 9px; }
.ai-messages .md-content tbody tr:last-child td { border-bottom: none; }
.ai-messages .md-content tbody tr:last-child td:first-child { border-bottom-left-radius: 9px; }
.ai-messages .md-content tbody tr:last-child td:last-child { border-bottom-right-radius: 9px; }
.ai-messages .md-content blockquote { margin: 10px 0; padding: 9px 14px; border-left: 3px solid #1a6b68; background: rgba(26,107,104,.06); border-radius: 0 8px 8px 0; color: #53627a; }
.ai-messages .md-content blockquote p { margin: 4px 0; }
.ai-messages .md-content hr { border: none; height: 1px; background: rgba(38,54,78,.12); margin: 16px 0; }
.ai-message.user .ai-bubble { color: #fff; background: linear-gradient(135deg, #1a6b68, #209477); }
.theme-dark .ai-message.user .ai-bubble { color: #fff; background: linear-gradient(135deg, #1a6b68, #209477); }
.theme-dark .ai-messages .md-content { color: var(--fiva-dark-text); }
.theme-dark .ai-messages .md-content h1, .theme-dark .ai-messages .md-content h2, .theme-dark .ai-messages .md-content h3, .theme-dark .ai-messages .md-content h4 { color: var(--fiva-dark-text); }
.theme-dark .ai-messages .md-content code { background: rgba(120,240,194,.10); color: #9be8cf; }
.theme-dark .ai-messages .md-content pre { background: #0b1120; }
.theme-dark .ai-messages .md-content table { border-color: var(--fiva-dark-line); }
.theme-dark .ai-messages .md-content th { background: rgba(26,107,104,.18); color: var(--fiva-dark-text); border-bottom-color: var(--fiva-dark-line); }
.theme-dark .ai-messages .md-content td { border-bottom-color: var(--fiva-dark-line); }
.theme-dark .ai-messages .md-content blockquote { background: rgba(26,107,104,.10); color: var(--fiva-dark-muted); }
.theme-dark .ai-messages .md-content hr { background: var(--fiva-dark-line); }
.theme-dark .thinking-bubble { color: var(--fiva-dark-muted); }
.typing-cursor { flex-shrink: 0; width: 2px; height: 18px; background: #1a6b68; animation: aiCursorBlink 1s infinite; }
.thinking-bubble { display: inline-flex; align-items: center; gap: 9px; padding: 6px 2px; color: #718096; }
.thinking-dots { display: inline-flex; gap: 4px; }
.thinking-dots i { width: 7px; height: 7px; border-radius: 50%; background: #209477; animation: thinkingBounce 1.2s ease-in-out infinite; }
.thinking-dots i:nth-child(2) { background: #1a6b68; animation-delay: .15s; }
.thinking-dots i:nth-child(3) { background: #2b5f8a; animation-delay: .3s; }
.thinking-text { font-size: 13px; letter-spacing: .5px; }
.stopped-tag { flex-shrink: 0; margin-bottom: 4px; padding: 2px 8px; border: 1px solid rgba(38,54,78,.16); border-radius: 999px; color: #718096; font-size: 11px; }
.tool-status-line { display: inline-flex; align-items: center; gap: 8px; margin: 6px 0 0 4px; color: #718096; font-size: 12.5px; }
.tool-status-line .thinking-dots i { width: 5px; height: 5px; }
.theme-dark .tool-status-line { color: var(--fiva-dark-muted); }
.theme-dark .thinking-bubble { color: var(--fiva-dark-muted); }
.theme-dark .stopped-tag { border-color: var(--fiva-dark-line); color: var(--fiva-dark-muted); }
.ai-input-row { display: flex; gap: 10px; align-items: center; padding: 16px; border-top: 1px solid rgba(38,54,78,.08); background: rgba(255,255,255,.92); }
.ai-input-row .el-input { flex: 1; }
.ai-input-row .el-button { flex-shrink: 0; }
.send-toggle { flex-shrink: 0; display: grid; place-items: center; width: 38px; height: 38px; padding: 0; border: none; border-radius: 50%; cursor: pointer; color: #fff; background: linear-gradient(135deg, #1a6b68, #209477); opacity: .45; transition: opacity .18s ease, transform .15s ease, background .2s ease, box-shadow .18s ease; }
.send-toggle:hover { transform: scale(1.07); }
.send-toggle.is-active { opacity: 1; box-shadow: 0 8px 20px rgba(26,107,104,.30); }
.send-toggle.is-stop { opacity: 1; background: #172033; }
.send-toggle.is-stop:hover { background: #22314d; }
.stop-square { width: 12px; height: 12px; border-radius: 2.5px; background: #fff; pointer-events: none; }
.send-toggle svg { pointer-events: none; }
.theme-dark .send-toggle.is-stop { background: #22314d; }
.theme-dark .send-toggle.is-stop:hover { background: #2b3c5c; }
.theme-dark .ai-input-row { border-top-color: var(--fiva-dark-line); background: rgba(15,23,38,.92); }
@keyframes aiSpin { to { transform: rotate(360deg); } }
@keyframes aiCursorBlink { 0%, 50% { opacity: 1; } 51%, 100% { opacity: 0; } }
@keyframes thinkingBounce { 0%, 80%, 100% { transform: translateY(0); opacity: .4; } 40% { transform: translateY(-5px); opacity: 1; } }
@keyframes aiPulse { 0%,100% { opacity: .55; transform: scale(.92); } 50% { opacity: 1; transform: scale(1.08); } }
@keyframes aiFloat { 0%,100% { transform: translateY(0); } 50% { transform: translateY(-4px); } }
@keyframes aiDialogIn { from { opacity: 0; transform: translateY(12px) scale(.96); } to { opacity: 1; transform: translateY(0) scale(1); } }
@keyframes aiQuestionsRoll { from { transform: translateX(0); } to { transform: translateX(-50%); } }
@media (max-width: 768px) {
  .sidebar-rail, .sidebar-rail.is-collapsed { flex: 0 0 0 !important; width: 0 !important; overflow: visible; }
  .sidebar-edge-toggle { display: none; }
  .app-sidebar { transition: left .25s ease; overflow: hidden; }
  .app-header { height: 64px; padding: 0 12px 0 58px; }
  .current-title { font-size: 17px; }
  .user-name, .role-pill, .ai-launcher-text { display: none; }
  .theme-toggle { width: 54px; }
  .theme-track-icon { display: none; }
  .theme-toggle.active .theme-thumb { transform: translateX(14px); }
  .ai-launcher { width: 44px; padding: 0; justify-content: center; }
  .app-main { padding: 14px !important; }
  .ai-dialog { width: 94% !important; }
  .ai-panel { height: 82vh; }
}
</style>
