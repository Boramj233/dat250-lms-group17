import AppRoutes from './routes'
import { CoursePreferencesProvider } from '../features/course'

function App() {
  return (
    <CoursePreferencesProvider>
      <AppRoutes />
    </CoursePreferencesProvider>
  )
}

export default App
