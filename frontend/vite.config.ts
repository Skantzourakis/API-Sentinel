import path from 'node:path'
import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  server: {
    // Requests to /api go to the Spring backend, so the browser sees one origin and no CORS setup is needed.
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
