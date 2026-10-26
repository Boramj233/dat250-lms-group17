// export default App

import { useEffect, useState } from 'react'

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
    <div>
      <h1>DAT250 LMS</h1>
      <p>Message from Spring Boot: {message}</p>
    </div>
  )
}

export default App
