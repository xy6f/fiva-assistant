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

  const backendUrl = import.meta.env.DEV ? 'http://localhost:8080' : ''
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
          if (currentEvent === 'token') onToken?.(data)
          else if (currentEvent === 'done') onDone?.()
          else if (currentEvent === 'error') onError?.(data)
          currentEvent = null
        }
      }
    }
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

  const backendUrl = import.meta.env.DEV ? 'http://localhost:8080' : ''
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
          if (currentEvent === 'token') onToken?.(data)
          else if (currentEvent === 'done') onDone?.()
          else if (currentEvent === 'error') onError?.(data)
          currentEvent = null
        }
      }
    }
    onDone?.()
  }).catch(err => {
    if (err.name !== 'AbortError') onError?.(err.message)
  })

  return controller
}
