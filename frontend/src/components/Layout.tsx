import { Link, Outlet } from 'react-router-dom'
import { useAuthStore } from '../store/authStore'

export function Layout() {
  const currentUser = useAuthStore((state) => state.currentUser)
  const logout = useAuthStore((state) => state.logout)

  return (
    <div className="min-h-screen bg-slate-50 text-slate-900">
      <header className="border-b border-slate-200 bg-white/90">
        <nav className="mx-auto flex max-w-6xl items-center justify-between p-4">
          <div className="flex items-center gap-3">
            <Link className="text-lg font-semibold" to="/">
              KhaLeo
            </Link>
            <Link to="/decks">Decks</Link>
            <Link to="/cards">Cards</Link>
            <Link to="/study">Study</Link>
            <Link to="/profile">Profile</Link>
            {currentUser?.role === 'ADMIN' ? <Link to="/admin">Admin</Link> : null}
          </div>
          <div className="flex items-center gap-3">
            {currentUser ? (
              <>
                <span className="text-sm text-slate-600">{currentUser.email}</span>
                <button
                  className="rounded bg-slate-900 px-3 py-1 text-white"
                  onClick={() => {
                    void logout()
                  }}
                >
                  Logout
                </button>
              </>
            ) : (
              <>
                <Link to="/login">Login</Link>
                <Link to="/register">Register</Link>
              </>
            )}
          </div>
        </nav>
      </header>
      <main className="mx-auto max-w-6xl p-4">
        <Outlet />
      </main>
    </div>
  )
}
