import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': 'http://localhost:4010',
    },
  },
  preview: {
    host: '127.0.0.1',
    proxy: {
      '/api': 'http://127.0.0.1:4010',
    },
  },
})
