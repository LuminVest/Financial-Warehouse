import vue from '@vitejs/plugin-vue'
import { defineConfig } from 'vite'
import { fileURLToPath, URL } from 'node:url'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  build: {
    chunkSizeWarningLimit: 1000,
    rolldownOptions: {
      output: {
        manualChunks(id) {
          if (id.includes('node_modules')) {
            if (id.includes('element-plus') || id.includes('@element-plus/icons-vue')) {
              return 'element-plus'
            }
            if (id.includes('vue') || id.includes('pinia')) {
              return 'vue-vendor'
            }
          }
        },
      },
    },
  },
  server: {
    port: 5174,
    open: true,
    proxy: {
      // 用户端 API：/api/** → 后端 8990
      '/api': {
        target: 'http://localhost:8990',
        changeOrigin: true,
      },
      // OSS 文件服务：/oss/** → 8130
      '/oss': {
        target: 'http://localhost:8130',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/oss/, '/api/oss'),
      },
      // OSS 静态图片：/uploads/** → 8130 本地目录映射
      '/uploads': {
        target: 'http://localhost:8130',
        changeOrigin: true,
      },
    },
  },
})
