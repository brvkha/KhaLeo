import type { User } from '../types'

export type AuthSession = {
  currentUser: User
  accessToken: string
  refreshToken: string | null
}

const SESSION_STORAGE_KEY = 'khaleo-auth-session'

export function readAuthSession(): AuthSession | null {
  const raw = localStorage.getItem(SESSION_STORAGE_KEY)
  if (!raw) {
    return null
  }
  try {
    return JSON.parse(raw) as AuthSession
  } catch {
    return null
  }
}

export function persistAuthSession(session: AuthSession | null): void {
  if (!session) {
    localStorage.removeItem(SESSION_STORAGE_KEY)
    return
  }
  localStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(session))
}

export function getAccessToken(): string | null {
  return readAuthSession()?.accessToken ?? null
}