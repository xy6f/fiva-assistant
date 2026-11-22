<template>
  <div class="ai-intel-page">
    <section class="intel-hero">
      <div class="intel-orb-wrap">
        <div class="intel-orb"><span></span><i></i></div>
        <div class="orbit-dot one"></div>
        <div class="orbit-dot two"></div>
      </div>
      <div class="intel-copy">
        <h2>智能助手</h2>
        <p>就税负、发票、利润与预算执行进行问答，并查看模型调用情况。</p>
      </div>
      <div class="intel-score">
        <span>累计调用成功率</span>
        <strong>{{ successRateText }}</strong>
        <small>平均延迟 {{ avgLatencyText }}</small>
      </div>
    </section>

    <section class="intel-grid">
      <article v-for="item in metrics" :key="item.label" class="intel-card">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <p>{{ item.desc }}</p>
      </article>
    </section>

    <section class="intel-workspace">
      <el-card shadow="never" class="ai-command-card">
        <template #header>
          <div class="card-title"><span>Token 趋势</span></div>
        </template>
        <div class="token-bars">
          <div v-for="item in tokenTrend" :key="item.day" class="token-bar-row">
            <span>{{ item.day }}</span>
            <div class="token-track"><i :style="{ width: item.percent + '%' }"></i></div>
            <strong>{{ item.value }}</strong>
          </div>
        </div>
      </el-card>

      <el-card shadow="never" class="ai-command-card capability-card">
        <template #header>
          <div class="card-title"><span>常用提问</span></div>
        </template>
        <div class="capability-list">
          <button v-for="item in capabilities" :key="item.title" type="button" @click="sendQuick(item.prompt)">
            <span>{{ item.title }}</span>
            <small>{{ item.desc }}</small>
          </button>
        </div>
      </el-card>

      <el-card shadow="never" class="ai-chat-card">
        <template #header>
          <div class="card-title"><span>财税问答</span></div>
        </template>
        <div class="chat-stream-wrap">
          <div ref="messageBoxRef" class="chat-stream" @scroll.passive="onScroll">
            <div v-if="messages.length === 0" class="chat-empty">
              <div class="mini-orb"><span></span></div>
              <strong>选择问题或直接输入</strong>
              <p>可以询问税负、发票风险、利润波动、预算执行和经营摘要。</p>
            </div>
            <div v-for="(msg, idx) in messages" :key="idx" class="chat-message" :class="msg.role">
              <div v-if="msg.role === 'assistant' && msg.thinking && !msg.content" class="thinking-bubble">
                <span class="thinking-dots"><i></i><i></i><i></i></span>
                <span class="thinking-text">{{ msg.thinkingText || '正在思考…' }}</span>
              </div>
              <div v-else-if="msg.role === 'assistant'" class="assistant-text md-content" v-html="renderMarkdown(msg.content)"></div>
              <div v-else class="chat-bubble">{{ msg.content }}</div>
              <span v-if="msg.streaming && msg.content" class="typing-cursor"></span>
              <span v-if="msg.stopped" class="stopped-tag">已中断</span>
              <div v-if="msg.role === 'assistant' && msg.toolSteps?.length" class="tool-status-line">
                <span v-if="msg.streaming" class="thinking-dots"><i></i><i></i><i></i></span>
                <span>{{ msg.streaming ? ('正在' + msg.toolSteps[msg.toolSteps.length - 1] + '…') : ('已查询: ' + msg.toolSteps.join('、')) }}</span>
              </div>
            </div>
          </div>
          <button v-if="showScrollBtn" class="scroll-bottom-btn" type="button" aria-label="回到底部" title="回到底部" @click="scrollToBottomForce">
            <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"><path d="M12 5v14" /><path d="M19 12l-7 7-7-7" /></svg>
          </button>
        </div>
        <div class="chat-input-row">
          <el-input v-model="input" @keyup.enter="send" placeholder="输入财税问题，例如：帮我检查本月发票风险" />
          <button
            class="send-toggle"
            :class="{ 'is-active': !!input.trim() && !loading, 'is-stop': loading }"
            type="button"
            :aria-label="loading ? '停止生成' : '发送'"
            :title="loading ? '停止生成' : '发送'"
            @click="onSendToggle"
          >
            <svg v-if="!loading" viewBox="0 0 24 24" width="19" height="19" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"><path d="M12 19V5" /><path d="M5 12l7-7 7 7" /></svg>
            <span v-else class="stop-square"></span>
          </button>
          <el-button plain @click="clearChat">清空</el-button>
        </div>
      </el-card>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { streamChat, getHistory, clearHistory, getAiMetrics } from '../api/chat'
import { renderMarkdown } from '../utils/markdown'
import { ElMessage } from 'element-plus'

const sessionId = ref(localStorage.getItem('chat_session') || '')
const messages = ref([])
const input = ref('')
const loading = ref(false)
const messageBoxRef = ref(null)
const abortController = ref(null)
// 自动跟随:用户在底部时才随流式滚动;用户上翻阅读历史时暂停跟随并显示"回到底部"按钮
const autoFollow = ref(true)
const showScrollBtn = ref(false)

function onScroll() {
  const el = messageBoxRef.value
  if (!el) return
  const dist = el.scrollHeight - el.scrollTop - el.clientHeight
  autoFollow.value = dist < 80
  showScrollBtn.value = !autoFollow.value
}
function scrollToBottomForce() {
  autoFollow.value = true
  showScrollBtn.value = false
  nextTick(() => {
    if (messageBoxRef.value) messageBoxRef.value.scrollTo({ top: messageBoxRef.value.scrollHeight, behavior: 'smooth' })
  })
}

// ===== AI 指标（真实调用数据）=====
const metricsData = ref({
  totalCalls: 0,
  successRate: 100,
  avgLatencyMs: 0,
  todayTokens: 0
})
const tokenTrendRaw = ref([])

const successRateText = computed(() => `${metricsData.value.successRate ?? 100}%`)
const avgLatencyText = computed(() => {
  const ms = metricsData.value.avgLatencyMs || 0
  return ms >= 1000 ? `${(ms / 1000).toFixed(1)}s` : `${Math.round(ms)}ms`
})
function formatTokens(n) {
  const v = n || 0
  return v >= 1000 ? `${(v / 1000).toFixed(1)}k` : `${v}`
}
const metrics = computed(() => [
  { label: '今日 Token', value: formatTokens(metricsData.value.todayTokens), desc: '来自真实 AI 调用日志' },
  { label: '累计调用', value: `${metricsData.value.totalCalls ?? 0} 次`, desc: '问答/OCR/税务分析合计' },
  { label: '成功率', value: `${metricsData.value.successRate ?? 100}%`, desc: '全部调用成功率' },
  { label: '平均延迟', value: avgLatencyText.value, desc: '端到端响应耗时' }
])
const tokenTrend = computed(() => {
  const rows = tokenTrendRaw.value.map(r => ({ day: r.day, tokens: r.tokens || 0 }))
  const max = Math.max(...rows.map(r => r.tokens), 1)
  return rows.map(r => ({
    day: r.day,
    value: formatTokens(r.tokens),
    percent: Math.round((r.tokens / max) * 100)
  }))
})

async function loadMetrics() {
  try {
    const { data } = await getAiMetrics()
    metricsData.value = {
      totalCalls: data.totalCalls ?? 0,
      successRate: data.successRate ?? 100,
      avgLatencyMs: data.avgLatencyMs ?? 0,
      todayTokens: data.todayTokens ?? 0
    }
    tokenTrendRaw.value = data.tokenTrend || []
  } catch (e) { /* 指标加载失败不影响主功能 */ }
}
onMounted(loadMetrics)
const capabilities = [
  { title: '税负巡检', desc: '识别税额占比波动', prompt: '请帮我做一次税负巡检，重点看税额占比和发票风险。' },
  { title: '凭证摘要', desc: '生成老板可读摘要', prompt: '请把近期凭证和经营数据整理成老板版摘要。' },
  { title: '利润归因', desc: '解释利润变化原因', prompt: '请分析本月利润变化的主要原因，并给出后续建议。' },
  { title: '现金压力', desc: '预测短期现金风险', prompt: '请预测未来一个月现金流压力，并列出关注指标。' }
]

function scrollToBottom() {
  if (!autoFollow.value) return
  nextTick(() => {
    if (messageBoxRef.value) messageBoxRef.value.scrollTop = messageBoxRef.value.scrollHeight
  })
}
function finalizeInterrupted() {
  const last = messages.value[messages.value.length - 1]
  if (last && last.role === 'assistant' && last.streaming) {
    last.streaming = false
    last.thinking = false
    last.stopped = true
    if (!last.content) last.content = '（已停止生成）'
  }
  loading.value = false
  abortController.value = null
}

/** 中断当前正在生成的回复 */
function stopGeneration() {
  if (abortController.value) abortController.value.abort()
  finalizeInterrupted()
  scrollToBottom()
}

/** 输入行右侧的主按钮：未在生成时是发送键，生成中变为方形停止键（与 DeepSeek/ChatGLM 一致） */
function onSendToggle() {
  if (loading.value) {
    stopGeneration()
    return
  }
  send()
}

function send() {
  const text = input.value.trim()
  // 生成中回车不发送（与主流 agent 一致）；中断请点击方形停止键
  if (!text || loading.value) return
  autoFollow.value = true
  showScrollBtn.value = false
  input.value = ''
  messages.value.push({ role: 'user', content: text })
  messages.value.push({ role: 'assistant', content: '', streaming: true, thinking: true, thinkingText: '' })
  // 必须从响应式数组中取回代理对象,直接改原始对象不会触发视图更新(流式会失效)
  const assistantMsg = messages.value[messages.value.length - 1]
  loading.value = true
  scrollToBottom()
  abortController.value = streamChat(sessionId.value, text, {
    onSession(id) {
      sessionId.value = id
      localStorage.setItem('chat_session', id)
    },
    onTool(label) {
      // 工具调用阶段的实时进度:思考气泡显示"正在xx…",完成后保留"已查询: xx、xx"记录
      assistantMsg.toolSteps = [...(assistantMsg.toolSteps || []), label]
      assistantMsg.thinkingText = '正在' + label + '…'
      scrollToBottom()
    },
    onToken(token) {
      if (assistantMsg.thinking) assistantMsg.thinking = false
      assistantMsg.content += token
      scrollToBottom()
    },
    onDone() {
      assistantMsg.streaming = false
      assistantMsg.thinking = false
      loading.value = false
      abortController.value = null
      loadMetrics()
    },
    onError() {
      if (!assistantMsg.content) {
        assistantMsg.content = 'AI 回复失败，请稍后重试。'
        ElMessage.error('AI 回复失败')
      }
      assistantMsg.streaming = false
      assistantMsg.thinking = false
      loading.value = false
      abortController.value = null
    }
  })
}
function sendQuick(text) {
  input.value = text
  send()
}
async function clearChat() {
  if (loading.value) stopGeneration()
  if (sessionId.value) await clearHistory(sessionId.value)
  messages.value = []
  sessionId.value = ''
  autoFollow.value = true
  showScrollBtn.value = false
  localStorage.removeItem('chat_session')
}

;(async () => {
  if (!sessionId.value) return
  try {
    const { data } = await getHistory(sessionId.value)
    messages.value = data.map(m => ({ role: m.role, content: m.content }))
  } catch(e) {}
})()
</script>

<style scoped>
.ai-intel-page { display: flex; flex-direction: column; gap: 16px; }
.intel-hero { display: grid; grid-template-columns: auto minmax(0, 1fr) auto; gap: 22px; align-items: center; padding: 26px; border: 1px solid rgba(38,54,78,.08); border-radius: 8px; color: #fff; background: radial-gradient(circle at 16% 12%, rgba(120,240,194,.30), transparent 28%), linear-gradient(135deg, #111a2b, #17333a 58%, #1a6b68); box-shadow: 0 20px 54px rgba(17,32,50,.14); overflow: hidden; }
.intel-orb-wrap { position: relative; display: grid; width: 112px; height: 112px; place-items: center; }
.intel-orb { position: relative; display: grid; width: 82px; height: 82px; place-items: center; border-radius: 24px; background: conic-gradient(from 160deg, #1a6b68, #209477, #2b5f8a, #1a6b68); box-shadow: 0 22px 55px rgba(26,107,104,.34); animation: aiFloat 3s ease-in-out infinite; }
.intel-orb::before { content: ''; position: absolute; inset: -12px; border-radius: 30px; border: 1px solid rgba(255,255,255,.24); animation: aiPulse 2.2s ease-in-out infinite; }
.intel-orb span { position: relative; width: 40px; height: 40px; border-radius: 50%; background: #fff; box-shadow: inset 0 0 0 10px rgba(26,107,104,.16); }
.intel-orb i { position: absolute; width: 14px; height: 14px; right: 17px; top: 17px; border-radius: 50%; background: #78f0c2; box-shadow: 0 0 20px #78f0c2; }
.orbit-dot { position: absolute; width: 10px; height: 10px; border-radius: 50%; background: #78f0c2; box-shadow: 0 0 18px #78f0c2; animation: orbit 4s linear infinite; }
.orbit-dot.two { width: 8px; height: 8px; background: #9cc8ff; animation-duration: 5.5s; animation-direction: reverse; }
.intel-copy h2 { margin: 8px 0; color: #fff !important; font-size: 30px !important; }
.intel-copy p { max-width: 680px; color: rgba(232,241,255,.76); line-height: 1.8; }
.intel-score { min-width: 180px; padding: 16px; border-radius: 8px; background: rgba(255,255,255,.12); backdrop-filter: blur(12px); }
.intel-score span, .intel-score small { display: block; color: rgba(232,241,255,.72); font-size: 12px; }
.intel-score strong { display: block; margin: 8px 0; color: #fff; font-size: 28px; }
.intel-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 14px; }
.intel-card { padding: 18px; border: 1px solid rgba(38,54,78,.08); border-radius: 8px; background: rgba(255,255,255,.92); box-shadow: 0 14px 35px rgba(17,32,50,.06); transition: transform .22s ease, box-shadow .22s ease; }
.intel-card:hover, .ai-command-card:hover, .ai-chat-card:hover { transform: translateY(-3px); box-shadow: 0 22px 52px rgba(17,32,50,.12) !important; }
.intel-card span { color: #718096; font-size: 13px; font-weight: 700; }
.intel-card strong { display: block; margin-top: 10px; color: #172033; font-size: 26px; }
.intel-card p { margin-top: 8px; color: #718096; font-size: 13px; }
.intel-workspace { display: grid; grid-template-columns: minmax(280px, .8fr) minmax(280px, .8fr) minmax(420px, 1.4fr); gap: 16px; align-items: stretch; }
.ai-command-card, .ai-chat-card { transition: transform .22s ease, box-shadow .22s ease; }
.card-title { display: flex; align-items: baseline; justify-content: space-between; gap: 12px; }
.card-title span { color: #26364f; font-size: 15px; font-weight: 700; }
.token-bars { display: grid; gap: 15px; }
.token-bar-row { display: grid; grid-template-columns: 42px 1fr 44px; gap: 10px; align-items: center; color: #53627a; font-size: 12px; font-weight: 700; }
.token-track { height: 10px; overflow: hidden; border-radius: 999px; background: #edf2f7; }
.token-track i { display: block; height: 100%; border-radius: inherit; background: linear-gradient(90deg, #1a6b68, #209477); box-shadow: 0 0 18px rgba(26,107,104,.30); }
.capability-list { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px; }
.capability-list button { min-height: 86px; padding: 14px; border: 1px solid rgba(26,107,104,.14); border-radius: 8px; text-align: left; background: linear-gradient(135deg, #fff, #f1faf8); cursor: pointer; transition: transform .2s ease, border-color .2s ease; }
.capability-list button:hover { transform: translateY(-2px); border-color: rgba(26,107,104,.34); }
.capability-list span { display: block; color: #172033; font-weight: 700; }
.capability-list small { display: block; margin-top: 8px; color: #718096; line-height: 1.5; }
.ai-chat-card :deep(.el-card__body) { display: grid; grid-template-rows: minmax(300px, 1fr) auto; gap: 14px; }
.chat-stream-wrap { position: relative; }
.chat-stream { min-height: 330px; max-height: 430px; overflow: auto; padding: 16px; border: 1px solid rgba(38,54,78,.08); border-radius: 8px; background: linear-gradient(180deg, rgba(247,250,252,.92), rgba(255,255,255,.92)); }
.scroll-bottom-btn { position: absolute; right: 14px; bottom: 12px; z-index: 6; display: grid; place-items: center; width: 34px; height: 34px; padding: 0; border: 1px solid rgba(38,54,78,.14); border-radius: 50%; color: #26364f; background: rgba(255,255,255,.95); box-shadow: 0 8px 22px rgba(17,32,50,.18); cursor: pointer; transition: transform .15s ease, color .2s ease; }
.scroll-bottom-btn:hover { transform: translateY(-2px); color: #1a6b68; }
.theme-dark .scroll-bottom-btn { border-color: var(--fiva-dark-line); color: var(--fiva-dark-text); background: rgba(27,39,64,.95); }
.chat-empty { height: 100%; min-height: 280px; display: grid; place-items: center; align-content: center; gap: 10px; text-align: center; color: #718096; }
.chat-empty strong { color: #172033; font-size: 16px; }
.chat-empty p { max-width: 360px; line-height: 1.7; }
.mini-orb { display: grid; width: 54px; height: 54px; place-items: center; border-radius: 16px; background: conic-gradient(from 160deg, #1a6b68, #209477, #2b5f8a, #1a6b68); animation: aiSpin 4s linear infinite; }
.mini-orb span { width: 24px; height: 24px; border-radius: 50%; background: #fff; }
.chat-message { display: flex; align-items: flex-end; gap: 6px; margin-bottom: 16px; }
.chat-message.user { justify-content: flex-end; }
/* 用户消息保留彩色气泡;助手消息平铺排版(对齐 DeepSeek/ChatGPT 风格) */
.chat-bubble { max-width: 76%; padding: 10px 14px; border-radius: 14px; color: #fff; line-height: 1.7; white-space: pre-wrap; background: linear-gradient(135deg, #1a6b68, #209477); }
.assistant-text { flex: 1; min-width: 0; padding: 2px 4px; }
.md-content { color: #26364f; font-size: 14.5px; line-height: 1.8; word-break: break-word; }
.md-content :deep(p) { margin: 8px 0; }
.md-content :deep(p:first-child), .md-content :deep(h1:first-child), .md-content :deep(h2:first-child), .md-content :deep(h3:first-child), .md-content :deep(h4:first-child), .md-content :deep(ul:first-child), .md-content :deep(ol:first-child), .md-content :deep(table:first-child), .md-content :deep(blockquote:first-child), .md-content :deep(pre:first-child) { margin-top: 0; }
.md-content :deep(p:last-child), .md-content :deep(h1:last-child), .md-content :deep(h2:last-child), .md-content :deep(h3:last-child), .md-content :deep(h4:last-child), .md-content :deep(ul:last-child), .md-content :deep(ol:last-child), .md-content :deep(table:last-child), .md-content :deep(blockquote:last-child), .md-content :deep(pre:last-child) { margin-bottom: 0; }
.md-content :deep(strong) { color: inherit; font-weight: 700; }
.md-content :deep(h1), .md-content :deep(h2), .md-content :deep(h3), .md-content :deep(h4) { margin: 18px 0 8px; color: #172033; font-weight: 700; line-height: 1.45; }
.md-content :deep(h1) { font-size: 18px; }
.md-content :deep(h2) { font-size: 16.5px; }
.md-content :deep(h3) { font-size: 15px; }
.md-content :deep(h4) { font-size: 14.5px; }
.md-content :deep(ul), .md-content :deep(ol) { margin: 8px 0; padding-left: 24px; }
.md-content :deep(li) { margin: 5px 0; }
.md-content :deep(li::marker) { color: #7c9a97; font-weight: 600; }
.md-content :deep(code) { background: rgba(26,107,104,.09); padding: 2px 6px; border-radius: 5px; font-size: .88em; font-family: 'JetBrains Mono', Consolas, 'Courier New', monospace; color: #145754; }
.md-content :deep(pre) { background: #101828; color: #dbe5f1; padding: 14px 16px; border-radius: 10px; overflow-x: auto; margin: 10px 0; line-height: 1.65; }
.md-content :deep(pre code) { background: none; padding: 0; color: inherit; font-size: 13px; }
.md-content :deep(table) { width: 100%; margin: 12px 0; font-size: 13.5px; line-height: 1.6; border-collapse: separate; border-spacing: 0; border: 1px solid rgba(38,54,78,.14); border-radius: 10px; overflow: hidden; }
.md-content :deep(th) { background: rgba(26,107,104,.08); color: #172033; font-weight: 700; text-align: left; padding: 9px 14px; border-bottom: 1px solid rgba(38,54,78,.16); }
.md-content :deep(td) { padding: 9px 14px; border-bottom: 1px solid rgba(38,54,78,.08); }
.md-content :deep(th:first-child) { border-top-left-radius: 9px; }
.md-content :deep(th:last-child) { border-top-right-radius: 9px; }
.md-content :deep(tbody tr:last-child td) { border-bottom: none; }
.md-content :deep(tbody tr:last-child td:first-child) { border-bottom-left-radius: 9px; }
.md-content :deep(tbody tr:last-child td:last-child) { border-bottom-right-radius: 9px; }
.md-content :deep(blockquote) { margin: 10px 0; padding: 9px 14px; border-left: 3px solid #1a6b68; background: rgba(26,107,104,.06); border-radius: 0 8px 8px 0; color: #53627a; }
.md-content :deep(blockquote p) { margin: 4px 0; }
.md-content :deep(hr) { border: none; height: 1px; background: rgba(38,54,78,.12); margin: 16px 0; }
/* 深色主题下的正文排版 */
.theme-dark .md-content { color: var(--fiva-dark-text); }
.theme-dark .md-content :deep(h1), .theme-dark .md-content :deep(h2), .theme-dark .md-content :deep(h3), .theme-dark .md-content :deep(h4) { color: var(--fiva-dark-text); }
.theme-dark .md-content :deep(code) { background: rgba(120,240,194,.10); color: #9be8cf; }
.theme-dark .md-content :deep(pre) { background: #0b1120; }
.theme-dark .md-content :deep(table) { border-color: var(--fiva-dark-line); }
.theme-dark .md-content :deep(th) { background: rgba(26,107,104,.18); color: var(--fiva-dark-text); border-bottom-color: var(--fiva-dark-line); }
.theme-dark .md-content :deep(td) { border-bottom-color: var(--fiva-dark-line); }
.theme-dark .md-content :deep(blockquote) { background: rgba(26,107,104,.10); color: var(--fiva-dark-muted); }
.theme-dark .md-content :deep(hr) { background: var(--fiva-dark-line); }
.theme-dark .thinking-bubble { color: var(--fiva-dark-muted); }
.typing-cursor { width: 2px; height: 18px; background: #1a6b68; animation: blink 1s infinite; }
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
.chat-input-row { display: flex; gap: 10px; align-items: center; }
.chat-input-row .el-input { flex: 1; }
.chat-input-row .el-button { flex-shrink: 0; }
.send-toggle { flex-shrink: 0; display: grid; place-items: center; width: 38px; height: 38px; padding: 0; border: none; border-radius: 50%; cursor: pointer; color: #fff; background: linear-gradient(135deg, #1a6b68, #209477); opacity: .45; transition: opacity .18s ease, transform .15s ease, background .2s ease, box-shadow .18s ease; }
.send-toggle:hover { transform: scale(1.07); }
.send-toggle.is-active { opacity: 1; box-shadow: 0 8px 20px rgba(26,107,104,.30); }
.send-toggle.is-stop { opacity: 1; background: #172033; }
.send-toggle.is-stop:hover { background: #22314d; }
.stop-square { width: 12px; height: 12px; border-radius: 2.5px; background: #fff; pointer-events: none; }
.send-toggle svg { pointer-events: none; }
@keyframes blink { 0%, 50% { opacity: 1; } 51%, 100% { opacity: 0; } }
@keyframes thinkingBounce { 0%, 80%, 100% { transform: translateY(0); opacity: .4; } 40% { transform: translateY(-5px); opacity: 1; } }
@keyframes aiSpin { to { transform: rotate(360deg); } }
@keyframes aiPulse { 0%,100% { opacity: .55; transform: scale(.92); } 50% { opacity: 1; transform: scale(1.08); } }
@keyframes aiFloat { 0%,100% { transform: translateY(0); } 50% { transform: translateY(-5px); } }
@keyframes orbit { from { transform: rotate(0deg) translateX(54px) rotate(0deg); } to { transform: rotate(360deg) translateX(54px) rotate(-360deg); } }
@media (max-width: 1180px) { .intel-hero, .intel-workspace { grid-template-columns: 1fr; } .intel-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } .intel-score { width: fit-content; } }
@media (max-width: 640px) { .intel-grid, .capability-list { grid-template-columns: 1fr; } .chat-input-row { flex-wrap: wrap; } .chat-input-row .el-input { flex: 1 1 100%; } }
</style>