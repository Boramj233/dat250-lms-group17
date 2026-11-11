import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'
import { mockBackend } from './mock/server.ts'

// Everything that belongs to Spring Boot goes through the Vite dev server, so the
// browser only talks to one address (localhost:5173) and the session cookie works.
// Keep the browser's Host (localhost:5173) so Spring builds OAuth2 redirect URIs
// back to Vite instead of to the backend port. The string shorthand would set
// changeOrigin: true and rewrite Host to localhost:8080.
const backend = {
  target: 'http://localhost:8080',
  changeOrigin: false,
  xfwd: true,
}

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  // `npm run dev:mock`: no backend or Keycloak; mock/ answers instead (see mock/README.md).
  if (mode === 'mock') {
    return { plugins: [react(), mockBackend()] }
  }
  return {
    plugins: [react()],
    server: {
      proxy: {
        '/api': backend,
        '/oauth2': backend,
        '/login/oauth2': backend,
        // Exact paths only (same as nginx); keys starting with ^ are regular expressions.
        '^/(token|logout)$': backend,
      },
    },
  }
})
