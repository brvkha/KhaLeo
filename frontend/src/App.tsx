import { useEffect } from 'react'
import { AppRouter } from './router/AppRouter'
import { NotificationCenter } from './components/NotificationCenter'
import { useAuthStore } from './store/authStore'

function App() {
  const bootstrap = useAuthStore((state) => state.bootstrap)

  useEffect(() => {
    bootstrap()
  }, [bootstrap])

  return (
    <>
      <NotificationCenter />
      <AppRouter />
    </>
  )
}

export default App
