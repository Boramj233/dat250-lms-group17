import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// Keep the browser's Host (localhost:5173) so Spring builds OAuth2 redirect URIs
// back to Vite instead of to the backend port. The string shorthand would set
// changeOrigin: true and rewrite Host to localhost:8080.
const backend = {
  target: 'http://localhost:8080',
  changeOrigin: false,
  xfwd: true,
}

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': backend,
      '/oauth2': backend,
      '/login/oauth2': backend,
      '/token': backend,
      '/logout': backend,
    },
  },
})
