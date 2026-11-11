import { isLoginPopup } from '../data/auth/auth'
import AppRoutes from './routes'

function App() {
  // The login popup comes back to the SPA after Keycloak; the main window then reads its
  // address and closes it. Show a short message there instead of loading the whole app.
  if (isLoginPopup()) {
    return <p className="auth-status" role="status">Login complete. This window closes by itself.</p>
  }
  return <AppRoutes />
}

export default App
