import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    // Only the REST API goes to Spring Boot. Login and logout happen directly
    // between the browser and Keycloak (localhost:8081).
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
