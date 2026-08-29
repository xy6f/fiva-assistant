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
 * @param {Object} callbacks - { onSession, onToken, onDone, onError }
 * @returns {AbortController} 可用于取消请求
 */
export function streamChat(sessionId, message, { onSession, onToken, onDone, onError }) {
  const controller = new AbortController()
  const backendUrl = import.meta.env.DEV ? 'http://localhost:8080' : ''
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

    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })

      const lines = buffer.split(/\r?\n/)
      buffer = lines.pop() || ''

      let currentEvent = null
      for (const line of lines) {
        if (line.startsWith('event:')) {
          currentEvent = line.slice(6).trim()
        } else if (line.startsWith('data:')) {
          const data = line.slice(5).trim()
          if (currentEvent === 'session') onSession?.(data)
          else if (currentEvent === 'token') onToken?.(data)
          else if (currentEvent === 'done' && !doneCalled) { doneCalled = true; onDone?.() }
          else if (currentEvent === 'error') onError?.(data)
          currentEvent = null
        }
      }
    }
    // stream 正常结束但未收到 event:done 时兜底
    if (!doneCalled) { doneCalled = true; onDone?.() }
  }).catch(err => {
    if (err.name !== 'AbortError') onError?.(err.message)
  })

  return controller
}
