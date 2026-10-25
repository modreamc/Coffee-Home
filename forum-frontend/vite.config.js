import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

const backend = process.env.VITE_PROXY_TARGET || 'http://localhost:8080'

const proxy = {
  '/api': { target: backend, changeOrigin: true },
  '/uploads': { target: backend, changeOrigin: true },
  '/picture': { target: backend, changeOrigin: true },
}

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 3000,
    proxy,
  },
  // vite preview 默认不带 proxy，生产预览时也要能打到后端
  preview: {
    port: 4173,
    proxy,
  },
  build: {
    // 把体积最大的两个依赖拆出来，避免单个 chunk 超过 500kB 的告警
    rollupOptions: {
      output: {
        manualChunks: {
          vue: ['vue', 'vue-router', 'pinia'],
          'element-plus': ['element-plus', '@element-plus/icons-vue'],
        },
      },
    },
    // element-plus 全量引入本身约 1.1MB，已单独分包；这里把告警阈值调到 1200kB
    chunkSizeWarningLimit: 1200,
  },
})
