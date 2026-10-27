import { useEffect, useState } from 'react'
import AppLayout from './layout/AppLayout'

function App() {
  const [message, setMessage] = useState('Loading...')

  useEffect(() => {
    fetch('/api/hello')
      .then(response => {
        if (!response.ok) {
          throw new Error('API request failed')
        }
        return response.json()
      })
      .then(data => setMessage(data.message))
      .catch(() => setMessage('Cannot connect to backend'))
  }, [])

  return (
    <AppLayout>
      <h1>Velkommen</h1>
      <p>Message from Spring Boot: {message}</p>
    </AppLayout>
  )
}

export default App
