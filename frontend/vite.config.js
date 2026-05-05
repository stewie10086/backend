import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

const API_TARGET = 'http://localhost:8080'


const proxyPrefixes = [
  '/auth',
  '/expertise',
  '/me',
  '/specialists',
  '/bookings',
  '/specialist',
  '/admin',
  '/pricing'
]

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue()],
  server: {
    host: '0.0.0.0',
    port: 3000,
    allowedHosts: true,
    proxy: Object.fromEntries(proxyPrefixes.map((p) => [p, API_TARGET]))
  },
  preview: {
    host: '0.0.0.0',
    port: 3000
  },
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  }
})
