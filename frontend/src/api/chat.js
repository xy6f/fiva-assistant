import api from './index'

export function sendMessage(sessionId, message) {
  return api.post('/chat/send', { sessionId, message })
}

export function getHistory(sessionId) {
  return api.get('/chat/history', { params: { sessionId } })
}

export function clearHistory(sessionId) {
  return api.delete('/chat/history', { params: { sessionId } })
}

/**
 * AI 指标（真实调用数据：累计次数/成功率/平均延迟/今日Token + 近7天趋势）
 */
export function getAiMetrics() {
  return api.get('/chat/metrics')
}

/**
 * 流式聊天（SSE）
 * @param {string} sessionId
 * @param {string} message
 * @param {Object} callbacks - { onSession, onTool, onToken, onDone, onError }
 *   onTool: 工具调用阶段的进度文案（如 "正在查询本月收支…"）
 * @returns {AbortController} 可用于取消请求
 */
export function streamChat(sessionId, message, { onSession, onTool, onToken, onDone, onError }) {
  const controller = new AbortController()
  // 对话 Agent 已迁移至 Python LangChain 服务（ai-agent，端口 8001），统一走 vite 代理
  const backendUrl = ''
  const token = localStorage.getItem('token')
  let doneCalled = false

  fetch(`${backendUrl}/api/chat/stream`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { 'Authorization': `Bearer ${token}` } : {})
    },
    body: JSON.stringify({ sessionId, message }),
    signal: controller.signal
  }).then(async response => {
    console.log('[SSE] Response status:', response.status, 'Content-Type:', response.headers.get('content-type'))
    if (!response.ok) {
      if (response.status === 401) throw new Error('登录已过期')
      throw new Error('请求失败')
    }
    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''
    // 标准 SSE 解析:event 与其后的多行 data 用空行作为边界,多个 data 行以 \n 连接,
    // 这样 token 里的换行符不会丢失,流式过程中 markdown 才能实时渲染
    let eventName = null
    let dataLines = []
    const dispatchEvent = () => {
      if (eventName === null && dataLines.length === 0) return
      const data = dataLines.join('\n')
      if (eventName === 'session') onSession?.(data)
      else if (eventName === 'tool') onTool?.(data)
      else if (eventName === 'token') onToken?.(data)
      else if (eventName === 'done' && !doneCalled) { doneCalled = true; onDone?.() }
      else if (eventName === 'error') onError?.(data)
      eventName = null
      dataLines = []
    }

    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })

      const lines = buffer.split(/\r?\n/)
      buffer = lines.pop() || ''

      for (const line of lines) {
        if (line === '') { dispatchEvent(); continue }
        if (line.startsWith('event:')) {
          eventName = line.slice(6).trim()
        } else if (line.startsWith('data:')) {
          dataLines.push(line.slice(5).replace(/^ /, ''))
        }
      }
    }
    dispatchEvent()
    // stream 正常结束但未收到 event:done 时兜底
    if (!doneCalled) { doneCalled = true; onDone?.() }
  }).catch(err => {
    if (err.name !== 'AbortError') onError?.(err.message)
  })

  return controller
}
