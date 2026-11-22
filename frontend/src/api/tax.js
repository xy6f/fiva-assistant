import api from './index'

export function calculateTax(data) {
  return api.post('/tax/calculate', data)
}

export function getTaxSuggestion(data) {
  return api.post('/tax/suggestion', data)
}

/**
 * 流式税务建议（SSE）
 * @param {Object} data - 税务测算结果
 * @param {Object} callbacks - { onToken, onDone, onError }
 * @returns {AbortController}
 */
export function streamTaxSuggestion(data, { onToken, onDone, onError }) {
  const controller = new AbortController()

  // 税负 AI 已迁移至 Python LangChain 服务（ai-agent，端口 8001），统一走 vite 代理
  const backendUrl = ''
  const token = localStorage.getItem('token')

  fetch(`${backendUrl}/api/tax/suggestion/stream`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { 'Authorization': `Bearer ${token}` } : {})
    },
    body: JSON.stringify(data),
    signal: controller.signal
  }).then(async response => {
    if (!response.ok) {
      if (response.status === 401) throw new Error('登录已过期')
      throw new Error('请求失败')
    }
    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''
    // 标准 SSE 解析(同 chat.js):多行 data 以 \n 连接,保留 token 中的换行
    let eventName = null
    let dataLines = []
    const dispatchEvent = () => {
      if (eventName === null && dataLines.length === 0) return
      const data = dataLines.join('\n')
      if (eventName === 'token') onToken?.(data)
      else if (eventName === 'done') onDone?.()
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
    onDone?.()
  }).catch(err => {
    if (err.name !== 'AbortError') onError?.(err.message)
  })

  return controller
}

/**
 * 流式税务深度分析（SSE）
 * @param {Object} data - 税务测算结果
 * @param {Object} callbacks - { onToken, onDone, onError }
 * @returns {AbortController}
 */
export function streamTaxAnalysis(data, { onToken, onDone, onError }) {
  const controller = new AbortController()

  // 税负 AI 已迁移至 Python LangChain 服务（ai-agent，端口 8001），统一走 vite 代理
  const backendUrl = ''
  const token = localStorage.getItem('token')

  fetch(`${backendUrl}/api/tax/analysis`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { 'Authorization': `Bearer ${token}` } : {})
    },
    body: JSON.stringify(data),
    signal: controller.signal
  }).then(async response => {
    if (!response.ok) {
      if (response.status === 401) throw new Error('登录已过期')
      throw new Error('请求失败')
    }
    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''
    // 标准 SSE 解析(同 chat.js):多行 data 以 \n 连接,保留 token 中的换行
    let eventName = null
    let dataLines = []
    const dispatchEvent = () => {
      if (eventName === null && dataLines.length === 0) return
      const data = dataLines.join('\n')
      if (eventName === 'token') onToken?.(data)
      else if (eventName === 'done') onDone?.()
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
    onDone?.()
  }).catch(err => {
    if (err.name !== 'AbortError') onError?.(err.message)
  })

  return controller
}
