import { useEffect, useState } from 'react'
import './App.css'

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
    <main className="app">
      <h1>DAT250 LMS</h1>
      <p>Message from Spring Boot: {message}</p>
    </main>
  )
}

export default App
