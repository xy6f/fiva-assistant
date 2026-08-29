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
        <div ref="messageBoxRef" class="chat-stream">
          <div v-if="messages.length === 0" class="chat-empty">
            <div class="mini-orb"><span></span></div>
            <strong>选择问题或直接输入</strong>
            <p>可以询问税负、发票风险、利润波动、预算执行和经营摘要。</p>
          </div>
          <div v-for="(msg, idx) in messages" :key="idx" class="chat-message" :class="msg.role">
            <div v-if="msg.role === 'assistant'" class="chat-bubble md-content" v-html="renderMarkdown(msg.content)"></div>
            <div v-else class="chat-bubble">{{ msg.content }}</div>
            <span v-if="msg.streaming" class="typing-cursor"></span>
          </div>
        </div>
        <div class="chat-input-row">
          <el-input v-model="input" @keyup.enter="send" placeholder="输入财税问题，例如：帮我检查本月发票风险" :disabled="loading" />
          <el-button type="primary" @click="send" :loading="loading">发送</el-button>
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
  nextTick(() => {
    if (messageBoxRef.value) messageBoxRef.value.scrollTop = messageBoxRef.value.scrollHeight
  })
}
function send() {
  const text = input.value.trim()
  if (!text || loading.value) return
  input.value = ''
  messages.value.push({ role: 'user', content: text })
  const assistantMsg = { role: 'assistant', content: '', streaming: true }
  messages.value.push(assistantMsg)
  loading.value = true
  scrollToBottom()
  streamChat(sessionId.value, text, {
    onSession(id) {
      sessionId.value = id
      localStorage.setItem('chat_session', id)
    },
    onToken(token) {
      assistantMsg.content += token
      scrollToBottom()
    },
    onDone() {
      assistantMsg.streaming = false
      loading.value = false
      loadMetrics()
    },
    onError() {
      if (!assistantMsg.content) {
        assistantMsg.content = 'AI 回复失败，请稍后重试。'
        ElMessage.error('AI 回复失败')
      }
      assistantMsg.streaming = false
      loading.value = false
    }
  })
}
function sendQuick(text) {
  input.value = text
  send()
}
async function clearChat() {
  if (sessionId.value) await clearHistory(sessionId.value)
  messages.value = []
  sessionId.value = ''
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
.chat-stream { min-height: 330px; max-height: 430px; overflow: auto; padding: 16px; border: 1px solid rgba(38,54,78,.08); border-radius: 8px; background: linear-gradient(180deg, rgba(247,250,252,.92), rgba(255,255,255,.92)); }
.chat-empty { height: 100%; min-height: 280px; display: grid; place-items: center; align-content: center; gap: 10px; text-align: center; color: #718096; }
.chat-empty strong { color: #172033; font-size: 16px; }
.chat-empty p { max-width: 360px; line-height: 1.7; }
.mini-orb { display: grid; width: 54px; height: 54px; place-items: center; border-radius: 16px; background: conic-gradient(from 160deg, #1a6b68, #209477, #2b5f8a, #1a6b68); animation: aiSpin 4s linear infinite; }
.mini-orb span { width: 24px; height: 24px; border-radius: 50%; background: #fff; }
.chat-message { display: flex; align-items: flex-end; gap: 6px; margin-bottom: 12px; }
.chat-message.user { justify-content: flex-end; }
.chat-bubble { max-width: 78%; padding: 12px 14px; border-radius: 10px; color: #26364f; line-height: 1.75; white-space: pre-wrap; background: #fff; box-shadow: 0 8px 22px rgba(17,32,50,.06); }
.chat-message.user .chat-bubble { color: #fff; background: linear-gradient(135deg, #1a6b68, #209477); }
.md-content :deep(p) { margin: 6px 0; }
.md-content :deep(ul), .md-content :deep(ol) { margin: 6px 0; padding-left: 20px; }
.md-content :deep(li) { margin: 3px 0; }
.md-content :deep(strong) { color: #1a6b68; }
.md-content :deep(h1), .md-content :deep(h2), .md-content :deep(h3) { margin: 12px 0 6px; color: #172033; }
.md-content :deep(code) { background: #f0f4f8; padding: 2px 6px; border-radius: 4px; font-size: 13px; }
.md-content :deep(pre) { background: #1a2332; color: #e8e8e8; padding: 12px; border-radius: 6px; overflow-x: auto; }
.md-content :deep(table) { border-collapse: collapse; width: 100%; margin: 8px 0; }
.md-content :deep(th), .md-content :deep(td) { border: 1px solid #e2e8f0; padding: 6px 10px; text-align: left; font-size: 13px; }
.md-content :deep(th) { background: #f7fafc; font-weight: 600; }
.typing-cursor { width: 2px; height: 18px; background: #1a6b68; animation: blink 1s infinite; }
.chat-input-row { display: grid; grid-template-columns: 1fr auto auto; gap: 10px; }
@keyframes blink { 0%, 50% { opacity: 1; } 51%, 100% { opacity: 0; } }
@keyframes aiSpin { to { transform: rotate(360deg); } }
@keyframes aiPulse { 0%,100% { opacity: .55; transform: scale(.92); } 50% { opacity: 1; transform: scale(1.08); } }
@keyframes aiFloat { 0%,100% { transform: translateY(0); } 50% { transform: translateY(-5px); } }
@keyframes orbit { from { transform: rotate(0deg) translateX(54px) rotate(0deg); } to { transform: rotate(360deg) translateX(54px) rotate(-360deg); } }
@media (max-width: 1180px) { .intel-hero, .intel-workspace { grid-template-columns: 1fr; } .intel-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } .intel-score { width: fit-content; } }
@media (max-width: 640px) { .intel-grid, .capability-list { grid-template-columns: 1fr; } .chat-input-row { grid-template-columns: 1fr; } }
</style>