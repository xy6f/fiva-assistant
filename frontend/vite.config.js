import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      // 对话 Agent 与税负 AI 由 Python LangChain 服务（ai-agent，端口 8001）提供
      // 注意：这三条必须放在 '/api' 之前，避免被通用代理先匹配到 Java 后端
      '/api/chat': {
        target: 'http://localhost:8001',
        changeOrigin: true,
        // SSE 流式输出需要禁用缓冲
        configure: (proxy) => {
          proxy.on('proxyRes', (proxyRes) => {
            if (proxyRes.headers['content-type']?.includes('text/event-stream')) {
              proxyRes.headers['cache-control'] = 'no-cache'
              proxyRes.headers['connection'] = 'keep-alive'
              proxyRes.headers['x-accel-buffering'] = 'no'
            }
          })
        }
      },
      '/api/tax/suggestion': {
        target: 'http://localhost:8001',
        changeOrigin: true,
        // SSE 流式输出需要禁用缓冲
        configure: (proxy) => {
          proxy.on('proxyRes', (proxyRes) => {
            if (proxyRes.headers['content-type']?.includes('text/event-stream')) {
              proxyRes.headers['cache-control'] = 'no-cache'
              proxyRes.headers['connection'] = 'keep-alive'
              proxyRes.headers['x-accel-buffering'] = 'no'
            }
          })
        }
      },
      '/api/tax/analysis': {
        target: 'http://localhost:8001',
        changeOrigin: true,
        // SSE 流式输出需要禁用缓冲
        configure: (proxy) => {
          proxy.on('proxyRes', (proxyRes) => {
            if (proxyRes.headers['content-type']?.includes('text/event-stream')) {
              proxyRes.headers['cache-control'] = 'no-cache'
              proxyRes.headers['connection'] = 'keep-alive'
              proxyRes.headers['x-accel-buffering'] = 'no'
            }
          })
        }
      },
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        // SSE 流式输出需要禁用缓冲
        configure: (proxy) => {
          proxy.on('proxyRes', (proxyRes) => {
            if (proxyRes.headers['content-type']?.includes('text/event-stream')) {
              proxyRes.headers['cache-control'] = 'no-cache'
              proxyRes.headers['connection'] = 'keep-alive'
              proxyRes.headers['x-accel-buffering'] = 'no'
            }
          })
        }
      }
    }
  }
})
