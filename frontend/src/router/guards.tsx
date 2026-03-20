import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuthStore } from '../store/authStore'

export function RequireAuth() {
  const currentUser = useAuthStore((state) => state.currentUser)
  const location = useLocation()
  if (!currentUser) {
    const returnTo = encodeURIComponent(`${location.pathname}${location.search}`)
    return <Navigate to={`/login?returnTo=${returnTo}`} replace />
  }
  if (currentUser.banned) {
    return <Navigate to="/blocked" replace />
  }
  return <Outlet />
}

export function RequireAdmin() {
  const currentUser = useAuthStore((state) => state.currentUser)
  const location = useLocation()
  if (!currentUser) {
    const returnTo = encodeURIComponent(`${location.pathname}${location.search}`)
    return <Navigate to={`/login?returnTo=${returnTo}`} replace />
  }
  if (currentUser.role !== 'ADMIN') {
    return <Navigate to="/" replace />
  }
  return <Outlet />
}
