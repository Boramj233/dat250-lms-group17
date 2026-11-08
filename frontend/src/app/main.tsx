import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { QueryClientProvider } from '@tanstack/react-query'
import { BrowserRouter } from 'react-router'
import '../ui/styles/index.css'
import App from './App.tsx'
import { queryClient } from './queryClient'
import { initAuth } from '../data/auth/auth'

// Find out whether the user is logged in before the first render. If Keycloak is
// unreachable the app still starts, as logged out.
try {
  await initAuth()
} catch (error) {
  console.error('Keycloak initialisation failed', error)
}

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <App />
      </BrowserRouter>
    </QueryClientProvider>
  </StrictMode>,
)
