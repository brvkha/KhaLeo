import { Navigate, Route, Routes } from 'react-router-dom'
import { Layout } from '../components/Layout'
import { LoginPage } from '../features/auth/LoginPage'
import { RegisterPage } from '../features/auth/RegisterPage'
import { DecksPage } from '../features/decks/DecksPage'
import { StudyWorkspacePage } from '../features/study-workspace/StudyWorkspacePage'
import { StudySessionPage } from '../features/study-session/StudySessionPage'
import { CardsWorkspacePage } from '../features/cards-workspace/CardsWorkspacePage'
import { ProfilePage } from '../features/profile/ProfilePage'
import { RequireAdmin, RequireAuth } from './guards'
import { AdminDashboardPage } from '../features/admin/dashboard/AdminDashboardPage'
import { AdminUsersPage } from '../features/admin/users/AdminUsersPage'
import { AdminDecksPage } from '../features/admin/decks/AdminDecksPage'
import { AdminCardsPage } from '../features/admin/cards/AdminCardsPage'

function BlockedPage() {
  return (
    <div className="rounded border border-rose-300 bg-rose-50 p-4 text-rose-700">
      Your account is blocked by an admin.
    </div>
  )
}

function HomePage() {
  return (
    <section className="rounded border border-slate-200 bg-white p-5">
      <h1 className="text-2xl font-semibold">Study Dashboard</h1>
      <p className="mt-2 text-slate-600">
        Use Decks, Cards, and Study routes to complete your daily flow.
      </p>
    </section>
  )
}

export function AppRouter() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/blocked" element={<BlockedPage />} />
        <Route path="/decks" element={<DecksPage />} />

        <Route element={<RequireAuth />}>
          <Route index element={<HomePage />} />
          <Route path="/cards" element={<CardsWorkspacePage />} />
          <Route path="/study" element={<StudyWorkspacePage />} />
          <Route path="/study/session/:deckId" element={<StudySessionPage />} />
          <Route path="/profile" element={<ProfilePage />} />
        </Route>

        <Route path="/admin" element={<RequireAdmin />}>
          <Route index element={<AdminDashboardPage />} />
          <Route path="users" element={<AdminUsersPage />} />
          <Route path="decks" element={<AdminDecksPage />} />
          <Route path="cards" element={<AdminCardsPage />} />
        </Route>

        <Route path="*" element={<Navigate replace to="/" />} />
      </Route>
    </Routes>
  )
}
